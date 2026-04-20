package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.in.DarDeBajaConductorPort;
import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.exception.ConductorYaAsignadoException;
import com.logistics.routes.domain.model.Conductor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DarDeBajaConductorUseCase implements DarDeBajaConductorPort {

    private final ConductorRepositoryPort conductorRepository;

    @Override
    public void ejecutar(UUID conductorId) {
        Conductor conductor = conductorRepository.buscarPorId(conductorId)
                .orElseThrow(() -> new ConductorNoEncontradoException(conductorId));

        if (conductor.getVehiculoAsignadoId() != null) {
            throw new ConductorYaAsignadoException(conductorId.toString());
        }

        conductor.marcarInactivo();
        conductorRepository.guardar(conductor);
    }
}
