package com.logistics.routes.infrastructure.dto.response;

import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Vehiculo;
import com.logistics.routes.domain.valueobject.ZonaGeografica;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlotaDisponibilidadResponseTest {

    @Test
    void no_disponible_para_planificacion_cuando_no_tiene_conductor() {
        Vehiculo v = Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford",
                300.0, 10.0, ZonaGeografica.from(4.0, -74.0));

        FlotaDisponibilidadResponse response = FlotaDisponibilidadResponse.from(v);

        assertThat(response.estado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
        assertThat(response.conductorId()).isNull();
        assertThat(response.disponibleParaPlanificacion()).isFalse();
    }

    @Test
    void disponible_para_planificacion_cuando_disponible_y_tiene_conductor() {
        Vehiculo v = Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford",
                300.0, 10.0, ZonaGeografica.from(4.0, -74.0));
        v.asignarConductor(UUID.randomUUID());

        FlotaDisponibilidadResponse response = FlotaDisponibilidadResponse.from(v);

        assertThat(response.estado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
        assertThat(response.conductorId()).isNotNull();
        assertThat(response.disponibleParaPlanificacion()).isTrue();
    }

    @Test
    void no_disponible_para_planificacion_cuando_en_transito() {
        Vehiculo v = Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford",
                300.0, 10.0, ZonaGeografica.from(4.0, -74.0));
        v.asignarConductor(UUID.randomUUID());
        v.marcarEnTransito();

        FlotaDisponibilidadResponse response = FlotaDisponibilidadResponse.from(v);

        assertThat(response.estado()).isEqualTo(EstadoVehiculo.EN_TRANSITO);
        assertThat(response.disponibleParaPlanificacion()).isFalse();
    }
}
