package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.domain.model.Conductor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarConductoresUseCase {

    private final ConductorRepositoryPort conductorRepository;

    public List<Conductor> ejecutar() {
        return conductorRepository.buscarTodos();
    }
}
