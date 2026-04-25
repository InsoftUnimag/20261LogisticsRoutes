package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.AlmacenamientoArchivoPort;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.domain.exception.ParadaNoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubirFotoParadaUseCase {

    private final ParadaRepositoryPort paradaRepository;
    private final AlmacenamientoArchivoPort almacenamiento;

    public String ejecutar(UUID paradaId, byte[] foto, String contentType) {
        paradaRepository.buscarPorId(paradaId)
                .orElseThrow(() -> new ParadaNoEncontradaException(paradaId));

        return almacenamiento.almacenarFoto(paradaId, foto, contentType);
    }
}
