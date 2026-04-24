package com.logistics.routes.infrastructure.scheduler;

import com.logistics.routes.application.usecase.ProcesarRutasVencidasUseCase;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler que transiciona a LISTA_PARA_DESPACHO las rutas CREADA cuya
 * fechaLimiteDespacho haya vencido. Ejecuta cada 5 minutos con ShedLock
 * para garantizar ejecución única en entornos multi-instancia.
 */
@Component
@RequiredArgsConstructor
public class FechaLimiteDespachoScheduler {

    private static final Logger log = LoggerFactory.getLogger(FechaLimiteDespachoScheduler.class);

    private final ProcesarRutasVencidasUseCase procesarRutasVencidas;

    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "fecha-limite-despacho", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public void transicionarRutasVencidas() {
        log.info("[SCHEDULER] Procesando rutas con fecha límite vencida");
        procesarRutasVencidas.ejecutar();
    }
}
