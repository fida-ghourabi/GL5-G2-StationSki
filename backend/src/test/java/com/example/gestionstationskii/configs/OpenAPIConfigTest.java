package com.example.gestionstationskii.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import org.junit.jupiter.api.Test;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class OpenAPIConfigTest {

    private final OpenAPIConfig config = new OpenAPIConfig();

    @Test
    void springShopOpenAPIExposesInfo() {
        OpenAPI api = config.springShopOpenAPI();

        assertNotNull(api.getInfo());
        assertEquals("1.0", api.getInfo().getVersion());
        assertTrue(api.getInfo().getTitle().contains("SKI STATION MANAGEMENT"));
    }

    @Test
    void infoApiContainsDescriptionAndContact() {
        Info info = config.infoAPI();

        assertNotNull(info.getDescription());
        assertNotNull(info.getContact());
        assertEquals("1.0", info.getVersion());
    }

    @Test
    void contactApiContainsTeamData() {
        Contact contact = config.contactAPI();

        assertEquals("TEAM ASI II", contact.getName());
        assertNotNull(contact.getEmail());
        assertNotNull(contact.getUrl());
    }

    @Test
    void productPublicApiDefinesGroupAndPaths() {
        GroupedOpenApi grouped = config.productPublicApi();

        assertEquals("SKI STATION Management API", grouped.getGroup());
        assertTrue(grouped.getPathsToMatch().contains("/**"));
    }

    @Test
    void customiserIgnoresApiWithoutPaths() {
        OpenApiCustomiser customiser = config.parameterDescriptions();
        OpenAPI api = new OpenAPI();

        customiser.customise(api);

        assertNull(api.getPaths());
    }

    @Test
    void customiserIgnoresOperationWithoutParameters() {
        OpenApiCustomiser customiser = config.parameterDescriptions();
        Operation operation = new Operation();
        OpenAPI api = apiWith(operation);

        customiser.customise(api);

        assertNull(operation.getParameters());
    }

    @Test
    void customiserUsesKnownDescriptions() {
        Parameter from = parameter("from");
        Parameter to = parameter("to");
        Parameter page = parameter("page");
        Operation operation = new Operation();
        operation.setParameters(Arrays.asList(from, to, page));

        config.parameterDescriptions().customise(apiWith(operation));

        assertTrue(from.getDescription().contains("au format ISO"));
        assertTrue(to.getDescription().startsWith("Fin exclue"));
        assertTrue(page.getDescription().startsWith("Index de page"));
    }

    @Test
    void customiserDescribesIdentifierParameters() {
        Parameter byId = parameter("courseId");
        Parameter byNum = parameter("numCourse");
        Operation operation = new Operation();
        operation.setParameters(Arrays.asList(byId, byNum));

        config.parameterDescriptions().customise(apiWith(operation));

        assertTrue(byId.getDescription().startsWith("Identifiant"));
        assertTrue(byNum.getDescription().startsWith("Identifiant"));
    }

    @Test
    void customiserUsesDefaultDescriptionForUnknownParameter() {
        Parameter support = parameter("support");
        Operation operation = new Operation();
        operation.setParameters(Arrays.asList(support));

        config.parameterDescriptions().customise(apiWith(operation));

        assertTrue(support.getDescription().startsWith("Valeur parmi"));
    }

    @Test
    void customiserKeepsExistingDescription() {
        Parameter described = parameter("from");
        described.setDescription("Description personnalisee");
        Operation operation = new Operation();
        operation.setParameters(Arrays.asList(described));

        config.parameterDescriptions().customise(apiWith(operation));

        assertEquals("Description personnalisee", described.getDescription());
    }

    @Test
    void customiserSkipsParameterWithoutName() {
        Parameter unnamed = new QueryParameter();
        Operation operation = new Operation();
        operation.setParameters(Arrays.asList(unnamed));

        config.parameterDescriptions().customise(apiWith(operation));

        assertNull(unnamed.getDescription());
    }

    private Parameter parameter(String name) {
        Parameter parameter = new QueryParameter();
        parameter.setName(name);
        return parameter;
    }

    private OpenAPI apiWith(Operation operation) {
        PathItem item = new PathItem();
        item.setGet(operation);
        Paths paths = new Paths();
        paths.addPathItem("/test", item);
        OpenAPI api = new OpenAPI();
        api.setPaths(paths);
        return api;
    }
}