package com.impactsure.sanctionui.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

@Value("${upload.base-dir}")
private String baseDir;

@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
 Path uploadPath = Paths.get(baseDir).toAbsolutePath().normalize();
 registry.addResourceHandler("/uploads/**")
     .addResourceLocations(uploadPath.toUri().toString())
     .setCachePeriod(3600);
}
}

