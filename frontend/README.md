# SARA Frontend (React + Vite)

Interfaz web para trabajar con el backend `TFGDAM-main` (Spring Boot).

## Requisitos

- Node.js 18+
- npm 9+
- Backend levantado en `http://localhost:8080`

## Scripts

```bash
npm install
npm run dev
npm run build
npm run preview
```

Por defecto, `npm run dev` publica en `http://localhost:5173`.

## Configuracion API

La app usa `VITE_API_BASE_URL` y por defecto apunta a `/api`.

- En desarrollo, `vite.config.js` proxifica `/api/*` hacia `http://localhost:8080/*`.
- Si despliegas sin proxy, define `VITE_API_BASE_URL` (por ejemplo `http://localhost:8080`) y habilita CORS en backend si aplica.

Ejemplo `.env`:

```bash
VITE_API_BASE_URL=/api
```

## Flujo actual

1. Login/logout con JWT (`/auth/login`).
2. Gestion de modulos:
   - crear modulo (`POST /modules`)
   - seleccionar modulo activo por ID
   - eliminar modulo (`DELETE /modules/{id}`)
3. Importacion:
   - PDF de RAs (`POST /imports/ra`) + consulta de job (`GET /imports/ra/{jobId}`) + guardado de RAs (`POST /modules/{id}/ras/import`)
   - XLSX de plantilla oficial (`POST /imports/excel-file`), con reemplazo de modulo si `moduleId` existe
4. Vista previa por modulo:
   - estructura base (`GET /modules/{id}/preview`)
   - evaluaciones (`GET /modules/{id}/reports/evaluation/{n}`)
   - final (`GET /modules/{id}/reports/final`)
   - edicion de ejercicios por alumno/instrumento + guardado (`POST /grades`)
   - descarga Excel del modulo (`GET /modules/{id}/export/excel`)
   - descarga plantilla base (`GET /modules/export/template/base`)
   - descarga plantilla rellenada (`GET /modules/export/template/filled`)

## Endpoints consumidos

- `POST /auth/login`
- `POST /modules`
- `DELETE /modules/{id}`
- `POST /imports/ra`
- `GET /imports/ra/{jobId}`
- `POST /modules/{moduleId}/ras/import`
- `POST /imports/excel-file`
- `GET /modules/{id}/preview`
- `GET /modules/{id}/reports/evaluation/{n}`
- `GET /modules/{id}/reports/final`
- `POST /grades`
- `GET /modules/{id}/export/excel`
- `GET /modules/export/template/base`
- `GET /modules/export/template/filled`

## Estructura relevante

- `src/App.jsx`: estado global y flujo principal (navegacion por hash).
- `src/lib/api.js`: wrapper de `fetch`, token y manejo de errores.
- `src/views/LoginView.jsx`: login.
- `src/views/ModulesView.jsx`: alta/borrado de modulo.
- `src/views/ImportView.jsx`: importacion PDF/XLSX y persistencia de RAs detectados.
- `src/views/PreviewView.jsx`: tablas de vista previa y reportes.

## Notas

- En importacion PDF, el `moduleId` es obligatorio.
- En importacion XLSX, `moduleId` es opcional: si existe se reemplaza ese modulo; si no, se crea uno nuevo.
- Tras importar XLSX, el frontend selecciona automaticamente el `moduleId` devuelto por backend.
- La UI ordena codigos de forma natural (`RA2` antes que `RA10`) en las tablas de preview.

