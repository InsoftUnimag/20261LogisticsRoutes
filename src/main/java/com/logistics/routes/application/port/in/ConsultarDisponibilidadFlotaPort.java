package com.logistics.routes.application.port.in;

import com.logistics.routes.domain.model.Vehiculo;

import java.util.List;

public interface ConsultarDisponibilidadFlotaPort {
    List<Vehiculo> ejecutar();
}
