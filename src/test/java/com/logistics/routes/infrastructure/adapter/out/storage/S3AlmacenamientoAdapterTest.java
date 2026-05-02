package com.logistics.routes.infrastructure.adapter.out.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3AlmacenamientoAdapterTest {

    private static final String BUCKET = "logistics-pod-bucket";
    private static final String REGION = "us-east-2";

    @Mock S3Client s3Client;
    S3AlmacenamientoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new S3AlmacenamientoAdapter(s3Client, BUCKET, REGION);
    }

    @Test
    void almacenarFoto_sube_al_bucket_y_retorna_url_https() {
        UUID paradaId = UUID.randomUUID();
        byte[] foto = new byte[]{1, 2, 3, 4};

        String url = adapter.almacenarFoto(paradaId, foto, "image/jpeg");

        ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(reqCaptor.capture(), any(RequestBody.class));
        PutObjectRequest req = reqCaptor.getValue();
        assertThat(req.bucket()).isEqualTo(BUCKET);
        assertThat(req.contentType()).isEqualTo("image/jpeg");
        assertThat(req.key()).startsWith("paradas/" + paradaId + "/foto_");
        assertThat(req.key()).endsWith(".jpg");
        assertThat(url).startsWith("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/paradas/" + paradaId + "/foto_");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    void almacenarFirma_sube_al_bucket_con_prefijo_firma() {
        UUID paradaId = UUID.randomUUID();
        byte[] firma = new byte[]{9, 8, 7};

        String url = adapter.almacenarFirma(paradaId, firma, "image/png");

        ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(reqCaptor.capture(), any(RequestBody.class));
        assertThat(reqCaptor.getValue().key()).startsWith("paradas/" + paradaId + "/firma_");
        assertThat(reqCaptor.getValue().key()).endsWith(".png");
        assertThat(url).startsWith("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/paradas/" + paradaId + "/firma_");
    }

    @Test
    void cada_subida_genera_una_key_diferente_aunque_misma_parada() {
        UUID paradaId = UUID.randomUUID();
        byte[] foto = new byte[]{1};

        String url1 = adapter.almacenarFoto(paradaId, foto, "image/jpeg");
        String url2 = adapter.almacenarFoto(paradaId, foto, "image/jpeg");

        assertThat(url1).isNotEqualTo(url2);
    }
}
