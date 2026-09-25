package dev.lucas.car_microservice.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Configuration
public class StorageWebConfig implements WebMvcConfigurer {

    @Value("${STORAGE_DIR:storage}")
    private String storageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(FileStorageService.PUBLIC_PREFIX + "**")
                .addResourceLocations(Path.of(storageDir).toAbsolutePath().normalize().toUri().toString())
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS));
    }
}
