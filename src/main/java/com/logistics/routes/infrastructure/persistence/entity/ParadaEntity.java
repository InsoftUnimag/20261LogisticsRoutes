package com.logistics.routes.infrastructure.persistence.entity;

import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.MotivoNovedad;
import com.logistics.routes.domain.enums.OrigenParada;
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
@Table(name = "paradas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParadaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "ruta_id", nullable = false)
    private UUID rutaId;

    @Column(name = "paquete_id", nullable = false)
    private UUID paqueteId;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "direccion", nullable = false, length = 500)
    private String direccion;

    @Column(name = "latitud", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitud;

    @Column(name = "longitud", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitud;

    @Column(name = "tipo_mercancia", length = 20)
    private String tipoMercancia;

    @Column(name = "metodo_pago", length = 20)
    private String metodoPago;

    @Column(name = "fecha_limite_entrega")
    private Instant fechaLimiteEntrega;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "estado_parada")
    private EstadoParada estado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "motivo_novedad", columnDefinition = "motivo_novedad")
    private MotivoNovedad motivoNovedad;

    @Column(name = "fecha_hora_gestion")
    private Instant fechaHoraGestion;

    @Column(name = "firma_receptor_url", length = 1000)
    private String firmaReceptorUrl;

    @Column(name = "foto_evidencia_url", length = 1000)
    private String fotoEvidenciaUrl;

    @Column(name = "nombre_receptor", length = 200)
    private String nombreReceptor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "origen", nullable = false, columnDefinition = "origen_parada")
    private OrigenParada origen;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
