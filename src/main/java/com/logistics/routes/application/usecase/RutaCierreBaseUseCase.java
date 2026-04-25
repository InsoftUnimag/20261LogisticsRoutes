package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.IntegracionModulo3Port;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.TipoCierre;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.exception.ParadasPendientesException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.exception.RutaNoEnTransitoException;
import com.logistics.routes.domain.exception.VehiculoNoEncontradoException;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.model.Vehiculo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Base compartida para los tres use cases de cierre de ruta.
 * Encapsula la lógica común: validación, gestión de pendientes,
 * transición de estado, publicación de eventos y liberación de recursos.
 */
public abstract class RutaCierreBaseUseCase {

    protected final RutaRepositoryPort rutaRepository;
    protected final ParadaRepositoryPort paradaRepository;
    protected final ConductorRepositoryPort conductorRepository;
    protected final VehiculoRepositoryPort vehiculoRepository;
    protected final IntegracionModulo1Port integracionModulo1;
    protected final IntegracionModulo3Port integracionModulo3;

    protected RutaCierreBaseUseCase(
            RutaRepositoryPort rutaRepository,
            ParadaRepositoryPort paradaRepository,
            ConductorRepositoryPort conductorRepository,
            VehiculoRepositoryPort vehiculoRepository,
            IntegracionModulo1Port integracionModulo1,
            IntegracionModulo3Port integracionModulo3) {
        this.rutaRepository = rutaRepository;
        this.paradaRepository = paradaRepository;
        this.conductorRepository = conductorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.integracionModulo1 = integracionModulo1;
        this.integracionModulo3 = integracionModulo3;
    }

    /**
     * Lógica compartida de cierre.
     *
     * @param rutaId                 ID de la ruta a cerrar
     * @param confirmarConPendientes si true, marca paradas PENDIENTE como SIN_GESTION;
     *                               si false y hay pendientes, lanza ParadasPendientesException
     * @param tipoCierre             tipo de cierre que determina el estado final de la ruta
     */
    protected void cerrar(UUID rutaId, boolean confirmarConPendientes, TipoCierre tipoCierre) {
        Ruta ruta = rutaRepository.buscarPorId(rutaId)
                .orElseThrow(() -> new RutaNoEncontradaException(rutaId));

        if (ruta.getEstado() != EstadoRuta.EN_TRANSITO) {
            throw new RutaNoEnTransitoException(rutaId);
        }

        List<Parada> todasLasParadas = paradaRepository.buscarPorRutaId(rutaId);

        List<Parada> pendientes = todasLasParadas.stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .toList();

        if (!pendientes.isEmpty() && !confirmarConPendientes) {
            throw new ParadasPendientesException(pendientes.size());
        }

        Instant ahora = Instant.now();

        if (!pendientes.isEmpty()) {
            pendientes.forEach(p -> p.marcarSinGestion(ahora));
            paradaRepository.guardarTodas(pendientes);
            List<UUID> paqueteIds = pendientes.stream().map(Parada::getPaqueteId).toList();
            integracionModulo1.publishParadasSinGestionar(rutaId, tipoCierre, paqueteIds);
        }

        EstadoRuta estadoCierre = switch (tipoCierre) {
            case MANUAL -> EstadoRuta.CERRADA_MANUAL;
            case AUTOMATICO -> EstadoRuta.CERRADA_AUTOMATICA;
            case FORZADO_DESPACHADOR -> EstadoRuta.CERRADA_FORZADA;
        };

        ruta.cerrar(estadoCierre, tipoCierre);
        rutaRepository.guardar(ruta);

        Conductor conductor = conductorRepository.buscarPorId(ruta.getConductorId())
                .orElseThrow(() -> new ConductorNoEncontradoException(ruta.getConductorId()));
        Vehiculo vehiculo = vehiculoRepository.buscarPorId(ruta.getVehiculoId())
                .orElseThrow(() -> new VehiculoNoEncontradoException(ruta.getVehiculoId()));

        integracionModulo3.publishRutaCerrada(
                RutaCerradaEvent.from(ruta, conductor, vehiculo, todasLasParadas));

        conductor.marcarActivo();
        vehiculo.marcarDisponible();
        conductorRepository.guardar(conductor);
        vehiculoRepository.guardar(vehiculo);
    }
}
