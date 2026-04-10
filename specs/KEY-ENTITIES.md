# Key Entities — Módulo 2: Planificación y Gestión de Rutas y Flota

---

## Entidades propias del Módulo 2

| Entidad | Descripción                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Ruta** | Recorrido asignado a un vehículo con un conjunto de paradas ordenadas geográficamente. Atributos: `id` (UUID), `estado` ("creada" / "lista_para_despacho" / "confirmada" / "en_transito" / "cerrada"), `zona`, `vehiculo_id`, `conductor_id`, `fecha_creacion_ruta`, `fecha_hora_inicio` (momento exacto en que el conductor confirma inicio), `fecha_hora_cierre`, `tipo_cierre` ("manual" / "automatica" / "forzado_despachador"), `fecha_limite_despacho`, `paradas` (lista).                                                                                               |
| **Vehículo** | Unidad de transporte tipificada con capacidad de peso y volumen. Atributos: `id` (UUID), `placa` (única en el sistema), `tipo` ("moto" / "van" / "nhr" / "turbo"), `modelo`, `capacidad_peso_kg`, `volumen_maximo_m3`, `zona_operacion`, `estado` ("disponible" / "en_transito" / "inactivo"), `conductor_id` (nullable).                                                                                                                                                                                                                                                      |
| **Conductor** | Operario habilitado para operar un tipo de vehículo. Atributos: `id` (UUID), `nombre`, `estado` ("activo" / "inactivo" / "en_ruta), `vehiculo_asignado_id` (UUID, nullable).                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Parada** | Punto de entrega dentro de una ruta asociado a un paquete. Atributos: `paquete_id`, `direccion`, `latitud`, `longitud`, `estado` ("pendiente" / "exitosa" / "fallida" / "novedad" / "sin_gestion_conductor" / "excluida_despacho"), `motivo_novedad` ("cliente_ausente" / "direccion_incorrecta" / "zona_dificil_acceso" / "rechazado_por_cliente" / "dañado_en_ruta" / "extraviado" / "devolucion", nullable), `fecha_hora_gestion`, `firma_receptor` (URL, nullable), `foto_evidencia_url` (URL, nullable), `nombre_receptor` (nullable), `origen` ("conductor" / "sistema"). |
| **Evento de cierre** | Payload enviado al Sistema de Facturación y Liquidación al cierre de ruta. Contiene: `fecha_hora_cierre`, datos de la ruta (`id`, `fecha_hora_inicio`, `tipo_cierre`), datos del vehículo (`id`, `placa`, `tipo`), datos del conductor (`id`, `nombre`, `tipo_contrato`), y el detalle completo de cada parada con su estado, motivo de novedad, fecha/hora de gestión.                                                                                                                                                                                                        |

---

## Entidad externa referenciada — Paquete (Sistema de Gestión de Paquetes)

| Entidad | Descripción |
|---|---|
| **Paquete** *(externa — Sistema de Gestión de Paquetes)* | El sistema de rutas no posee ni almacena esta entidad. La recibe del Sistema de Gestión de Paquetes mediante el evento de "solicitar_ruta" y la referencia únicamente por su `id` dentro de cada Parada. Campos que el sistema de rutas recibe y utiliza: `paquete_id` (referencia en Parada y en todos los eventos de actualización), `peso_kg` (algoritmo de consolidación y validación de capacidad), `volumen_m3` (validación volumétrica del vehículo), `latitud` / `longitud` (agrupación por zona geográfica), `tipo_mercancia` (información contextual para el conductor en campo), `metodo_pago` (Informacion para el conductor en campo), `fecha_limite_entrega` (priorización en el algoritmo y alertas de vencimiento). |

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
| `porcentaje_capacidad_maxima` | Umbral de llenado máximo del vehículo antes de cambiar la ruta a "lista_para_despacho" | 90% |
| `tiempo_maximo_transito_dias` | Días máximos en estado "en_transito" antes del cierre automático | **2 días** |
| `plazo_maximo_despacho_dias` | Días máximos desde la creación de la ruta para despacharla (`fecha_limite_despacho`) | **5 días** |
