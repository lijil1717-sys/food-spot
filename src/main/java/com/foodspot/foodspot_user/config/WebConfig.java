package com.foodspot.foodspot_user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Point directly to your foodspot uploads folder using forward slashes
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:L:/Project/foodspot/uploads/");
    }
}