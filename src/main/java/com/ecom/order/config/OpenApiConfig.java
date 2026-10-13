package com.ecom.order.config;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version("1.0.0")
                        .description("Microservice responsible for managing customer orders and checkout workflows.")
                        .contact(new Contact()
                                .name("Bazaar Engineering Team")
                                .email("support@bazaar.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org"))
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token required for secured endpoints. Example: 'Bearer <token>'")));
    }

    @Bean
    GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("1-all-apis")
                .displayName("1. All Order APIs")
                .pathsToMatch("/**")
                .build();
    }
}
