package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.RegistrarVehiculoCommand;
import com.logistics.routes.application.port.in.RegistrarVehiculoPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.exception.PlacaDuplicadaException;
import com.logistics.routes.domain.model.Vehiculo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RegistrarVehiculoUseCase implements RegistrarVehiculoPort {

    private final VehiculoRepositoryPort vehiculoRepository;

    @Override
    public Vehiculo ejecutar(RegistrarVehiculoCommand command) {
        if (vehiculoRepository.existePorPlaca(command.placa())) {
            throw new PlacaDuplicadaException(command.placa());
        }
        Vehiculo vehiculo = Vehiculo.nuevo(command);
        return vehiculoRepository.guardar(vehiculo);
    }
}
