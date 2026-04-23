package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.exception.VehiculoNoEncontradoException;
import com.logistics.routes.domain.model.Vehiculo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DarDeBajaVehiculoUseCase {

    private final VehiculoRepositoryPort vehiculoRepository;

    public void ejecutar(UUID id) {
        Vehiculo vehiculo = vehiculoRepository.buscarPorId(id)
                .orElseThrow(() -> new VehiculoNoEncontradoException(id));

        vehiculo.marcarInactivo();
        vehiculoRepository.guardar(vehiculo);
    }
}
