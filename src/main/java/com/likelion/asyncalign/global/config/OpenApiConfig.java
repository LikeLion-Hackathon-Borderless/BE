package com.likelion.asyncalign.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI dittoOpenApi(@Value("${app.public-base-url}") String publicBaseUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("ditto API")
                        .description("글로벌 비동기 협업을 위한 JWT 인증, 워크스페이스, 사용자, 1:1 메신저 API")
                        .version("1.0.0"))
                .servers(java.util.List.of(
                        new Server()
                                .url(publicBaseUrl)
                                .description("현재 실행 환경")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 응답의 accessToken을 입력합니다.")));
    }
}
