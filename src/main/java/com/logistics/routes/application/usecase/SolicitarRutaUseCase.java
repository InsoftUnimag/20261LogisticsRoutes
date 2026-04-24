package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.SolicitarRutaCommand;
import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.FechaLimiteVencidaException;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.valueobject.ZonaGeografica;
import com.logistics.routes.infrastructure.helper.RutaCreadorTransaccional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SolicitarRutaUseCase {

    private static final double UMBRAL_CAPACIDAD = 90.0;
    private static final int PLAZO_DESPACHO_DIAS = 5;

    private final RutaRepositoryPort rutaRepository;
    private final ParadaRepositoryPort paradaRepository;
    private final NotificacionDespachadorPort notificacion;
    private final RutaCreadorTransaccional rutaCreador;

    public UUID ejecutar(SolicitarRutaCommand command) {
        validarFechaLimite(command);

        ZonaGeografica zona = ZonaGeografica.from(command.latitud(), command.longitud());
        Ruta ruta = resolverRuta(zona);

        ruta.agregarPeso(command.pesoKg());
        evaluarEscaladoOTransicion(ruta);
        rutaRepository.guardar(ruta);

        Parada parada = Parada.nueva(
                ruta.getId(), command.paqueteId(), command.direccion(),
                command.latitud(), command.longitud(),
                command.tipoMercancia(), command.metodoPago(), command.fechaLimiteEntrega());
        paradaRepository.guardar(parada);

        return ruta.getId();
    }

    private void validarFechaLimite(SolicitarRutaCommand command) {
        if (command.fechaLimiteEntrega() != null && command.fechaLimiteEntrega().isBefore(Instant.now())) {
            notificacion.notificarAlertaPrioritaria(
                    "Paquete " + command.paqueteId() + " rechazado: fecha límite vencida ("
                            + command.fechaLimiteEntrega() + ")");
            throw new FechaLimiteVencidaException(command.fechaLimiteEntrega());
        }
    }

    private Ruta resolverRuta(ZonaGeografica zona) {
        return rutaRepository.buscarRutaActivaPorZona(zona)
                .orElseGet(() -> crearRutaEnZona(zona));
    }

    private Ruta crearRutaEnZona(ZonaGeografica zona) {
        Ruta nueva = Ruta.nueva(
                zona.hash(),
                Instant.now().plus(PLAZO_DESPACHO_DIAS, ChronoUnit.DAYS));
        try {
            return rutaCreador.guardarNueva(nueva);
        } catch (DataIntegrityViolationException e) {
            // Condición de carrera: otro hilo creó la ruta en la misma zona
            // simultáneamente.
            // El índice único idx_rutas_zona_creada rechazó el duplicado — re-buscamos la
            // existente.
            return rutaRepository.buscarRutaActivaPorZona(zona)
                    .orElseThrow(() -> new IllegalStateException(
                            "Condición de carrera sin resolución en zona: " + zona.hash()));
        }
    }

    private void evaluarEscaladoOTransicion(Ruta ruta) {
        TipoVehiculo tipoActual = ruta.getTipoVehiculoRequerido();
        if (!tipoActual.porcentajeExcede(ruta.getPesoAcumuladoKg(), UMBRAL_CAPACIDAD)) {
            return;
        }
        tipoActual.siguienteTipo().ifPresentOrElse(
                ruta::setTipoVehiculoRequerido,
                () -> {
                    ruta.transicionarAListaParaDespacho();
                    notificacion.notificarRutaListaParaDespacho(
                            ruta.getId(), ruta.getZona(),
                            ruta.getPesoAcumuladoKg(), ruta.getTipoVehiculoRequerido(),
                            "capacidad_maxima_alcanzada");
                });
    }
}
