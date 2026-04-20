package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.in.AsignarVehiculoConductorPort;
import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.HistorialAsignacionRepositoryPort;
import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.exception.VehiculoNoDisponibleException;
import com.logistics.routes.domain.exception.VehiculoNoEncontradoException;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.HistorialAsignacion;
import com.logistics.routes.domain.model.Vehiculo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AsignarVehiculoConductorUseCase implements AsignarVehiculoConductorPort {

    private final ConductorRepositoryPort conductorRepository;
    private final VehiculoRepositoryPort vehiculoRepository;
    private final HistorialAsignacionRepositoryPort historialRepository;
    private final NotificacionDespachadorPort notificacion;

    @Override
    public Conductor ejecutar(UUID conductorId, UUID vehiculoId) {
        Conductor conductor = conductorRepository.buscarPorId(conductorId)
                .orElseThrow(() -> new ConductorNoEncontradoException(conductorId));

        Vehiculo vehiculo = vehiculoRepository.buscarPorId(vehiculoId)
                .orElseThrow(() -> new VehiculoNoEncontradoException(vehiculoId));

        if (vehiculo.getEstado() != EstadoVehiculo.DISPONIBLE) {
            throw new VehiculoNoDisponibleException(vehiculoId.toString());
        }

        // Reglas de dominio: lanza ConductorYaAsignadoException o ConductorNoDisponibleException
        conductor.asignarVehiculo(vehiculoId);
        vehiculo.asignarConductor(conductorId);

        HistorialAsignacion historial = HistorialAsignacion.iniciar(conductorId, vehiculoId);

        conductorRepository.guardar(conductor);
        vehiculoRepository.guardar(vehiculo);
        historialRepository.guardar(historial);

        notificacion.notificarAsignacion(conductorId, vehiculoId);

        return conductor;
    }
}
