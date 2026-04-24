package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.IniciarTransitoCommand;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.exception.ConductorNoDisponibleException;
import com.logistics.routes.domain.exception.RutaEstadoInvalidoException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class IniciarTransitoUseCase {

    private final RutaRepositoryPort rutaRepository;
    private final ParadaRepositoryPort paradaRepository;
    private final IntegracionModulo1Port integracionModulo1;

    public Ruta ejecutar(IniciarTransitoCommand command) {
        Ruta ruta = rutaRepository.buscarPorId(command.rutaId())
                .orElseThrow(() -> new RutaNoEncontradaException(command.rutaId()));

        if (ruta.getEstado() != EstadoRuta.CONFIRMADA) {
            throw new RutaEstadoInvalidoException(command.rutaId(), EstadoRuta.CONFIRMADA, ruta.getEstado());
        }

        if (!command.conductorId().equals(ruta.getConductorId())) {
            throw new ConductorNoDisponibleException(command.conductorId().toString());
        }

        ruta.iniciarTransito();
        Ruta guardada = rutaRepository.guardar(ruta);

        Instant ahora = Instant.now();
        paradaRepository.buscarPorRutaId(ruta.getId()).stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .forEach(p -> integracionModulo1.publishPaqueteEnTransito(
                        p.getPaqueteId(), ruta.getId(), ahora));

        return guardada;
    }
}
