package com.logistics.routes.infrastructure.adapter.out.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalAlmacenamientoAdapterTest {

    private LocalAlmacenamientoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LocalAlmacenamientoAdapter();
    }

    // ── almacenarFoto ───────────────────────────────────────────────────

    @Test
    void almacenarFoto_retorna_url_con_extension_jpg() {
        // Given: datos de foto JPEG
        UUID paradaId = UUID.randomUUID();
        byte[] foto = "fake-image-data".getBytes();

        // When: se almacena la foto
        String url = adapter.almacenarFoto(paradaId, foto, "image/jpeg");

        // Then: la URL contiene el paradaId y extensión .jpg
        assertNotNull(url);
        assertTrue(url.contains(paradaId.toString()));
        assertTrue(url.contains("foto_"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void almacenarFoto_png_retorna_extension_png() {
        // Given: datos de foto PNG
        UUID paradaId = UUID.randomUUID();
        byte[] foto = "fake-png-data".getBytes();

        // When: se almacena
        String url = adapter.almacenarFoto(paradaId, foto, "image/png");

        // Then: extensión .png
        assertTrue(url.endsWith(".png"));
    }

    // ── almacenarFirma ──────────────────────────────────────────────────

    @Test
    void almacenarFirma_retorna_url_con_prefijo_firma() {
        // Given: datos de firma
        UUID paradaId = UUID.randomUUID();
        byte[] firma = "fake-signature-data".getBytes();

        // When: se almacena la firma
        String url = adapter.almacenarFirma(paradaId, firma, "image/png");

        // Then: la URL contiene prefijo "firma_"
        assertNotNull(url);
        assertTrue(url.contains("firma_"));
        assertTrue(url.contains(paradaId.toString()));
    }

    @Test
    void almacenarFoto_sin_content_type_no_agrega_extension() {
        // Given: foto sin content type
        UUID paradaId = UUID.randomUUID();
        byte[] foto = "data".getBytes();

        // When: se almacena sin content type
        String url = adapter.almacenarFoto(paradaId, foto, null);

        // Then: no tiene extensión de imagen conocida
        assertNotNull(url);
        assertFalse(url.endsWith(".jpg"));
        assertFalse(url.endsWith(".png"));
    }

    @Test
    void almacenar_dos_fotos_genera_urls_diferentes() {
        // Given: misma parada, dos fotos
        UUID paradaId = UUID.randomUUID();
        byte[] foto1 = "data1".getBytes();
        byte[] foto2 = "data2".getBytes();

        // When: se almacenan ambas
        String url1 = adapter.almacenarFoto(paradaId, foto1, "image/jpeg");
        String url2 = adapter.almacenarFoto(paradaId, foto2, "image/jpeg");

        // Then: URLs diferentes (UUID único en nombre)
        assertNotEquals(url1, url2);
    }
}
