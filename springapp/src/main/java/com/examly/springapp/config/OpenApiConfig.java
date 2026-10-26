package com.examly.springapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI gemvaultOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("/").description("Current backend"))
                .info(new Info()
                        .title("GemVault API")
                        .version("1.0.0")
                        .description("GemVault backend API. Protected endpoints require a JWT bearer token; "
                                + "public catalogue and authentication endpoints accept unauthenticated requests."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
