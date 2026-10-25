package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.PlanningSlot;
import com.example.gestionstationskii.entities.SlotStatus;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.entities.Registration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("concurrency")
class RegistrationConcurrencyTest extends RegistrationIntegrationSupport {
    @Autowired private IRegistrationServices registrationServices;

    @Test
    void simultaneousDoubleClickForSameSkierCreatesOnlyOneRegistration() throws Exception {
        Course course = createCourse();
        Instructor instructor = createInstructor(course);
        Skier skier = createAdultSkier();
        PlanningSlot slot = createSlot(course, instructor, 2);
        Long skierId = skier.getNumSkier();
        Long slotId = slot.getId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int attempt = 0; attempt < 2; attempt++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting to start");
                    try {
                        registrationServices.registerSlot(skierId, slotId);
                        return true;
                    } catch (BusinessException duplicateRegistration) {
                        return false;
                    }
                }));
            }

            if (!ready.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Workers did not become ready");
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(60, TimeUnit.SECONDS)) accepted++;
            }

            assertEquals(1, accepted);
            assertEquals(1, registrations.findBySkierNumSkier(skierId).size());
            assertEquals(1L, registrations.countByPlanningSlotId(slotId));
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void simultaneousCancellationAndLastSeatRegistrationKeepCapacityConsistent() throws Exception {
        Course course = createCourse();
        Instructor instructor = createInstructor(course);
        Skier existingSkier = createAdultSkier();
        Skier waitingSkier = createAdultSkier();
        PlanningSlot slot = createSlot(course, instructor, 1);
        Registration existingRegistration = registrationServices.registerSlot(existingSkier.getNumSkier(), slot.getId());
        Long registrationId = existingRegistration.getNumRegistration();
        Long waitingSkierId = waitingSkier.getNumSkier();
        Long slotId = slot.getId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> cancellation = pool.submit(() -> {
                ready.countDown();
                if (!start.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting to start");
                registrationServices.cancel(registrationId);
                return true;
            });
            Future<Boolean> registration = pool.submit(() -> {
                ready.countDown();
                if (!start.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting to start");
                try {
                    registrationServices.registerSlot(waitingSkierId, slotId);
                    return true;
                } catch (BusinessException fullSlot) {
                    return false;
                }
            });

            if (!ready.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Workers did not become ready");
            start.countDown();
            cancellation.get(60, TimeUnit.SECONDS);
            boolean newRegistrationAccepted = registration.get(60, TimeUnit.SECONDS);

            long activeRegistrations = registrations.findByPlanningSlotId(slotId).stream()
                    .filter(candidate -> !candidate.isCancelled()).count();
            assertEquals(newRegistrationAccepted ? 1L : 0L, activeRegistrations);
            assertEquals(newRegistrationAccepted ? SlotStatus.FULL : SlotStatus.OPEN,
                    slots.findById(slotId).orElseThrow(AssertionError::new).getStatus());
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void simultaneousRegistrationsNeverExceedSlotCapacity() throws Exception {
        int capacity = 3;
        int attempts = 8;
        Course course = createCourse();
        Instructor instructor = createInstructor(course);
        PlanningSlot slot = createSlot(course, instructor, capacity);
        List<Skier> candidates = new ArrayList<>();
        for (int i = 0; i < attempts; i++) candidates.add(createAdultSkier());

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (Skier candidate : candidates) {
                Long skierId = candidate.getNumSkier();
                results.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting to start");
                    try {
                        registrationServices.registerSlot(skierId, slot.getId());
                        return true;
                    } catch (BusinessException expectedRefusal) {
                        return false;
                    }
                }));
            }

            if (!ready.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Workers did not become ready");
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(60, TimeUnit.SECONDS)) accepted++;
            }

            assertEquals(capacity, accepted);
            assertEquals((long) capacity, registrations.countByPlanningSlotId(slot.getId()));
            assertEquals(SlotStatus.FULL, slots.findById(slot.getId()).orElseThrow(AssertionError::new).getStatus());
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }
}
