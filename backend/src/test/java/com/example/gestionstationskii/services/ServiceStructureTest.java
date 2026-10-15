package com.example.gestionstationskii.services;

import com.example.gestionstationskii.controllers.*;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/** Enforces the shared structure without starting Spring or connecting to MySQL. */
class ServiceStructureTest {
    private static final Class<?>[][] DOMAINS = {
            {ISkierServices.class, SkierServicesImpl.class, SkierRestController.class},
            {IPisteServices.class, PisteServicesImpl.class, PisteRestController.class},
            {ICourseServices.class, CourseServicesImpl.class, CourseRestController.class},
            {IInstructorServices.class, InstructorServicesImpl.class, InstructorRestController.class},
            {IRegistrationServices.class, RegistrationServicesImpl.class, RegistrationRestController.class},
            {IPlanningServices.class, PlanningServicesImpl.class, PlanningRestController.class},
            {ISubscriptionServices.class, SubscriptionServicesImpl.class, SubscriptionRestController.class},
            {IPaymentServices.class, PaymentServicesImpl.class, PaymentRestController.class}
    };

    @Test
    void everyDomainUsesAServiceContractAndConstructorInjection() throws NoSuchMethodException {
        for (Class<?>[] domain : DOMAINS) {
            Class<?> contract = domain[0], implementation = domain[1], controller = domain[2];
            assertTrue(contract.isInterface(), contract.getSimpleName());
            assertTrue(contract.isAssignableFrom(implementation), implementation.getSimpleName());
            assertNotNull(implementation.getAnnotation(Service.class));
            assertEquals(1, implementation.getConstructors().length, "One injectable constructor per implementation");
            assertEquals(1, controller.getConstructors().length, "One injectable constructor per controller");

            for (Field field : implementation.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                assertTrue(Modifier.isFinal(field.getModifiers()), field.toString());
                assertFalse(field.getType().getSimpleName().endsWith("ServicesImpl"), "Depend on service interfaces, not implementations");
            }
            for (Field field : controller.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                assertEquals(contract, field.getType(), "Controller must depend on its service interface");
                assertTrue(Modifier.isFinal(field.getModifiers()), field.toString());
            }
            for (Method method : implementation.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) continue;
                // No public business method should escape its domain's contract.
                contract.getMethod(method.getName(), method.getParameterTypes());
            }
        }
    }
}
