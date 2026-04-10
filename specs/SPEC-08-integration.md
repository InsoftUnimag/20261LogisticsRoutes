### SPEC - INTEGRATION
## Contrato de Integración: Módulo 1 ↔ Módulo 2 ↔ Módulo 3

**Created:** 2026-03-08 
**Propósito:** Definir los contratos de comunicación entre módulos.

---

## 1. Resumen de Integraciones

| # | Emisor   | Receptor | Tipo      | Disparador                                                      |
|---|----------|----------|-----------|-----------------------------------------------------------------|
| 1 | Módulo 1 | Módulo 2 | Asíncrono | Paquete alcanza estado `Recibido en Sede`                       |
| 2 | Módulo 2 | Módulo 1 | Asincrono | 'ruta_id'                                                       |
| 3 | Módulo 2 | Módulo 1 | Asíncrono | Conductor confirma inicio de tránsito                           |
| 4 | Módulo 2 | Módulo 1 | Asíncrono | Conductor registra entrega exitosa                              |
| 5 | Módulo 2 | Módulo 1 | Asíncrono | Conductor registra parada fallida                               |
| 6 | Módulo 2 | Módulo 1 | Asíncrono | Conductor registra novedad grave                                |
| 7 | Módulo 2 | Módulo 1 | Asíncrono | Ruta cerrada automaticamente o forzada                          |
| 8 | Módulo 2 | Módulo 3 | Asíncrono | Cierre de ruta (resumen para liquidación)                       |
| 9 | Módulo 2 | Módulo 1 | Asíncrono | Despachador excluye una parada de una ruta antes de confirmarla |


---

## 2. Módulo 1 → Módulo 2: Solicitud de Ruta

**Tipo:** Evento asíncrono 
**Disparador:** Un paquete alcanza el estado `Recibido en Sede` en el Módulo 1.

### Payload del evento

```json
{
  "tipo_evento": "SOLICITAR_RUTA",
  "paquete_id": "UUID",
  "peso_kg": "float",
  "volumen_m3": "float",
  "direccion": "string",
  "latitud": "float",
  "longitud": "float",
  "fecha_limite_entrega": "ISO8601",
  "tipo_mercancia": "FRAGIL | PELIGROSO | ESTANDAR"
}
```

### Respuesta:
{
    "ruta_id": "UUID" 
}

### Notas
- Si `fecha_limite_entrega` ya venció al recibir el evento, el Módulo 2 descarta la solicitud y notifica al Despachador Logístico.
---

## 3. Módulo 2 → Módulo 1: Eventos de Estado de Paquete

**Tipo:** Eventos asíncronos (sin respuesta esperada)  
**Descripción:** El Módulo 2 notifica al Módulo 1 cada vez que el estado de un paquete cambia como resultado de la operación en campo o del despacho.

---

### Evento 1 — Paquete en Tránsito

**Disparador:** El conductor confirma el inicio del tránsito desde su dispositivo.  
**Acción esperada en M1:** Cambiar estado del paquete a `En Tránsito`.

```json
{
  "tipo_evento": "PAQUETE_EN_TRANSITO",
  "paquete_id": "UUID",
  "ruta_id": "UUID",
  "fecha_hora_evento": "ISO8601"
}
```

---

### Evento 2 — Paquete Entregado

**Disparador:** El conductor registra entrega exitosa con POD (foto + firma).  
**Acción esperada en M1:** Cambiar estado del paquete a `Entregado`.

```json
{
  "tipo_evento": "PAQUETE_ENTREGADO",
  "paquete_id": "UUID",
  "ruta_id": "UUID",
  "fecha_hora_evento": "ISO8601",
  "evidencia": {
    "url_foto": "string",
    "url_firma": "string"
  }
}
```

---

### Evento 3 — Parada Fallida

**Disparador:** El conductor registra una parada fallida con motivo.  
**Acción esperada en M1:** Actualizar estado del paquete y gestionar según motivo.

```json
{
  "tipo_evento": "PARADA_FALLIDA",
  "paquete_id": "UUID",
  "ruta_id": "UUID",
  "fecha_hora_evento": "ISO8601",
  "motivo": "CLIENTE_AUSENTE | DIRECCION_INCORRECTA | RECHAZADO_CLIENTE | ZONA_DIFICIL_ACCESO"
}
```

---

### Evento 4 — Novedad Grave

**Disparador:** El conductor registra un paquete dañado, extraviado o que requiere devolución.  
**Acción esperada en M1:** Activar protocolo correspondiente (seguro o devolución al origen).

