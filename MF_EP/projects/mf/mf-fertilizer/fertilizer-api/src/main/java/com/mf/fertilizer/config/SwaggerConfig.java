package com.mf.fertilizer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("苗丰施肥管控平台 API")
                        .version("1.0.0")
                        .description("树木/苗木施肥管理后台系统接口文档")
                        .contact(new Contact().name("MF-Fertilizer")));
    }
}
