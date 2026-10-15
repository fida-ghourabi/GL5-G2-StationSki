package com.example.gestionstationskii.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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
                .description("Gestion de station : skieurs, pistes, cours, planning, matériel, paiements locaux et rapports. Dates ISO ; périodes [début, fin[ sauf indication contraire.")
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
            api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                if (operation.getParameters() == null) return;
                operation.getParameters().forEach(parameter -> {
                    if (parameter.getDescription() != null) return;
                    String name = parameter.getName();
                    if (name == null) return;
                    String description;
                    switch (name) {
                        case "from": description = "Début inclus de la période, au format ISO"; break;
                        case "to": description = "Fin exclue de la période, au format ISO"; break;
                        case "asOf": description = "Date de référence incluse, au format ISO yyyy-MM-dd"; break;
                        case "page": description = "Index de page à partir de zéro"; break;
                        case "size": description = "Nombre de résultats par page, de 1 à 100"; break;
                        case "name": description = "Fragment du nom ou du prénom, insensible à la casse"; break;
                        case "city": description = "Fragment du nom de ville, insensible à la casse"; break;
                        case "level": description = "Niveau du cours, entier strictement positif"; break;
                        case "date1": description = "Date de début inclusive (yyyy-MM-dd)"; break;
                        case "date2": description = "Date de fin inclusive (yyyy-MM-dd)"; break;
                        default:
                            description = name.toLowerCase(java.util.Locale.ROOT).contains("id") || name.startsWith("num")
                                    ? "Identifiant strictement positif de la ressource référencée"
                                    : "Valeur parmi les choix documentés dans le schéma";
                    }
                    parameter.setDescription(description);
                });
            }));
        };
    }


}
