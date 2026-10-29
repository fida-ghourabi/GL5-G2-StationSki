package com.example.gestionstationskii.services;

import com.example.gestionstationskii.configs.OpenAPIConfig;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigurationTest {
    private final OpenAPIConfig config = new OpenAPIConfig();

    @Test
    void publishesApiMetadataAndGroup() {
        assertEquals("1.0", config.springShopOpenAPI().getInfo().getVersion());
        assertTrue(config.infoAPI().getTitle().contains("SKI STATION"));
        assertNotNull(config.infoAPI().getDescription());
        assertEquals("TEAM ASI II", config.contactAPI().getName());
        assertNotNull(config.contactAPI().getEmail());
        assertTrue(config.contactAPI().getUrl().startsWith("https://"));
        assertEquals("SKI STATION Management API", config.productPublicApi().getGroup());
        assertTrue(config.productPublicApi().getPathsToMatch().contains("/**"));
    }

    @ParameterizedTest
    @CsvSource({
        "from,Début inclus", "to,Fin exclue", "asOf,Date de référence", "page,Index de page",
        "size,Nombre de résultats", "name,Fragment du nom", "city,Fragment du nom de ville",
        "level,Niveau du cours", "date1,Date de début", "date2,Date de fin",
        "skierId,Identifiant strictement positif", "numCourse,Identifiant strictement positif",
        "support,Valeur parmi les choix"
    })
    void describesRecognizedParameters(String name, String expectedPrefix) {
        Parameter parameter = new Parameter().name(name);
        OpenAPI api = new OpenAPI().paths(new Paths().addPathItem("/test",
                new PathItem().get(new Operation().addParametersItem(parameter))));
        config.parameterDescriptions().customise(api);
        assertTrue(parameter.getDescription().startsWith(expectedPrefix), parameter.getDescription());
    }

    @Test
    void toleratesMissingPathsOperationsAndPreservesExplicitDescriptions() {
        assertDoesNotThrow(() -> config.parameterDescriptions().customise(new OpenAPI()));
        Parameter explicit = new Parameter().name("id").description("Keep this");
        Parameter unnamed = new Parameter();
        Paths paths = new Paths().addPathItem("/empty", new PathItem().get(new Operation()))
                .addPathItem("/parameters", new PathItem().get(new Operation()
                        .addParametersItem(explicit).addParametersItem(unnamed)));
        config.parameterDescriptions().customise(new OpenAPI().paths(paths));
        assertEquals("Keep this", explicit.getDescription());
        assertNull(unnamed.getDescription());
    }
}
