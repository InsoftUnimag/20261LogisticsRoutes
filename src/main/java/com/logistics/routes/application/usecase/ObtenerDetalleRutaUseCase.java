package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObtenerDetalleRutaUseCase {

    private final RutaRepositoryPort rutaRepository;
    private final ParadaRepositoryPort paradaRepository;

    public record Detalle(Ruta ruta, List<Parada> paradas) {}

    public Detalle ejecutar(UUID rutaId) {
        Ruta ruta = rutaRepository.buscarPorId(rutaId)
                .orElseThrow(() -> new RutaNoEncontradaException(rutaId));
        List<Parada> paradas = paradaRepository.buscarPorRutaId(rutaId);
        return new Detalle(ruta, paradas);
    }
}
