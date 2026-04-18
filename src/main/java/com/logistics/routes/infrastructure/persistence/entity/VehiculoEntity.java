package com.logistics.routes.infrastructure.persistence.entity;

import com.logistics.routes.domain.enums.EstadoVehiculo;
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
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculoEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "placa", nullable = false, unique = true, length = 10)
    private String placa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_vehiculo")
    private TipoVehiculo tipo;

    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;

    @Column(name = "capacidad_peso_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacidadPesoKg;

    @Column(name = "volumen_maximo_m3", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumenMaximoM3;

    @Column(name = "zona_operacion", nullable = false, length = 20)
    private String zonaOperacion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "estado_vehiculo")
    private EstadoVehiculo estado;

    @Column(name = "conductor_id")
    private UUID conductorId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
