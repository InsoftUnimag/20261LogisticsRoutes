package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.domain.exception.ParadaNoEncontradaException;
import com.logistics.routes.domain.model.Parada;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ExcluirPaqueteRutaUseCase {

    private final ParadaRepositoryPort paradaRepository;
    private final IntegracionModulo1Port integracionM1;

    public void ejecutar(UUID rutaId, UUID paqueteId, String motivo) {
        Parada parada = paradaRepository.buscarPorRutaYPaquete(rutaId, paqueteId)
                .orElseThrow(() -> new ParadaNoEncontradaException(paqueteId));
        parada.marcarExcluidaDespacho();
        paradaRepository.guardar(parada);
        integracionM1.publishPaqueteExcluidoDespacho(paqueteId, rutaId, motivo, Instant.now());
    }
}
