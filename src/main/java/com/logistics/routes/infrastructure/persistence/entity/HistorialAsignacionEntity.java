package com.logistics.routes.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "historial_asignaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialAsignacionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "conductor_id", nullable = false)
    private UUID conductorId;

    @Column(name = "vehiculo_id", nullable = false)
    private UUID vehiculoId;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private Instant fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private Instant fechaHoraFin;
}
