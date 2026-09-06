package com.mediflow.securityconfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // WHY:
    // This Bean customizes the OpenAPI documentation
    // and gives our MediFlow API a proper title and version.
    @Bean
    public OpenAPI customOpenAPI() {

        // WHY:
        // This defines JWT Bearer authentication for Swagger.
        // Swagger will understand that authentication uses
        // an HTTP Bearer token containing a JWT.
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        // WHY:
        // This registers the JWT authentication scheme
        // and applies it as the default security requirement
        // for documented API operations.
        return new OpenAPI()
                .info(new Info()
                        .title("MediFlow API")
                        .version("1.0")
                        .description(
                                "MediFlow E-Pharmacy Backend API"
                        )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        bearerAuth
                                )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("bearerAuth")
                );
    }
}