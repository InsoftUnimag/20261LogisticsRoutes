package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.model.Parada;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConsultarRutaActivaUseCase {

    private final RutaRepositoryPort rutaRepository;
    private final ParadaRepositoryPort paradaRepository;

    public Optional<RutaActivaView> ejecutar(UUID conductorId) {
        return rutaRepository.buscarRutaActivaPorConductorId(conductorId)
                .map(ruta -> {
                    List<Parada> paradas = paradaRepository.buscarPorRutaId(ruta.getId())
                            .stream()
                            .sorted(Comparator.comparingInt(Parada::getOrden))
                            .toList();
                    return new RutaActivaView(ruta, paradas);
                });
    }
}
