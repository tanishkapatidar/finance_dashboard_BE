package com.finance.dashboard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Finance Dashboard API")
                .description("""
                    Backend API for the Finance Dashboard system.

                    **Roles:**
                    - `VIEWER` — Can view dashboard summary data only
                    - `ANALYST` — Can view records and access insights/summaries
                    - `ADMIN` — Full access: manage records, users, and all data

                    **Authentication:** Use `POST /api/auth/login` to get a JWT token,
                    then click **Authorize** and enter `Bearer <token>`.

                    **Default credentials (seeded on startup):**
                    - admin / admin123
                    - analyst / analyst123
                    - viewer / viewer123
                    """)
                .version("1.0.0")
                .contact(new Contact().name("Finance Dashboard Team")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Provide the JWT token. Example: Bearer eyJhbGci...")));
    }
}
