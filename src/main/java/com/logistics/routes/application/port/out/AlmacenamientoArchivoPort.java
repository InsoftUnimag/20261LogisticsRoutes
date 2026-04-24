package com.logistics.routes.application.port.out;

import java.util.UUID;

public interface AlmacenamientoArchivoPort {

    String almacenarFoto(UUID paradaId, byte[] foto, String contentType);

    String almacenarFirma(UUID paradaId, byte[] firma, String contentType);
}
