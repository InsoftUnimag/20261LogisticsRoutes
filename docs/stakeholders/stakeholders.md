# Stakeholders – Planificación y Gestión de Rutas y Flota

---

## Despachador Logístico
Es el operador encargado de revisar las rutas en estado 'Lista para Despacho' y confirmar su despacho desde su panel. Es el puente entre la planificación automática del sistema y la operación real en campo. Sin su confirmación, ninguna ruta pasa a manos del conductor. También recibe alertas cuando hay bloqueos en la planificación, rutas próximas a vencer su plazo y cierres automáticos por tiempo excedido.
---

## Conductor
Es el operario de campo responsable de ejecutar la ruta asignada. Consulta sus paradas, registra el resultado de cada entrega — exitosa, fallida o novedad grave — y cierra la ruta al finalizar su jornada. Es la fuente primaria de todos los datos operativos que el sistema procesa y distribuye a los demás sistemas.

---

## Administrador de Flota
Es el responsable de mantener el inventario de vehículos actualizado y garantizar que cada vehículo tenga un conductor habilitado asignado. Sus decisiones son prerequisito para que el algoritmo de planificación pueda operar correctamente, ya que sin vehículos registrados y conductores asignados no es posible generar ni despachar rutas.

---

## Sistema de Gestión de Paquetes
Es el sistema externo que actúa como disparador del flujo logístico. Emite el evento SOLICITAR_RUTA cuando un paquete alcanza el estado 'Recibido en Sede' y recibe en tiempo real los eventos de cambio de estado de cada paquete conforme el conductor va registrando sus paradas. Es el sistema que mantiene la trazabilidad del ciclo de vida de cada paquete.
---

## Sistema de Facturación y Liquidación
Es el sistema externo que recibe el evento `cierre_ruta` al finalizar una ruta. Con esa información calcula y emite la liquidación correspondiente al conductor. No interactúa con ningún actor directamente — solo recibe el informe consolidado que el sistema de rutas genera al cierre.

