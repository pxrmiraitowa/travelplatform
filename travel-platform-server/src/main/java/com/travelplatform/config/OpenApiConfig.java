package com.travelplatform.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI travelPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("出行旅游平台 API")
                        .description("软件工程课程大作业后端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact().name("travel-platform-team"))
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://example.com"));
    }
}
