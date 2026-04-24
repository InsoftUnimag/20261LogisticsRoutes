package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProcesarRutasVencidasUseCase {

    private final RutaRepositoryPort rutaRepository;
    private final NotificacionDespachadorPort notificacion;

    public void ejecutar() {
        List<Ruta> vencidas = rutaRepository.buscarRutasVencidas(Instant.now());
        vencidas.forEach(ruta -> {
            ruta.transicionarAListaParaDespacho();
            rutaRepository.guardar(ruta);
            notificacion.notificarRutaListaParaDespacho(
                    ruta.getId(), ruta.getZona(),
                    ruta.getPesoAcumuladoKg(), ruta.getTipoVehiculoRequerido(),
                    "vencimiento_plazo");
        });
    }
}
