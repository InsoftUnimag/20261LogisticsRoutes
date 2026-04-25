package com.logistics.routes.infrastructure.adapter.out.storage;

import com.logistics.routes.application.port.out.AlmacenamientoArchivoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@Profile("!aws")
public class LocalAlmacenamientoAdapter implements AlmacenamientoArchivoPort {

    private static final Logger log = LoggerFactory.getLogger(LocalAlmacenamientoAdapter.class);
    private static final Path BASE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "logistics", "fotos");

    @Override
    public String almacenarFoto(UUID paradaId, byte[] foto, String contentType) {
        return almacenar(paradaId, foto, contentType, "foto");
    }

    @Override
    public String almacenarFirma(UUID paradaId, byte[] firma, String contentType) {
        return almacenar(paradaId, firma, contentType, "firma");
    }

    private String almacenar(UUID paradaId, byte[] datos, String contentType, String prefijo) {
        try {
            Path dir = BASE_DIR.resolve(paradaId.toString());
            Files.createDirectories(dir);

            String extension = resolverExtension(contentType);
            String nombreArchivo = prefijo + "_" + UUID.randomUUID() + extension;
            Path archivo = dir.resolve(nombreArchivo);

            Files.write(archivo, datos);
            log.info("Archivo almacenado localmente: {}", archivo);

            return archivo.toUri().toString();
        } catch (IOException e) {
            throw new RuntimeException("Error almacenando archivo para parada " + paradaId, e);
        }
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
