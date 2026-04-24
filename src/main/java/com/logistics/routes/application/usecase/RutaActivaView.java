package com.logistics.routes.application.usecase;

import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;

import java.util.List;

public record RutaActivaView(Ruta ruta, List<Parada> paradas) {}
