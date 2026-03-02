# Key Entities — Módulo 2: Planificación y Gestión de Rutas y Flota

---

## Entidades propias del Módulo 2

| Entidad | Descripción |
|---|---|
| **Ruta** | Recorrido asignado a un vehículo con un conjunto de paradas ordenadas geográficamente. Atributos: `id` (UUID), `estado` (planificada / lista_para_despacho / despachada / cerrada), `zona`, `vehiculo_id`, `conductor_id`, `fecha_creacion_ruta`, `fecha_hora_inicio` (momento exacto del despacho), `fecha_hora_cierre`, `tipo_cierre` (manual / automático / forzado_despachador), `fecha_limite_asignacion`, `paradas` (lista). |
| **Vehículo** | Unidad de transporte tipificada con capacidad de peso y volumen. Atributos: `id` (UUID), `placa` (única en el sistema), `tipo` (Moto / Van / NHR / Turbo), `modelo`, `capacidad_peso_kg`, `volumen_maximo_m3`, `zona_operacion`, `estado` (disponible / en_transito / inactivo / en_mantenimiento), `conductor_id` (nullable). |
| **Conductor** | Operario habilitado para operar un tipo de vehículo. Atributos: `id` (UUID), `nombre`, `estado` (activo / inactivo), `turno_activo` (boolean), `vehiculo_asignado_id` (nullable). |
| **Parada** | Punto de entrega dentro de una ruta asociado a un paquete. Atributos: `paquete_id`, `direccion`, `latitud`, `longitud`, `estado_final` (entregado / fallido_cliente / fallido_conductor / dañado / sin_gestión_conductor), `motivo_novedad` (cliente_ausente / dirección_incorrecta / zona_difícil_acceso / rechazado_por_cliente / dañado_en_ruta / cierre_automático), `fecha_hora_gestion`, `firma_receptor` (base64, nullable), `foto_evidencia_url` (nullable), `nombre_receptor` (nullable), `origen` (conductor / sistema). |
| **Evento `ruta_cerrada`** | Payload enviado al Sistema de Facturación y Liquidación al cierre de ruta. Contiene: `fecha_hora_cierre`, datos de la ruta (`id`, `fecha`, `fecha_hora_inicio`, `tipo_cierre`), datos del vehículo (`id`, `placa`, `tipo`), datos del conductor (`id`, `nombre`), y el detalle completo de cada parada con su `estado_final`, `motivo_novedad`, `fecha_hora_gestion` y `origen`. |

---

## Entidad externa referenciada — Paquete (Módulo 1)

| Entidad | Descripción |
|---|---|
| **Paquete** *(externa — Módulo 1)* | El Módulo 2 no posee ni almacena esta entidad. La recibe del Módulo 1 mediante el evento `solicitar_ruta` y la referencia únicamente por su `id` dentro de cada Parada. Campos que el Módulo 2 recibe y utiliza: `id` (referencia en Parada y en todos los eventos de actualización), `peso_kg` (algoritmo de consolidación y validación de capacidad), `volumen_m3` (validación volumétrica del vehículo), `latitud` / `longitud` (agrupación por zona geográfica), `tipo_mercancia` (compatibilidad con tipo de vehículo), `metodo_pago` (información contextual para el conductor en campo), `fecha_limite_entrega` (priorización en el algoritmo y alertas de vencimiento). |

---

## Capacidades por tipo de vehículo

| Tipo | Capacidad de peso | Uso típico |
|---|---|---|
| Moto | hasta 20 kg | Última milla urbana, paquetes pequeños |
| Van | hasta 500 kg | Rutas urbanas, volumen medio |
| NHR | hasta 2.000 kg | Rutas interurbanas o volumen alto |
| Turbo | hasta 4.500 kg | Rutas de alta carga o larga distancia |

---

## Parámetros Configurables

| Parámetro | Descripción | Valor |
|---|---|---|
| `radio_zona_km` | Radio en km para agrupar paquetes en la misma zona geográfica | 5 km |
| `porcentaje_capacidad_maxima` | Umbral de llenado máximo del vehículo antes de crear una nueva ruta | 90% |
| `tiempo_maximo_ruta_horas` | Horas máximas de una ruta activa antes del cierre automático | 12 horas |
| `max_reintentos_parada` | Intentos fallidos permitidos por parada antes de marcarla como fallida definitiva | 2 |
| `max_reintentos_evento` | Intentos de reenvío de evento a sistemas externos antes de emitir alerta manual | 3 |
| `intervalo_reintento_min` | Minutos entre cada reintento de envío de evento | 5 min |
| `plazo_maximo_asignacion_dias` | Días máximos desde 'Listo para Despacho' para despachar la ruta sin comprometer `fecha_limite_entrega` | **A definir con todos los módulos** |
