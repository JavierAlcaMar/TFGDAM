# SARA Backend (TFG)

Backend en Java 17 + Spring Boot 3 para automatizar evaluacion por Resultados de Aprendizaje (RA) en FP.

## Stack

- Java 17+
- Spring Boot 3.4.x
- Spring Web
- Spring Data JPA (Hibernate)
- MySQL
- Jakarta Validation
- Lombok

No se usa Flyway/Liquibase en este repo. El esquema se crea con JPA (`spring.jpa.hibernate.ddl-auto=update`).

## Arranque

1. Levanta MySQL y crea credenciales (o usa las de defecto).
2. Configura variables opcionales:

```bash
export SARA_DB_URL='jdbc:mysql://localhost:3306/sara?createDatabaseIfNotExist=true&serverTimezone=UTC'
export SARA_DB_USER='root'
export SARA_DB_PASSWORD=''
```

3. Ejecuta:

```bash
./mvnw spring-boot:run
```

Si la base esta vacia, se insertan datos demo automaticamente con `CommandLineRunner`.

## Endpoints principales

### Configuracion

- `POST /teachers`
- `POST /modules`
- `POST /modules/{id}/ras`
- `POST /modules/{moduleId}/ras/import` (persiste RAs confirmados desde import)
- `PUT /ras/{raId}`
- `PATCH /ras/{raId}`
- `DELETE /ras/{raId}`
- `POST /modules/{id}/uts`
- `PUT /uts/{utId}`
- `PATCH /uts/{utId}`
- `DELETE /uts/{utId}`
- `POST /modules/{id}/ut-ra` (upsert por `utId + raId`)
- `PUT /modules/{id}/ut-ra/{linkId}`
- `PATCH /modules/{id}/ut-ra/{linkId}`
- `DELETE /modules/{id}/ut-ra/{linkId}`
- `POST /uts/{utId}/instruments`
- `PUT /instruments/{id}`
- `PATCH /instruments/{id}`
- `DELETE /instruments/{id}`
- `POST /instruments/{id}/ras` (reemplaza asociaciones)
- `PUT /instruments/{id}/ras` (reemplaza asociaciones)
- `PATCH /instruments/{id}/ras` (agrega/elimina asociaciones)
- `DELETE /instruments/{id}/ras`
- `DELETE /instruments/{id}/ras/{raId}`
- `POST /students`
- `POST /imports/ra` (multipart: `file` + `moduleId`)
- `POST /imports/excel-json` (JSON completo: modulo+RAs+UTs+instrumentos+alumnos+notas, con `evaluationOverrides` opcional)
- `POST /imports/excel-file` (multipart `.xlsx` plantilla oficial -> importacion completa; tambien lee `Evaluaciones`)
- `GET /imports/ra/{jobId}`

### Notas

- `POST /grades` (batch)

### Reportes

- `GET /students/{id}/report?moduleId={moduleId}`
- `GET /modules/{id}/reports/evaluation/{n}`
- `GET /modules/{id}/reports/final`

### Auth y usuarios

- `POST /auth/login` (publico)
- `POST /users` (SUPERADMIN y DIRECTOR)
- `PATCH /users/{id}/activate` (SUPERADMIN y DIRECTOR sobre profesores)
- `PATCH /users/{id}/disable` (SUPERADMIN y DIRECTOR sobre profesores)
- `PATCH /users/{id}/roles` (solo SUPERADMIN)
- `GET /users` (SUPERADMIN y DIRECTOR)
- `GET /users/me` (autenticado)

## Ejemplos curl

Crear modulo (crea tambien docente si no pasas `teacherId`):

```bash
curl -X POST http://localhost:8080/modules \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sistemas","academicYear":"2025-2026","teacherName":"Ana Docente"}'
```

Crear RA:

```bash
curl -X POST http://localhost:8080/modules/1/ras \
  -H 'Content-Type: application/json' \
  -d '{"code":"RA1","name":"Resultado 1","weightPercent":60}'
```

Crear UT:

```bash
curl -X POST http://localhost:8080/modules/1/uts \
  -H 'Content-Type: application/json' \
  -d '{"name":"UT1","evaluationPeriod":1}'
```

Reparto UT-RA:

```bash
curl -X POST http://localhost:8080/modules/1/ut-ra \
  -H 'Content-Type: application/json' \
  -d '{"utId":1,"raId":1,"percent":50}'
```

Crear instrumento:

```bash
curl -X POST http://localhost:8080/uts/1/instruments \
  -H 'Content-Type: application/json' \
  -d '{"name":"Examen UT1","weightPercent":60}'
```

Asociar instrumento a RAs:

```bash
curl -X POST http://localhost:8080/instruments/1/ras \
  -H 'Content-Type: application/json' \
  -d '{"raIds":[1]}'
```

Cargar notas:

```bash
curl -X POST http://localhost:8080/grades \
  -H 'Content-Type: application/json' \
  -d '{"grades":[{"studentId":1,"instrumentId":1,"gradeValue":7.5}]}'
```

Reporte alumno:

