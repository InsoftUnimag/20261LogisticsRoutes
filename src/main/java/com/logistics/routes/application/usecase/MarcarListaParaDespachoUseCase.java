package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.exception.RutaEstadoInvalidoException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MarcarListaParaDespachoUseCase {

    private static final String MOTIVO = "dispatcher_manual";

    private final RutaRepositoryPort rutaRepository;
    private final NotificacionDespachadorPort notificacion;

    public Ruta ejecutar(UUID rutaId) {
        Ruta ruta = rutaRepository.buscarPorId(rutaId)
                .orElseThrow(() -> new RutaNoEncontradaException(rutaId));

        if (ruta.getEstado() != EstadoRuta.CREADA) {
            throw new RutaEstadoInvalidoException(rutaId, EstadoRuta.CREADA, ruta.getEstado());
        }

        ruta.transicionarAListaParaDespacho(MOTIVO);
        Ruta guardada = rutaRepository.guardar(ruta);

        notificacion.notificarRutaListaParaDespacho(
                guardada.getId(), guardada.getZona(),
                guardada.getPesoAcumuladoKg(), guardada.getTipoVehiculoRequerido(),
                guardada.getMotivoDespacho());

        return guardada;
    }
}
