package com.logistics.routes.infrastructure.dto.request;

import com.logistics.routes.application.command.RegistrarConductorCommand;
import com.logistics.routes.domain.enums.ModeloContrato;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConductorRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        @Size(max = 255)
        String email,

        @NotNull(message = "El modelo de contrato es obligatorio")
        ModeloContrato modeloContrato

) {
    public RegistrarConductorCommand toCommand() {
        return new RegistrarConductorCommand(nombre, email, modeloContrato);
    }
}
