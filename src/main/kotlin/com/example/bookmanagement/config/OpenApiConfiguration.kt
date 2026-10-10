package com.example.bookmanagement.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    @Bean
    fun bookManagementOpenApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("Book Management API")
            .version("v1")
            .description("書籍・著者管理REST API"),
    )
}
