package com.logistics.routes;

import io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration;
import io.awspring.cloud.autoconfigure.s3.S3CrtAsyncClientAutoConfiguration;
import io.awspring.cloud.autoconfigure.s3.S3TransferManagerAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Excluimos los autoconfigs de Spring Cloud AWS S3 porque intentan crear
 * S3Client al arrancar y fallan en perfiles dev/test sin credenciales AWS.
 * En perfil "aws" construimos S3Client manualmente vía
 * {@link com.logistics.routes.infrastructure.config.AwsS3Config}.
 */
@SpringBootApplication(exclude = {
        S3AutoConfiguration.class,
        S3CrtAsyncClientAutoConfiguration.class,
        S3TransferManagerAutoConfiguration.class
})
@EnableScheduling
public class LogisticsRoutesApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsRoutesApplication.class, args);
    }
}
