package com.example.gestionstationskii.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenAPIConfigTest {
    private final OpenAPIConfig config = new OpenAPIConfig();

    @Test
    void apiBeansContainTheExpectedMetadata() {
        OpenAPI openApi = config.springShopOpenAPI();
        GroupedOpenApi groupedApi = config.productPublicApi();

        assertNotNull(groupedApi);
        assertEquals("🎿 SKI STATION MANAGEMENT 🚠", openApi.getInfo().getTitle());
        assertEquals("1.0", openApi.getInfo().getVersion());
        assertEquals("TEAM ASI II", openApi.getInfo().getContact().getName());
        assertEquals("ons.bensalah@esprit.tn", config.contactAPI().getEmail());
    }

    @Test
    void customizerReturnsWhenApiHasNoPaths() {
        OpenApiCustomiser customiser = config.parameterDescriptions();

        assertDoesNotThrow(() -> customiser.customise(new OpenAPI()));
    }

    @Test
    void customizerReturnsWhenOperationHasNoParameters() {
        OpenAPI openApi = openApiWithOperation(new Operation());

        config.parameterDescriptions().customise(openApi);

        assertNotNull(openApi.getPaths());
    }

    @Test
    void customizerDescribesKnownAndFallbackParameters() {
        List<Parameter> parameters = new ArrayList<>(Arrays.asList(
                parameter("from"), parameter("to"), parameter("asOf"),
                parameter("page"), parameter("size"), parameter("name"),
                parameter("city"), parameter("level"), parameter("date1"),
                parameter("date2"), parameter("skierId"), parameter("numCourse"),
                parameter("support"), parameter("existing", "Keep this description"),
                parameter(null)));
        OpenAPI openApi = openApiWithOperation(new Operation().parameters(parameters));

        config.parameterDescriptions().customise(openApi);

        assertEquals("Début inclus de la période, au format ISO", parameters.get(0).getDescription());
        assertEquals("Fin exclue de la période, au format ISO", parameters.get(1).getDescription());
        assertEquals("Date de référence incluse, au format ISO yyyy-MM-dd", parameters.get(2).getDescription());
        assertEquals("Index de page à partir de zéro", parameters.get(3).getDescription());
        assertEquals("Nombre de résultats par page, de 1 à 100", parameters.get(4).getDescription());
        assertEquals("Fragment du nom ou du prénom, insensible à la casse", parameters.get(5).getDescription());
        assertEquals("Fragment du nom de ville, insensible à la casse", parameters.get(6).getDescription());
        assertEquals("Niveau du cours, entier strictement positif", parameters.get(7).getDescription());
        assertEquals("Date de début inclusive (yyyy-MM-dd)", parameters.get(8).getDescription());
        assertEquals("Date de fin inclusive (yyyy-MM-dd)", parameters.get(9).getDescription());
        assertEquals("Identifiant strictement positif de la ressource référencée", parameters.get(10).getDescription());
        assertEquals("Identifiant strictement positif de la ressource référencée", parameters.get(11).getDescription());
        assertEquals("Valeur parmi les choix documentés dans le schéma", parameters.get(12).getDescription());
        assertEquals("Keep this description", parameters.get(13).getDescription());
        assertNull(parameters.get(14).getDescription());
    }

    private static OpenAPI openApiWithOperation(Operation operation) {
        PathItem pathItem = new PathItem();
        pathItem.setGet(operation);
        Paths paths = new Paths();
        paths.addPathItem("/example", pathItem);
        return new OpenAPI().paths(paths);
    }

    private static Parameter parameter(String name) {
        return parameter(name, null);
    }

    private static Parameter parameter(String name, String description) {
        Parameter parameter = new Parameter();
        parameter.setName(name);
        parameter.setDescription(description);
        return parameter;
    }
}