```json
{
  "tipo_evento": "NOVEDAD_GRAVE",
  "paquete_id": "UUID",
  "ruta_id": "UUID",
  "fecha_hora_evento": "ISO8601",
  "tipo_novedad": "DAÑADO | EXTRAVIADO | DEVOLUCION"
}

```

### Evento 5 - Paradas sin gestionar
```json
{
  "tipo_evento": "PARADAS_SIN_GESTIONAR",
  "ruta_id": "UUID",
  "tipo_cierre": "AUTOMATICO | FORZADO",
  "fecha_hora_evento": "ISO8601",
  "paquetes": [
    {
      "paquete_id": "UUID"
    }
  ]
}

```
---

### Evento 6 — Paquete excluido por Despachador

**Disparador**: El Despachador excluye una parada de una ruta en estado "lista_para_despacho" antes de confirmarla.
**Acción esperada en M1**: Actualizar el estado del paquete y gestionar la novedad correspondiente.

```json
{
  "tipo_evento": "PAQUETE_EXCLUIDO_DESPACHO",
  "paquete_id": "UUID",
  "ruta_id": "UUID",
  "fecha_hora_evento": "ISO8601"
}
```

## 4. Módulo 2 → Módulo 3: Cierre de Ruta

**Tipo:** Evento asíncrono (sin respuesta esperada)  
**Disparador:** Cierre de ruta (manual, automático o forzado por despachador).  
**Descripción:** El Sistema de gestión de rutas envía al Módulo 3 el resumen completo de la ruta para que calcule la liquidación del conductor.

```json
{
  "tipo_evento": "RUTA_CERRADA",
  "ruta_id": "UUID",
  "tipo_cierre": "MANUAL | AUTOMATICO | FORZADO_DESPACHADOR",
  "fecha_hora_inicio_transito": "ISO8601",
  "fecha_hora_cierre": "ISO8601",
  "conductor": {
    "conductor_id": "UUID",
    "nombre": "string",
    "modelo_contrato": "Recorrido completo | Por Parada Realizada"  
  },
  "vehiculo": {
    "vehiculo_id": "UUID",
    "placa": "string",
    "tipo": "MOTO | VAN | NHR | TURBO"
  },
  "paradas": [
    {
      "paquete_id": "UUID",
      "estado": "EXITOSA | FALLIDA | NOVEDAD | SIN_GESTION_CONDUCTOR",
      "motivo_no_entrega": "DIRECCIÓN_INCORRECTA | CLIENTE_AUSENTE | RECHAZADO | ZONA DE DIFÍCIL ACESSO / ORDEN PÚBLICO",
      "fecha_hora_gestion": "ISO8601 | null"
    }
  ]
}
```


### Notas
- El conductor no tiene interacción directa con el Módulo 3. El envío de este evento es responsabilidad exclusiva del sistema.

---

## 5. Reglas de Negocio Compartidas

### Estados de Ruta
Todos los módulos deben reconocer los siguientes estados de ruta como válidos:

| Estado | Descripción |
|---|---|
| `CREADA` | La ruta existe y está acumulando Paradas en estado "pendiente". Cada nueva Parada representa un paquete a entregar en esa zona. |
| `LISTA_PARA_DESPACHO` | La ruta alcanzó condición de salida. Espera confirmación del Despachador. |
| `CONFIRMADA` | El Despachador confirmó. Conductor y vehículo físico asignados. |
| `EN_TRANSITO` | El conductor confirmó inicio. Paquetes en camino. |
| `CERRADA_MANUAL` | El conductor cerró la ruta manualmente. |
| `CERRADA_AUTOMATICA` | El sistema cerró la ruta por superar 2 días en tránsito. |
| `CERRADA_FORZADA` | El Despachador forzó el cierre desde su panel. |

### SLA y Distribución de Plazos

| Fase | Responsable | Días |
|---|---|---|
| Gestión interna (admisión → despacho) | Módulo 1 + Módulo 2 | 5 días |
| Tránsito (despacho → entrega) | Módulo 2 (conductor) | 2 días |
| **Total SLA** | | **7 días** |

- `fecha_limite_entrega` = `fecha_creacion_paquete` + 7 días → calculada por **Módulo 1**
- `fecha_limite_despacho` = `fecha_creacion_ruta` + 5 días → calculada por **Módulo 2**
- El cierre automático se activa si la ruta supera **2 días** en estado `EN_TRANSITO`.
