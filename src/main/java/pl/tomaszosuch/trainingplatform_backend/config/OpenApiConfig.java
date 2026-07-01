package pl.tomaszosuch.trainingplatform_backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI trainingPlatformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Platforma Treningowa API")
                        .description("""
                                REST API dla aplikacji do planowania i rejestrowania treningów \
                                (taniec, gimnastyka, ogólnorozwojowe).

                                ## Autoryzacja
                                Większość endpointów wymaga tokenu JWT. Aby go uzyskać:
                                1. Zarejestruj konto przez `POST /auth/register`
                                2. Zaloguj się przez `POST /auth/login` — otrzymasz token
                                3. Kliknij przycisk **Authorize** u góry i wklej token

                                ## Role
                                - **USER** — zwykły użytkownik (zarządza własnymi treningami)
                                - **ADMIN** — administrator (zarządza kategoriami)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Tomasz Osuch")
                                .url("https://tomaszosuch.dev"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/api").description("Serwer lokalny (dev)")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Wklej token JWT otrzymany z /auth/login")));
    }
}
