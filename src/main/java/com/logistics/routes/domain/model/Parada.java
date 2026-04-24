package com.logistics.routes.domain.model;

import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.MotivoNovedad;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.exception.ParadaSinPODException;
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
    private MotivoNovedad motivoNovedad;
    private Instant fechaHoraGestion;
    private String firmaReceptorUrl;
    private String fotoEvidenciaUrl;
    private String nombreReceptor;
    private OrigenParada origen;

    private Parada(UUID id, UUID rutaId, UUID paqueteId, int orden,
                   String direccion, double latitud, double longitud,
                   String tipoMercancia, String metodoPago,
                   Instant fechaLimiteEntrega, EstadoParada estado,
                   MotivoNovedad motivoNovedad, Instant fechaHoraGestion,
                   String firmaReceptorUrl, String fotoEvidenciaUrl,
                   String nombreReceptor, OrigenParada origen) {
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
        this.motivoNovedad = motivoNovedad;
        this.fechaHoraGestion = fechaHoraGestion;
        this.firmaReceptorUrl = firmaReceptorUrl;
        this.fotoEvidenciaUrl = fotoEvidenciaUrl;
        this.nombreReceptor = nombreReceptor;
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
                EstadoParada.PENDIENTE, null, null,
                null, null, null, OrigenParada.SISTEMA
        );
    }

    public static Parada reconstituir(UUID id, UUID rutaId, UUID paqueteId, int orden,
                                      String direccion, double latitud, double longitud,
                                      String tipoMercancia, String metodoPago,
                                      Instant fechaLimiteEntrega, EstadoParada estado,
                                      MotivoNovedad motivoNovedad, Instant fechaHoraGestion,
                                      String firmaReceptorUrl, String fotoEvidenciaUrl,
                                      String nombreReceptor, OrigenParada origen) {
        return new Parada(id, rutaId, paqueteId, orden, direccion, latitud, longitud,
                tipoMercancia, metodoPago, fechaLimiteEntrega, estado,
                motivoNovedad, fechaHoraGestion, firmaReceptorUrl, fotoEvidenciaUrl,
                nombreReceptor, origen);
    }

    // ── Métodos de gestión de campo ──────────────────────────────────────

    public void marcarExitosa(String fotoUrl, String firmaUrl,
                              String nombreReceptor, Instant fechaHoraAccion) {
        if (fotoUrl == null || fotoUrl.isBlank()) {
            throw new ParadaSinPODException(this.paqueteId);
        }
        this.estado = EstadoParada.EXITOSA;
        this.fotoEvidenciaUrl = fotoUrl;
        this.firmaReceptorUrl = firmaUrl;
        this.nombreReceptor = nombreReceptor;
        this.fechaHoraGestion = fechaHoraAccion;
        this.origen = OrigenParada.CONDUCTOR;
    }

    public void marcarFallida(MotivoNovedad motivo, Instant fechaHoraAccion) {
        this.estado = EstadoParada.FALLIDA;
        this.motivoNovedad = motivo;
        this.fechaHoraGestion = fechaHoraAccion;
        this.origen = OrigenParada.CONDUCTOR;
    }

    public void marcarNovedad(MotivoNovedad tipoNovedad, Instant fechaHoraAccion) {
        this.estado = EstadoParada.NOVEDAD;
        this.motivoNovedad = tipoNovedad;
        this.fechaHoraGestion = fechaHoraAccion;
        this.origen = OrigenParada.CONDUCTOR;
    }

    public void marcarSinGestion(Instant fechaHoraAccion) {
        this.estado = EstadoParada.SIN_GESTION_CONDUCTOR;
        this.fechaHoraGestion = fechaHoraAccion;
        this.origen = OrigenParada.SISTEMA;
    }

    public boolean esPODValido() {
        return fotoEvidenciaUrl != null && !fotoEvidenciaUrl.isBlank();
    }

    // ── Otros métodos de dominio ─────────────────────────────────────────

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