```bash
curl 'http://localhost:8080/students/1/report?moduleId=1'
```

Crear import job de RAs desde archivo oficial:

```bash
curl -X POST http://localhost:8080/imports/ra \
  -F "moduleId=1" \
  -F "file=@/ruta/documento_oficial.pdf"
```

Consultar resultado de parseo:

```bash
curl http://localhost:8080/imports/ra/1
```

Persistir RAs confirmados desde el JSON detectado:

```bash
curl -X POST http://localhost:8080/modules/1/ras/import \
  -H 'Content-Type: application/json' \
  -d '{"ras":[{"code":"RA1","name":"Resultado importado 1","weightPercent":50},{"code":"RA2","name":"Resultado importado 2","weightPercent":50}]}'
```

Importar configuracion completa desde JSON (pensado para Python/Excel):

```bash
curl -X POST http://localhost:8080/imports/excel-json \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "module": {
      "name": "Sistemas",
      "academicYear": "2025-2026",
      "teacherName": "Ana Docente"
    },
    "ras": [
      {"code":"RA1","name":"Resultado 1","weightPercent":60},
      {"code":"RA2","name":"Resultado 2","weightPercent":40}
    ],
    "uts": [
      {
        "key":"UT1",
        "name":"UT1 Fundamentos",
        "evaluationPeriod":1,
        "raDistributions":[
          {"raCode":"RA1","percent":70},
          {"raCode":"RA2","percent":30}
        ],
        "instruments":[
          {"key":"I1","name":"Examen UT1","weightPercent":100,"raCodes":["RA1","RA2"]}
        ]
      },
      {
        "key":"UT2",
        "name":"UT2 Servicios",
        "evaluationPeriod":2,
        "raDistributions":[
          {"raCode":"RA1","percent":30},
          {"raCode":"RA2","percent":70}
        ],
        "instruments":[
          {"key":"I2","name":"Proyecto UT2","weightPercent":100,"raCodes":["RA1","RA2"]}
        ]
      }
    ],
    "students":[
      {
        "studentCode":"A001",
        "fullName":"Ana Perez",
        "grades":[
          {"instrumentKey":"I1","gradeValue":7.5},
          {"instrumentKey":"I2","gradeValue":8.0}
        ]
      }
    ],
    "evaluationOverrides":[
      {
        "studentCode":"A001",
        "evaluationPeriod":1,
        "numericGrade":6.5578,
        "suggestedBulletinGrade":7,
        "allRAsPassed":true
      }
    ]
  }'
```

Importar directamente el `.xlsx` de plantilla oficial:

```bash
curl -X POST http://localhost:8080/imports/excel-file \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/ruta/source_template_rellenado.xlsx"
```

Notas sobre importacion desde plantilla:

- Se toman `Datos Iniciales` y `Actividades` para crear modulo, RAs, UTs, instrumentos, alumnos y notas.
- Si existe la hoja `Evaluaciones`, tambien se importan los valores de evaluacion por alumno (`nota numerica`, `boletin sugerido`, `RAs superados`) como overrides por evaluacion.
- En `GET /modules/{id}/reports/evaluation/{n}`, si hay override para alumno+evaluacion se muestra ese valor; si no, se usa el calculo dinamico.

Flujo de autenticacion y autorizacion (SARA):

1) Login admin semilla:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@admin.com","password":"adminmiralmonte"}'
```

2) Crear director (usar token admin en `ADMIN_TOKEN`):

```bash
curl -X POST http://localhost:8080/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"email":"director@colegiomiralmonte.es","password":"director123","roles":["ROLE_DIRECTOR"],"status":"ACTIVE"}'
```

3) Login director:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"director@colegiomiralmonte.es","password":"director123"}'
```

4) Crear profesor (usar token director en `DIRECTOR_TOKEN`):

```bash
curl -X POST http://localhost:8080/users \
  -H "Authorization: Bearer $DIRECTOR_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"email":"profesor1@colegiomiralmonte.es","password":"profesor123"}'
```

5) Desactivar y activar profesor (como director):

```bash
curl -X PATCH http://localhost:8080/users/3/disable \
  -H "Authorization: Bearer $DIRECTOR_TOKEN"

curl -X PATCH http://localhost:8080/users/3/activate \
  -H "Authorization: Bearer $DIRECTOR_TOKEN"
```

Nota: sustituye `3` por el `id` real del profesor devuelto al crearlo.

## Reglas de consistencia implementadas

- No se permiten sumas > 100 en pesos RA, reparto UT-RA o pesos de instrumentos en edicion.
- Para calculo/reportes y carga de notas se exige consistencia completa:
- Suma de pesos RA por modulo = 100.
- Para cada RA, suma UT-RA = 100.
- Para cada actividad (UT), suma de instrumentos = 100.
- Un instrumento solo puede asociarse a RAs permitidos por su UT (reparto > 0).

Los calculos de actividad/RA/final se calculan dinamicamente en `CalculationService`.
Para reportes de evaluacion, el backend usa calculo dinamico salvo que exista un override importado desde `Evaluaciones` (en ese caso prevalece el valor importado).
