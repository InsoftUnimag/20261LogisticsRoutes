package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.IntegracionModulo3Port;
import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.TipoCierre;
import com.logistics.routes.domain.model.Ruta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class CerrarRutasExcedidasUseCase extends RutaCierreBaseUseCase {

    private final NotificacionDespachadorPort notificacion;

    public CerrarRutasExcedidasUseCase(
            RutaRepositoryPort rutaRepository,
            ParadaRepositoryPort paradaRepository,
            ConductorRepositoryPort conductorRepository,
            VehiculoRepositoryPort vehiculoRepository,
            IntegracionModulo1Port integracionModulo1,
            IntegracionModulo3Port integracionModulo3,
            NotificacionDespachadorPort notificacion) {
        super(rutaRepository, paradaRepository, conductorRepository,
                vehiculoRepository, integracionModulo1, integracionModulo3);
        this.notificacion = notificacion;
    }

    public void ejecutar() {
        Instant limite = Instant.now().minus(2, ChronoUnit.DAYS);
        List<Ruta> excedidas = rutaRepository.buscarRutasEnTransitoExcedidas(limite);
        for (Ruta ruta : excedidas) {
            cerrar(ruta.getId(), true, TipoCierre.AUTOMATICO);
            notificacion.notificarAlertaPrioritaria(
                    "Ruta " + ruta.getId() + " cerrada automáticamente por exceder 2 días en tránsito");
        }
    }
}
