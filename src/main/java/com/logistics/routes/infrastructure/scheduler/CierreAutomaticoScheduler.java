package com.logistics.routes.infrastructure.scheduler;

import com.logistics.routes.application.usecase.CerrarRutasExcedidasUseCase;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler que cierra automáticamente las rutas EN_TRANSITO que llevan
 * más de 2 días sin finalizar. Ejecuta cada hora con ShedLock para
 * garantizar ejecución única en entornos multi-instancia.
 */
@Component
@RequiredArgsConstructor
public class CierreAutomaticoScheduler {

    private static final Logger log = LoggerFactory.getLogger(CierreAutomaticoScheduler.class);

    private final CerrarRutasExcedidasUseCase cerrarRutasExcedidas;

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "cierre-automatico-scheduler", lockAtMostFor = "50m", lockAtLeastFor = "5m")
    public void ejecutarCierresAutomaticos() {
        log.info("[SCHEDULER] Procesando rutas excedidas en tránsito (> 2 días)");
        cerrarRutasExcedidas.ejecutar();
    }
}
