package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.AlmacenamientoArchivoPort;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.exception.ParadaNoEncontradaException;
import com.logistics.routes.domain.model.Parada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubirFotoParadaUseCaseTest {

    @Mock ParadaRepositoryPort paradaRepository;
    @Mock AlmacenamientoArchivoPort almacenamiento;

    SubirFotoParadaUseCase useCase;

    UUID paradaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new SubirFotoParadaUseCase(paradaRepository, almacenamiento);
    }

    @Test
    void sube_foto_y_retorna_url() {
        Parada parada = Parada.reconstituir(paradaId, UUID.randomUUID(), UUID.randomUUID(), 1,
                "Dir", 11.24, -74.21, null, null, null,
                EstadoParada.PENDIENTE, null, null, null, null, null,
                OrigenParada.SISTEMA);
        byte[] foto = new byte[]{1, 2, 3};
        String urlEsperada = "file:///tmp/logistics/fotos/" + paradaId + "/foto_abc.jpg";

        when(paradaRepository.buscarPorId(paradaId)).thenReturn(Optional.of(parada));
        when(almacenamiento.almacenarFoto(paradaId, foto, "image/jpeg")).thenReturn(urlEsperada);

        String resultado = useCase.ejecutar(paradaId, foto, "image/jpeg");

        assertThat(resultado).isEqualTo(urlEsperada);
    }

    @Test
    void lanza_excepcion_cuando_parada_no_existe() {
        when(paradaRepository.buscarPorId(paradaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(paradaId, new byte[]{1}, "image/jpeg"))
                .isInstanceOf(ParadaNoEncontradaException.class);
    }
}
