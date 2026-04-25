package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.RegistrarParadaCommand;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.exception.ParadaNoEncontradaException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.exception.RutaNoEnTransitoException;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RegistrarParadaUseCase {

    private final ParadaRepositoryPort paradaRepository;
    private final RutaRepositoryPort rutaRepository;
    private final IntegracionModulo1Port integracionModulo1;

    public Parada ejecutar(RegistrarParadaCommand command) {
        Parada parada = paradaRepository.buscarPorId(command.paradaId())
                .orElseThrow(() -> new ParadaNoEncontradaException(command.paradaId()));

        Ruta ruta = rutaRepository.buscarPorId(parada.getRutaId())
                .orElseThrow(() -> new RutaNoEncontradaException(parada.getRutaId()));

        if (ruta.getEstado() != EstadoRuta.EN_TRANSITO) {
            throw new RutaNoEnTransitoException(ruta.getId());
        }

        switch (command) {
            case RegistrarParadaCommand.Exitosa c -> {
                parada.marcarExitosa(c.fotoUrl(), c.firmaUrl(), c.nombreReceptor(), c.fechaAccion());
                paradaRepository.guardar(parada);
                integracionModulo1.publishPaqueteEntregado(
                        parada.getPaqueteId(), ruta.getId(), c.fechaAccion(),
                        c.fotoUrl(), c.firmaUrl());
            }
            case RegistrarParadaCommand.Fallida c -> {
                parada.marcarFallida(c.motivo(), c.fechaAccion());
                paradaRepository.guardar(parada);
                integracionModulo1.publishParadaFallida(
                        parada.getPaqueteId(), ruta.getId(), c.motivo().name(), c.fechaAccion());
            }
            case RegistrarParadaCommand.Novedad c -> {
                parada.marcarNovedad(c.tipoNovedad(), c.fechaAccion());
                paradaRepository.guardar(parada);
                integracionModulo1.publishNovedadGrave(
                        parada.getPaqueteId(), ruta.getId(), c.tipoNovedad().name(), c.fechaAccion());
            }
        }

        return parada;
    }
}
