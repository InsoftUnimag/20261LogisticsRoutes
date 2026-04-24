package com.logistics.routes.domain.model;

import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.OrigenParada;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Parada {

    private final UUID id;
    private final UUID rutaId;
    private final UUID paqueteId;
    private int orden;
    private final String direccion;
    private final double latitud;
    private final double longitud;
    private final String tipoMercancia;
    private final String metodoPago;
    private final Instant fechaLimiteEntrega;
    private EstadoParada estado;
    private OrigenParada origen;

    private Parada(UUID id, UUID rutaId, UUID paqueteId, int orden,
                   String direccion, double latitud, double longitud,
                   String tipoMercancia, String metodoPago,
                   Instant fechaLimiteEntrega, EstadoParada estado,
                   OrigenParada origen) {
        this.id = id;
        this.rutaId = rutaId;
        this.paqueteId = paqueteId;
        this.orden = orden;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.tipoMercancia = tipoMercancia;
        this.metodoPago = metodoPago;
        this.fechaLimiteEntrega = fechaLimiteEntrega;
        this.estado = estado;
        this.origen = origen;
    }

    public static Parada nueva(UUID rutaId, UUID paqueteId, String direccion,
                               double latitud, double longitud,
                               String tipoMercancia, String metodoPago,
                               Instant fechaLimiteEntrega) {
        if (rutaId == null) {
            throw new IllegalArgumentException("El rutaId no puede ser nulo");
        }
        if (paqueteId == null) {
            throw new IllegalArgumentException("El paqueteId no puede ser nulo");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("La dirección no puede ser nula o vacía");
        }
        return new Parada(
                UUID.randomUUID(), rutaId, paqueteId, 0,
                direccion, latitud, longitud,
                tipoMercancia, metodoPago, fechaLimiteEntrega,
                EstadoParada.PENDIENTE, OrigenParada.SISTEMA
        );
    }

    public static Parada reconstituir(UUID id, UUID rutaId, UUID paqueteId, int orden,
                                      String direccion, double latitud, double longitud,
                                      String tipoMercancia, String metodoPago,
                                      Instant fechaLimiteEntrega, EstadoParada estado,
                                      OrigenParada origen) {
        return new Parada(id, rutaId, paqueteId, orden, direccion, latitud, longitud,
                tipoMercancia, metodoPago, fechaLimiteEntrega, estado, origen);
    }

    public void marcarExcluidaDespacho() {
        if (estado != EstadoParada.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se puede excluir una parada en estado PENDIENTE, estado actual: " + estado);
        }
        this.estado = EstadoParada.EXCLUIDA_DESPACHO;
    }

    public void asignarOrden(int orden) {
        if (orden < 0) {
            throw new IllegalArgumentException("El orden no puede ser negativo");
        }
        this.orden = orden;
    }
}
