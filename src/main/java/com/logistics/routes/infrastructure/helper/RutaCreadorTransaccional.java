package com.logistics.routes.infrastructure.helper;

import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper transaccional que intenta persistir una ruta nueva en una transacción independiente (REQUIRES_NEW).
 * Permite que SolicitarRutaUseCase capture DataIntegrityViolationException sin contaminar
 * su propia transacción exterior cuando el índice único idx_rutas_zona_creada rechaza duplicados.
 */
@Component
@RequiredArgsConstructor
public class RutaCreadorTransaccional {

    private final RutaRepositoryPort rutaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Ruta guardarNueva(Ruta ruta) {
        return rutaRepository.guardar(ruta);
    }
}
