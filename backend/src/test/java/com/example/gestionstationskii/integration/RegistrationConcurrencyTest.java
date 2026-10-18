package com.example.gestionstationskii.integration;

import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.PlanningSlot;
import com.example.gestionstationskii.entities.SlotStatus;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IRegistrationServices;
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
            assertEquals((long) capacity, registrations.countByPlanningSlot_Id(slot.getId()));
            assertEquals(SlotStatus.FULL, slots.findById(slot.getId()).orElseThrow(AssertionError::new).getStatus());
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }
}
