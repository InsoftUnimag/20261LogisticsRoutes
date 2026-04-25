package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.IntegracionModulo3Port;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.TipoCierre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CerrarRutaManualUseCase extends RutaCierreBaseUseCase {

    public CerrarRutaManualUseCase(
            RutaRepositoryPort rutaRepository,
            ParadaRepositoryPort paradaRepository,
            ConductorRepositoryPort conductorRepository,
            VehiculoRepositoryPort vehiculoRepository,
            IntegracionModulo1Port integracionModulo1,
            IntegracionModulo3Port integracionModulo3) {
        super(rutaRepository, paradaRepository, conductorRepository,
                vehiculoRepository, integracionModulo1, integracionModulo3);
    }

    public void ejecutar(UUID rutaId, boolean confirmarConPendientes) {
        cerrar(rutaId, confirmarConPendientes, TipoCierre.MANUAL);
    }
}
