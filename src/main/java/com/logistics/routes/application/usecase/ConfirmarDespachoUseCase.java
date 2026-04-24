package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.ConfirmarDespachoCommand;
import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.exception.ConductorNoDisponibleException;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.exception.VehiculoNoDisponibleException;
import com.logistics.routes.domain.exception.VehiculoNoEncontradoException;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.model.Vehiculo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ConfirmarDespachoUseCase {

    private static final double RADIO_TIERRA_KM = 6371.0;

    private final RutaRepositoryPort rutaRepository;
    private final VehiculoRepositoryPort vehiculoRepository;
    private final ConductorRepositoryPort conductorRepository;
    private final ParadaRepositoryPort paradaRepository;

    public Ruta ejecutar(UUID rutaId, ConfirmarDespachoCommand command) {
        Ruta ruta = rutaRepository.buscarPorId(rutaId)
                .orElseThrow(() -> new RutaNoEncontradaException(rutaId));

        Conductor conductor = conductorRepository.buscarPorId(command.conductorId())
                .orElseThrow(() -> new ConductorNoEncontradoException(command.conductorId()));
        if (conductor.getEstado() != EstadoConductor.ACTIVO) {
            throw new ConductorNoDisponibleException(conductor.getId().toString());
        }

        Vehiculo vehiculo = vehiculoRepository.buscarPorId(command.vehiculoId())
                .orElseThrow(() -> new VehiculoNoEncontradoException(command.vehiculoId()));
        if (vehiculo.getEstado() != EstadoVehiculo.DISPONIBLE) {
            throw new VehiculoNoDisponibleException(vehiculo.getId().toString());
        }
        if (vehiculo.getTipo() != ruta.getTipoVehiculoRequerido()) {
            throw new VehiculoNoDisponibleException(
                    "El vehículo " + vehiculo.getId() + " es tipo " + vehiculo.getTipo()
                            + " pero la ruta requiere " + ruta.getTipoVehiculoRequerido());
        }

        List<Parada> paradas = paradaRepository.buscarPorRutaId(rutaId);
        List<Parada> paradasOrdenadas = optimizarOrdenParadas(paradas);
        paradaRepository.guardarTodas(paradasOrdenadas);

        ruta.confirmar(conductor.getId(), vehiculo.getId());
        vehiculo.marcarEnTransito();
        conductor.marcarEnRuta();

        rutaRepository.guardar(ruta);
        vehiculoRepository.guardar(vehiculo);
        conductorRepository.guardar(conductor);

        return ruta;
    }

    /**
     * Nearest-neighbor greedy: ancla en la primera parada de la lista,
     * luego toma iterativamente la parada no visitada más cercana.
     * Complejidad O(n²) — suficiente para n < 50 paradas por ruta.
     */
    private List<Parada> optimizarOrdenParadas(List<Parada> paradas) {
        if (paradas.isEmpty()) {
            return paradas;
        }
        List<Parada> pendientes = new ArrayList<>(paradas);
        List<Parada> ordenadas = new ArrayList<>(pendientes.size());

        Parada actual = pendientes.removeFirst();
        actual.asignarOrden(1);
        ordenadas.add(actual);

        while (!pendientes.isEmpty()) {
            final Parada anterior = actual;
            Parada siguiente = pendientes.stream()
                    .min(Comparator.comparingDouble(p -> distanciaKm(anterior, p)))
                    .orElseThrow();
            pendientes.remove(siguiente);
            siguiente.asignarOrden(ordenadas.size() + 1);
            ordenadas.add(siguiente);
            actual = siguiente;
        }
        return ordenadas;
    }

    private double distanciaKm(Parada a, Parada b) {
        double dLat = Math.toRadians(b.getLatitud() - a.getLatitud());
        double dLon = Math.toRadians(b.getLongitud() - a.getLongitud());
        double lat1 = Math.toRadians(a.getLatitud());
        double lat2 = Math.toRadians(b.getLatitud());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(h));
    }
}
