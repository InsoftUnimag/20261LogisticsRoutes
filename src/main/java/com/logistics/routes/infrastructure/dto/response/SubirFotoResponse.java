package com.logistics.routes.infrastructure.dto.response;

/**
 * Respuesta del endpoint de subida de foto POD.
 * Devuelve la URL del archivo almacenado para que el conductor la incluya
 * luego en el registro de la parada como evidencia.
 */
public record SubirFotoResponse(String url) {
}
