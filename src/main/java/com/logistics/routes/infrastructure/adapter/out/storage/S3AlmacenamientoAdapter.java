package com.logistics.routes.infrastructure.adapter.out.storage;

import com.logistics.routes.application.port.out.AlmacenamientoArchivoPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

/**
 * Implementación AWS S3 del puerto de almacenamiento de archivos.
 * Sube fotos POD y firmas al bucket configurado en {@code app.s3.bucket-pod}
 * y retorna la URI {@code s3://bucket/key}. La presigned URL para acceso de
 * lectura se genera bajo demanda al consultar la parada.
 * Solo activo bajo el perfil {@code aws}.
 */
@Component
@Profile("aws")
@RequiredArgsConstructor
public class S3AlmacenamientoAdapter implements AlmacenamientoArchivoPort {

    private static final Logger log = LoggerFactory.getLogger(S3AlmacenamientoAdapter.class);

    private final S3Client s3Client;

    @Value("${app.s3.bucket-pod}")
    private String bucket;

    @Override
    public String almacenarFoto(UUID paradaId, byte[] foto, String contentType) {
        return almacenar(paradaId, foto, contentType, "foto");
    }

    @Override
    public String almacenarFirma(UUID paradaId, byte[] firma, String contentType) {
        return almacenar(paradaId, firma, contentType, "firma");
    }

    private String almacenar(UUID paradaId, byte[] datos, String contentType, String prefijo) {
        String key = "pod/" + prefijo + "s/" + paradaId + "/" + UUID.randomUUID();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(datos)
        );
        String uri = "s3://" + bucket + "/" + key;
        log.info("[S3] {} subida a {}", prefijo, uri);
        return uri;
    }
}
