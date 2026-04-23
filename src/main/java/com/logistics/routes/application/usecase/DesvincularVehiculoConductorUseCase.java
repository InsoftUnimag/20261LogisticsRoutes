package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.HistorialAsignacionRepositoryPort;
import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.model.Conductor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DesvincularVehiculoConductorUseCase {

    private final ConductorRepositoryPort conductorRepository;
    private final VehiculoRepositoryPort vehiculoRepository;
    private final HistorialAsignacionRepositoryPort historialRepository;
    private final NotificacionDespachadorPort notificacion;

    public Conductor ejecutar(UUID conductorId) {
        Conductor conductor = conductorRepository.buscarPorId(conductorId)
                .orElseThrow(() -> new ConductorNoEncontradoException(conductorId));

        Optional<UUID> vehiculoAnteriorId = conductor.desvincularVehiculo();

        vehiculoAnteriorId.ifPresent(vid ->
                vehiculoRepository.buscarPorId(vid).ifPresent(v -> {
                    v.desvincularConductor();
                    vehiculoRepository.guardar(v);
                })
        );

        historialRepository.buscarActivoPorConductorId(conductorId).ifPresent(h -> {
            h.cerrar();
            historialRepository.guardar(h);
        });

        conductorRepository.guardar(conductor);

        vehiculoAnteriorId.ifPresent(vid -> notificacion.notificarDesvinculacion(conductorId, vid));

        return conductor;
    }
}
