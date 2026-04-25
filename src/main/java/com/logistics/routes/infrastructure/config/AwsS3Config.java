package com.logistics.routes.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Construye {@link S3Client} solo bajo el perfil {@code aws}.
 * Los autoconfigs nativos de Spring Cloud AWS S3 están excluidos en
 * {@link com.logistics.routes.LogisticsRoutesApplication} porque fallan al
 * arrancar dev/test sin credenciales AWS configuradas.
 */
@Configuration
@Profile("aws")
public class AwsS3Config {

    @Value("${spring.cloud.aws.region.static:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
