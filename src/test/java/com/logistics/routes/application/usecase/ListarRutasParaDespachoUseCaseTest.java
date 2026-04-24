package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.model.Ruta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarRutasParaDespachoUseCaseTest {

    @Mock RutaRepositoryPort rutaRepository;

    ListarRutasParaDespachoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListarRutasParaDespachoUseCase(rutaRepository);
    }

    @Test
    void delega_en_el_puerto_filtrando_por_estado_lista_para_despacho() {
        // Given: dos rutas en LISTA_PARA_DESPACHO
        Ruta ruta1 = Ruta.nueva("d29ej", Instant.now().plus(5, ChronoUnit.DAYS));
        ruta1.transicionarAListaParaDespacho();
        Ruta ruta2 = Ruta.nueva("d29ek", Instant.now().plus(5, ChronoUnit.DAYS));
        ruta2.transicionarAListaParaDespacho();

        when(rutaRepository.buscarPorEstado(EstadoRuta.LISTA_PARA_DESPACHO))
                .thenReturn(List.of(ruta1, ruta2));

        // When
        List<Ruta> resultado = useCase.ejecutar();

        // Then
        assertThat(resultado).containsExactly(ruta1, ruta2);
        verify(rutaRepository).buscarPorEstado(EstadoRuta.LISTA_PARA_DESPACHO);
    }

    @Test
    void retorna_lista_vacia_cuando_no_hay_rutas_listas_para_despacho() {
        when(rutaRepository.buscarPorEstado(EstadoRuta.LISTA_PARA_DESPACHO))
                .thenReturn(List.of());

        assertThat(useCase.ejecutar()).isEmpty();
    }
}
