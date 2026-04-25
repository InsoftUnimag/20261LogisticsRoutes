package com.logistics.routes.domain.model;

import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.TipoCierre;
import com.logistics.routes.domain.enums.TipoVehiculo;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Ruta {

    private final UUID id;
    private final String zona;
    private EstadoRuta estado;
    private double pesoAcumuladoKg;
    private TipoVehiculo tipoVehiculoRequerido;
    private UUID vehiculoId;
    private UUID conductorId;
    private final Instant fechaCreacionRuta;
    private final Instant fechaLimiteDespacho;
    private Instant fechaHoraInicio;
    private Instant fechaHoraCierre;
    private TipoCierre tipoCierre;

    private Ruta(UUID id, String zona, EstadoRuta estado, double pesoAcumuladoKg,
                 TipoVehiculo tipoVehiculoRequerido, UUID vehiculoId, UUID conductorId,
                 Instant fechaCreacionRuta, Instant fechaLimiteDespacho,
                 Instant fechaHoraInicio, Instant fechaHoraCierre, TipoCierre tipoCierre) {
        this.id = id;
        this.zona = zona;
        this.estado = estado;
        this.pesoAcumuladoKg = pesoAcumuladoKg;
        this.tipoVehiculoRequerido = tipoVehiculoRequerido;
        this.vehiculoId = vehiculoId;
        this.conductorId = conductorId;
        this.fechaCreacionRuta = fechaCreacionRuta;
        this.fechaLimiteDespacho = fechaLimiteDespacho;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraCierre = fechaHoraCierre;
        this.tipoCierre = tipoCierre;
    }

    public static Ruta nueva(String zona, Instant fechaLimiteDespacho) {
        if (zona == null || zona.isBlank()) {
            throw new IllegalArgumentException("La zona no puede ser nula o vacía");
        }
        if (fechaLimiteDespacho == null) {
            throw new IllegalArgumentException("La fecha límite de despacho no puede ser nula");
        }
        return new Ruta(
                UUID.randomUUID(), zona, EstadoRuta.CREADA, 0.0,
                TipoVehiculo.MOTO, null, null,
                Instant.now(), fechaLimiteDespacho,
                null, null, null
        );
    }

    public static Ruta reconstituir(UUID id, String zona, EstadoRuta estado, double pesoAcumuladoKg,
                                    TipoVehiculo tipoVehiculoRequerido, UUID vehiculoId, UUID conductorId,
                                    Instant fechaCreacionRuta, Instant fechaLimiteDespacho,
                                    Instant fechaHoraInicio, Instant fechaHoraCierre, TipoCierre tipoCierre) {
        return new Ruta(id, zona, estado, pesoAcumuladoKg, tipoVehiculoRequerido,
                vehiculoId, conductorId, fechaCreacionRuta, fechaLimiteDespacho,
                fechaHoraInicio, fechaHoraCierre, tipoCierre);
    }

    public void agregarPeso(double kg) {
        if (estado != EstadoRuta.CREADA) {
            throw new IllegalStateException(
                    "Solo se puede agregar peso a una ruta en estado CREADA, estado actual: " + estado);
        }
        if (kg <= 0) {
            throw new IllegalArgumentException("El peso a agregar debe ser mayor a 0");
        }
        this.pesoAcumuladoKg += kg;
    }

    public void setTipoVehiculoRequerido(TipoVehiculo nuevo) {
        if (estado != EstadoRuta.CREADA) {
            throw new IllegalStateException(
                    "Solo se puede cambiar el tipo de vehículo en estado CREADA, estado actual: " + estado);
        }
        if (nuevo == null) {
            throw new IllegalArgumentException("El tipo de vehículo no puede ser nulo");
        }
        if (nuevo.ordinal() < this.tipoVehiculoRequerido.ordinal()) {
            throw new IllegalArgumentException(
                    "No se puede hacer downgrade de " + tipoVehiculoRequerido + " a " + nuevo);
        }
        this.tipoVehiculoRequerido = nuevo;
    }

    public void transicionarAListaParaDespacho() {
        if (estado != EstadoRuta.CREADA) {
            throw new IllegalStateException(
                    "Solo se puede transicionar a LISTA_PARA_DESPACHO desde CREADA, estado actual: " + estado);
        }
        this.estado = EstadoRuta.LISTA_PARA_DESPACHO;
    }

    public void iniciarTransito() {
        if (estado != EstadoRuta.CONFIRMADA) {
            throw new IllegalStateException(
                    "Solo se puede iniciar tránsito desde CONFIRMADA, estado actual: " + estado);
        }
        this.estado = EstadoRuta.EN_TRANSITO;
        this.fechaHoraInicio = Instant.now();
    }

    public void cerrar(EstadoRuta estadoCierre, TipoCierre tipoCierre, Instant fechaHoraCierre) {
        if (estado != EstadoRuta.EN_TRANSITO) {
            throw new IllegalStateException(
                    "Solo se puede cerrar desde EN_TRANSITO, estado actual: " + estado);
        }
        if (fechaHoraCierre == null) {
            throw new IllegalArgumentException("La fecha hora de cierre no puede ser nula");
        }
        this.estado = estadoCierre;
        this.fechaHoraCierre = fechaHoraCierre;
        this.tipoCierre = tipoCierre;
    }

    public void confirmar(UUID conductorId, UUID vehiculoId) {
        if (estado != EstadoRuta.LISTA_PARA_DESPACHO) {
            throw new IllegalStateException(
                    "Solo se puede confirmar desde LISTA_PARA_DESPACHO, estado actual: " + estado);
        }
        if (conductorId == null) {
            throw new IllegalArgumentException("El conductorId no puede ser nulo");
        }
        if (vehiculoId == null) {
            throw new IllegalArgumentException("El vehiculoId no puede ser nulo");
        }
        this.conductorId = conductorId;
        this.vehiculoId = vehiculoId;
        this.estado = EstadoRuta.CONFIRMADA;
    }
}
