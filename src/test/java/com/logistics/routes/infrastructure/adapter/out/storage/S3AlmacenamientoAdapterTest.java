package com.logistics.routes.infrastructure.adapter.out.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Mock S3Client s3Client;
    S3AlmacenamientoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new S3AlmacenamientoAdapter(s3Client);
        ReflectionTestUtils.setField(adapter, "bucket", BUCKET);
    }

    @Test
    void almacenarFoto_sube_al_bucket_y_retorna_uri_s3() {
        UUID paradaId = UUID.randomUUID();
        byte[] foto = new byte[]{1, 2, 3, 4};

        String uri = adapter.almacenarFoto(paradaId, foto, "image/jpeg");

        ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(reqCaptor.capture(), any(RequestBody.class));
        PutObjectRequest req = reqCaptor.getValue();
        assertThat(req.bucket()).isEqualTo(BUCKET);
        assertThat(req.contentType()).isEqualTo("image/jpeg");
        assertThat(req.key()).startsWith("pod/fotos/" + paradaId + "/");
        assertThat(uri).startsWith("s3://" + BUCKET + "/pod/fotos/" + paradaId + "/");
    }

    @Test
    void almacenarFirma_sube_al_bucket_con_prefijo_firmas() {
        UUID paradaId = UUID.randomUUID();
        byte[] firma = new byte[]{9, 8, 7};

        String uri = adapter.almacenarFirma(paradaId, firma, "image/png");

        ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(reqCaptor.capture(), any(RequestBody.class));
        assertThat(reqCaptor.getValue().key()).startsWith("pod/firmas/" + paradaId + "/");
        assertThat(uri).startsWith("s3://" + BUCKET + "/pod/firmas/" + paradaId + "/");
    }

    @Test
    void cada_subida_genera_una_key_diferente_aunque_misma_parada() {
        UUID paradaId = UUID.randomUUID();
        byte[] foto = new byte[]{1};

        String uri1 = adapter.almacenarFoto(paradaId, foto, "image/jpeg");
        String uri2 = adapter.almacenarFoto(paradaId, foto, "image/jpeg");

        assertThat(uri1).isNotEqualTo(uri2);
    }
}
