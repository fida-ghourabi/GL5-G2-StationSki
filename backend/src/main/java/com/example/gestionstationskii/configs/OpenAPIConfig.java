package com.example.gestionstationskii.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

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
    public org.springdoc.core.customizers.OpenApiCustomiser parameterDescriptions() {
        return api -> {
            if (api.getPaths() == null) return;
            api.getPaths().values().forEach(path ->
                    path.readOperations().forEach(this::describeOperationParameters));
        };
    }

    private void describeOperationParameters(Operation operation) {
        if (operation.getParameters() == null) return;
        operation.getParameters().forEach(this::describeParameter);
    }

    private void describeParameter(Parameter parameter) {
        if (parameter.getDescription() != null || parameter.getName() == null) return;
        parameter.setDescription(descriptionFor(parameter.getName()));
    }

    private String descriptionFor(String name) {
        switch (name) {
            case "from": return "Début inclus de la période, au format ISO";
            case "to": return "Fin exclue de la période, au format ISO";
            case "asOf": return "Date de référence incluse, au format ISO yyyy-MM-dd";
            case "page": return "Index de page à partir de zéro";
            case "size": return "Nombre de résultats par page, de 1 à 100";
            case "name": return "Fragment du nom ou du prénom, insensible à la casse";
            case "city": return "Fragment du nom de ville, insensible à la casse";
            case "level": return "Niveau du cours, entier strictement positif";
            case "date1": return "Date de début inclusive (yyyy-MM-dd)";
            case "date2": return "Date de fin inclusive (yyyy-MM-dd)";
            default: return isIdentifier(name)
                    ? "Identifiant strictement positif de la ressource référencée"
                    : "Valeur parmi les choix documentés dans le schéma";
        }
    }

    private boolean isIdentifier(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).contains("id") || name.startsWith("num");
    }


}
