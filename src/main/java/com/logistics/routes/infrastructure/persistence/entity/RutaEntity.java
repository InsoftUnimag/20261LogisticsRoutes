package com.logistics.routes.infrastructure.persistence.entity;

import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.TipoCierre;
import com.logistics.routes.domain.enums.TipoVehiculo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rutas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "zona", nullable = false, length = 20)
    private String zona;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "estado_ruta")
    private EstadoRuta estado;

    @Column(name = "peso_acumulado_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoAcumuladoKg;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_vehiculo_requerido", nullable = false, columnDefinition = "tipo_vehiculo")
    private TipoVehiculo tipoVehiculoRequerido;

    @Column(name = "vehiculo_id")
    private UUID vehiculoId;

    @Column(name = "conductor_id")
    private UUID conductorId;

    @Column(name = "fecha_creacion_ruta", nullable = false)
    private Instant fechaCreacionRuta;

    @Column(name = "fecha_limite_despacho", nullable = false)
    private Instant fechaLimiteDespacho;

    @Column(name = "fecha_hora_inicio")
    private Instant fechaHoraInicio;

    @Column(name = "fecha_hora_cierre")
    private Instant fechaHoraCierre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_cierre", columnDefinition = "tipo_cierre")
    private TipoCierre tipoCierre;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
