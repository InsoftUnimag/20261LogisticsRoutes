# Guía de contribución

Bienvenido al equipo. Esta guía define las convenciones que seguimos para mantener un historial limpio y un flujo de trabajo ordenado.

---

## Git Flow

### Ramas principales

| Rama | Propósito | Protegida |
|---|---|---|
| `master` | Producción — solo recibe merges desde `release/*` | Sí |
| `develop` | Integración continua — rama base para todo desarrollo | Sí |

### Ramas de trabajo

| Patrón | Cuándo usarla |
|---|---|
| `feature/nombre-corto-descriptivo` | Nueva funcionalidad |
| `fix/descripcion-del-bug` | Corrección de un bug |
| `release/vX.Y.Z` | Preparación de una release |

### Flujo

```
feature/* ──► develop ──► release/* ──► master
fix/*     ──►
```

**Prohibido** hacer push directo a `master` o `develop`. Todo cambio entra por Pull Request.

---

## Nomenclatura de ramas

```
feature/agregar-endpoint-ruta
feature/validacion-zona-geografica
fix/error-despacho-vehiculo
fix/npe-en-cierre-ruta
release/v1.0.0
```

- Usa **kebab-case** (minúsculas, guiones).
- Sé descriptivo pero conciso (máximo 5 palabras).
- No uses caracteres especiales ni espacios.

---

## Conventional Commits

Formato: `tipo(scope): descripción en minúsculas`

### Tipos permitidos

| Tipo | Uso |
|---|---|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de bug |
| `docs` | Cambios en documentación |
| `style` | Formato, espacios, punto y coma (sin lógica) |
| `refactor` | Restructuración sin cambio de comportamiento |
| `test` | Adición o modificación de tests |
| `chore` | Mantenimiento: dependencias, configuración, CI |
| `perf` | Mejora de rendimiento |

### Ejemplos

```
feat(routes): agrega endpoint de creacion de ruta
fix(gradle): corrige version de spring cloud aws
docs(readme): actualiza instrucciones de ejecucion
chore(gitignore): excluye archivos de IDE
refactor(usecase): extrae logica de validacion de zona
test(routes): agrega test unitario para SolicitarRutaUseCase
```

### Reglas

- La descripción va en **minúsculas**, sin punto al final.
- El scope hace referencia al módulo o capa afectada (`routes`, `despacho`, `flota`, `security`, `gradle`, etc.).
- Commits atómicos: un commit = un cambio lógico cohesivo.

---

## Proceso de Pull Request

1. **Crear la rama** desde `develop`:
   ```bash
   git checkout develop && git pull
   git checkout -b feature/nombre-de-la-feature
   ```

2. **Desarrollar** con commits atómicos siguiendo Conventional Commits.

3. **Abrir el PR** contra `develop` — **nunca contra `master`**.

4. **Revisión**: requiere al menos **1 aprobación** de otro miembro del equipo.

5. **Resolver comentarios** antes de mergear. Responde o marca como resuelto cada hilo.

6. **Merge**: usar **squash merge** para mantener el historial de `develop` limpio.
   > El título del squash commit debe seguir el formato de Conventional Commits.

---

## Buenas prácticas

- Mantén los PRs pequeños y enfocados. Un PR grande es difícil de revisar.
- No subas archivos generados (`.gradle/`, `build/`, `.idea/`).
- Si tu rama lleva más de 2 días sin mergear, hazle rebase sobre `develop` para evitar conflictos grandes.
