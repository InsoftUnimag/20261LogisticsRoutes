package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.HistorialAsignacionRepositoryPort;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.model.HistorialAsignacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConsultarHistorialConductorUseCase {

    private final ConductorRepositoryPort conductorRepository;
    private final HistorialAsignacionRepositoryPort historialRepository;

    public List<HistorialAsignacion> ejecutar(UUID conductorId) {
        conductorRepository.buscarPorId(conductorId)
                .orElseThrow(() -> new ConductorNoEncontradoException(conductorId));

        return historialRepository.buscarPorConductorId(conductorId);
    }
}
