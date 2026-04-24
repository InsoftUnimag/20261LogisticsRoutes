package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarRutasParaDespachoUseCase {

    private final RutaRepositoryPort rutaRepository;

    public List<Ruta> ejecutar() {
        return rutaRepository.buscarPorEstado(EstadoRuta.LISTA_PARA_DESPACHO);
    }
}
