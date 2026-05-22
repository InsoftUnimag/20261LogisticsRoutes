# SPEC-09 — Contrato de Integración: Servicio Externo de Conductores

**Fecha:** 2026-05-16  
**Módulo solicitante:** Módulo 2 — Sistema de Gestión de Rutas  
**Servicio requerido:** API REST de Conductores (provisto por el equipo docente)  
**Propósito:** Definir el contrato mínimo que debe exponer el servicio externo de conductores para que el Módulo 2 pueda integrarse correctamente.

---

## Contexto

El Módulo 2 gestiona la asignación de conductores a rutas de entrega. Para ello necesita consultar información de conductores (identidad, contrato) desde un servicio externo. El Módulo 2 actuará como **cliente REST** de este servicio; no gestionará conductores de forma interna.

---

## Endpoints requeridos

### 1. Obtener un conductor por ID

```
GET /conductores/{id}
```

**Parámetro de ruta:**

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `id` | UUID | Identificador único del conductor |

**Respuesta exitosa — `200 OK`:**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "nombre": "Carlos Mendoza",
  "email": "carlos.mendoza@transportes.com",
  "modelo_contrato": "POR_PARADA"
}
```

**Respuesta cuando no existe — `404 Not Found`:**

```json
{
  "mensaje": "Conductor no encontrado"
}
```

---

### 2. Listar todos los conductores

```
GET /conductores
```

**Respuesta exitosa — `200 OK`:**

```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "nombre": "Carlos Mendoza",
    "email": "carlos.mendoza@transportes.com",
    "modelo_contrato": "POR_PARADA"
  },
  {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "nombre": "Ana Torres",
    "email": "ana.torres@transportes.com",
    "modelo_contrato": "RECORRIDO_COMPLETO"
  }
]
```

---

### 3. Actualizar un conductor

```
PUT /conductores/{id}
```

**Parámetro de ruta:**

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `id` | UUID | Identificador único del conductor |

**Body de la solicitud:**

```json
{
  "nombre": "Carlos Mendoza Ruiz",
  "email": "carlos.mendoza@transportes.com",
  "modelo_contrato": "RECORRIDO_COMPLETO"
}
```

**Respuesta exitosa — `200 OK`:**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "nombre": "Carlos Mendoza Ruiz",
  "email": "carlos.mendoza@transportes.com",
  "modelo_contrato": "RECORRIDO_COMPLETO"
}
```

**Respuesta cuando no existe — `404 Not Found`:**

```json
{
  "mensaje": "Conductor no encontrado"
}
```

---

## Descripción de campos

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `id` | UUID | Sí | Identificador único del conductor. El Módulo 2 lo usará como referencia en todos sus registros internos. |
| `nombre` | String | Sí | Nombre completo del conductor. |
| `email` | String | Sí | Correo electrónico. Debe ser único entre conductores. |
| `modelo_contrato` | Enum | Sí | Modelo de pago del conductor. Ver valores permitidos abajo. |

### Valores permitidos para `modelo_contrato`

| Valor | Descripción |
|-------|-------------|
| `RECORRIDO_COMPLETO` | El conductor se paga por ruta completa, independientemente del número de paradas realizadas. |
| `POR_PARADA` | El conductor se paga por cada parada exitosamente gestionada. |

> **Nota:** Este campo es crítico para el Módulo 2. Es usado al cierre de ruta.



## Requerimientos técnicos del servicio

| Aspecto | Requerimiento |
|---------|---------------|
| Formato | JSON (`Content-Type: application/json`) |
| Protocolo | HTTP/HTTPS |
| Autenticación | A definir entre los equipos (Bearer token o sin autenticación para ambiente de pruebas) |
| URL base | A confirmar por el equipo docente |

---

## Ejemplo de uso desde el Módulo 2

Cuando el despachador confirma el despacho de una ruta, el Módulo 2 llamará internamente:

```
GET {url-base}/conductores/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

Y usará el `modelo_contrato` retornado para incluirlo en el evento `RUTA_CERRADA` que se envía al Módulo 3.

---

## Contacto

**Equipo Módulo 2 — Gestión de Rutas**  
Cualquier duda sobre este contrato puede consultarse con el equipo antes de la implementación.
