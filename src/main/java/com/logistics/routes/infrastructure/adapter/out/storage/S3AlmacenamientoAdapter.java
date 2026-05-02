package com.logistics.routes.infrastructure.adapter.out.storage;

import com.logistics.routes.application.port.out.AlmacenamientoArchivoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Component
@Profile("aws")
public class S3AlmacenamientoAdapter implements AlmacenamientoArchivoPort {

    private static final Logger log = LoggerFactory.getLogger(S3AlmacenamientoAdapter.class);

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public S3AlmacenamientoAdapter(S3Client s3Client,
                                   @Value("${app.s3.bucket-pod}") String bucket,
                                   @Value("${spring.cloud.aws.region.static:us-east-1}") String region) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
    }

    @Override
    public String almacenarFoto(UUID paradaId, byte[] foto, String contentType) {
        return almacenar(paradaId, foto, contentType, "foto");
    }

    @Override
    public String almacenarFirma(UUID paradaId, byte[] firma, String contentType) {
        return almacenar(paradaId, firma, contentType, "firma");
    }

    private String almacenar(UUID paradaId, byte[] datos, String contentType, String prefijo) {
        String extension = resolverExtension(contentType);
        String key = "paradas/" + paradaId + "/" + prefijo + "_" + UUID.randomUUID() + extension;
        String ct = contentType != null ? contentType : "application/octet-stream";

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(ct)
                        .build(),
                RequestBody.fromBytes(datos)
        );

        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        log.info("[S3] {} subida a {}", prefijo, url);
        return url;
    }

    private String resolverExtension(String contentType) {
        if (contentType == null) return "";
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}
