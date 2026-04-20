package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.model.Vehiculo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConsultarDisponibilidadFlotaUseCase {

    private final VehiculoRepositoryPort vehiculoRepository;

    public List<Vehiculo> ejecutar() {
        return vehiculoRepository.buscarTodos();
    }
}
