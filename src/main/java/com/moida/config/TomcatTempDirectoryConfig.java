package com.moida.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class TomcatTempDirectoryConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatTempDirectoryCustomizer(
            @Value("${server.tomcat.basedir:build/tomcat}") String tomcatBaseDir
    ) {
        return factory -> {
            Path baseDir = Path.of(tomcatBaseDir).toAbsolutePath().normalize();
            Path docBaseDir = baseDir.resolve("docbase");

            createDirectory(baseDir);
            createDirectory(docBaseDir);

            factory.setBaseDirectory(baseDir.toFile());
            factory.setDocumentRoot(docBaseDir.toFile());
        };
    }

    private static void createDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create Tomcat working directory: " + path, e);
        }
    }
}
