package com.example.gestionstationskii;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class GestionStationSkiiApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationWithProvidedArguments() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            GestionStationSkiiApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(GestionStationSkiiApplication.class, args));
        }
    }
}
