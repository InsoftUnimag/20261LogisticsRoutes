package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ConfirmarRutaUseCase {

    private final RutaRepositoryPort rutaRepository;

    public void ejecutar(UUID rutaId, UUID conductorId, UUID vehiculoId) {
        Ruta ruta = rutaRepository.buscarPorId(rutaId)
                .orElseThrow(() -> new RutaNoEncontradaException(rutaId));
        ruta.confirmar(conductorId, vehiculoId);
        rutaRepository.guardar(ruta);
    }
}
