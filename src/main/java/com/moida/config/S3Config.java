package com.moida.config;

import com.moida.domain.product.ProductImageStorageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(ProductImageStorageProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
