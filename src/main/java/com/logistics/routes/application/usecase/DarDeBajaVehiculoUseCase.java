package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.in.DarDeBajaVehiculoPort;
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
public class DarDeBajaVehiculoUseCase implements DarDeBajaVehiculoPort {

    private final VehiculoRepositoryPort vehiculoRepository;

    @Override
    public void ejecutar(UUID id) {
        Vehiculo vehiculo = vehiculoRepository.buscarPorId(id)
                .orElseThrow(() -> new VehiculoNoEncontradoException(id));

        vehiculo.marcarInactivo(); // lanza VehiculoEnTransitoException si EN_TRANSITO
        vehiculoRepository.guardar(vehiculo);
    }
}
