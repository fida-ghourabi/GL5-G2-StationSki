package com.example.gestionstationskii.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Configuration
public class OpenAPIConfig {

    private static final Map<String, String> PARAMETER_DESCRIPTIONS;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("from", "Début inclus de la période, au format ISO");
        m.put("to", "Fin exclue de la période, au format ISO");
        m.put("asOf", "Date de référence incluse, au format ISO yyyy-MM-dd");
        m.put("page", "Index de page à partir de zéro");
        m.put("size", "Nombre de résultats par page, de 1 à 100");
        m.put("name", "Fragment du nom ou du prénom, insensible à la casse");
        m.put("city", "Fragment du nom de ville, insensible à la casse");
        m.put("level", "Niveau du cours, entier strictement positif");
        m.put("date1", "Date de début inclusive (yyyy-MM-dd)");
        m.put("date2", "Date de fin inclusive (yyyy-MM-dd)");
        PARAMETER_DESCRIPTIONS = Collections.unmodifiableMap(m);
    }

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(infoAPI());

    }

    public Info infoAPI() {
        return new Info().title("\uD83C\uDFBF SKI STATION MANAGEMENT \uD83D\uDEA0")
                .description("Gestion de station : skieurs, pistes, cours, moniteurs, inscriptions, planning, abonnements et paiements locaux. Dates ISO ; périodes [début, fin[ sauf indication contraire.")
                .version("1.0")
                .contact(contactAPI());
    }

    public Contact contactAPI() {
        return new Contact().name("TEAM ASI II")
                .email("ons.bensalah@esprit.tn")
                .url("https://www.linkedin.com/in/ons-ben-salah-24b73494/");
    }


    @Bean
    public GroupedOpenApi productPublicApi() {
        return GroupedOpenApi.builder()
                .group("SKI STATION Management API")
                .pathsToMatch("/**")
                .addOpenApiCustomiser(parameterDescriptions())
                .build();
    }

    @Bean
    public OpenApiCustomiser parameterDescriptions() {
        return api -> {
            if (api.getPaths() == null) return;
            api.getPaths().values().forEach(path -> path.readOperations().forEach(OpenAPIConfig::describeParameters));
        };
    }

    private static void describeParameters(Operation operation) {
        if (operation.getParameters() == null) return;
        operation.getParameters().stream()
                .filter(parameter -> parameter.getDescription() == null && parameter.getName() != null)
                .forEach(parameter -> parameter.setDescription(describe(parameter.getName())));
    }

    private static String describe(String name) {
        String known = PARAMETER_DESCRIPTIONS.get(name);
        if (known != null) return known;
        return name.toLowerCase(Locale.ROOT).contains("id") || name.startsWith("num")
                ? "Identifiant strictement positif de la ressource référencée"
                : "Valeur parmi les choix documentés dans le schéma";
    }

}