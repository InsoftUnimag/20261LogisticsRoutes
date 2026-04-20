package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.ActualizarVehiculoCommand;
import com.logistics.routes.application.port.in.ActualizarVehiculoPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.exception.VehiculoEnTransitoException;
import com.logistics.routes.domain.exception.VehiculoNoEncontradoException;
import com.logistics.routes.domain.model.Vehiculo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ActualizarVehiculoUseCase implements ActualizarVehiculoPort {

    private final VehiculoRepositoryPort vehiculoRepository;

    @Override
    public Vehiculo ejecutar(UUID id, ActualizarVehiculoCommand command) {
        Vehiculo vehiculo = vehiculoRepository.buscarPorId(id)
                .orElseThrow(() -> new VehiculoNoEncontradoException(id));

        vehiculo.actualizar(command); // lanza VehiculoEnTransitoException si EN_TRANSITO
        return vehiculoRepository.guardar(vehiculo);
    }
}
