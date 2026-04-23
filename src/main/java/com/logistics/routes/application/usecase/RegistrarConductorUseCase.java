package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.RegistrarConductorCommand;
import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.domain.exception.EmailDuplicadoException;
import com.logistics.routes.domain.model.Conductor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RegistrarConductorUseCase {

    private final ConductorRepositoryPort conductorRepository;

    public Conductor ejecutar(RegistrarConductorCommand command) {
        if (conductorRepository.existePorEmail(command.email())) {
            throw new EmailDuplicadoException(command.email());
        }
        Conductor conductor = Conductor.nuevo(command.nombre(), command.email(), command.modeloContrato());
        return conductorRepository.guardar(conductor);
    }
}
