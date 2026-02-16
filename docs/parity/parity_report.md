# Paridad Excel/PDF vs Backend SARA

## Fuentes analizadas
- Excel oficial: `docs/reference/source_template.xlsx`
- Documento de ayuda: `docs/reference/source_help.pdf`
- Extracción técnica generada:
  - `docs/reference/excel_extract/excel_structure.json`
  - `docs/reference/excel_extract/formula_index.txt`
  - `docs/reference/pdf_extract_lines.txt`

## Paso 1: Extracción de lógica desde Excel

Resumen de estructura:
- Hojas detectadas: 3
  - `Datos Iniciales`
  - `Actividades`
  - `Evaluaciones`

Fórmulas clave detectadas:
- Pesos y sumas:
  - `Datos Iniciales!C19 = SUM(C9:C18)` (suma pesos RA)
  - `Datos Iniciales!F29 = SUM(F9:F28)` (repartos UT/RA)
  - `Datos Iniciales!F65 = SUM(F35:F64)` (otro bloque de reparto)
- Agregación ponderada:
  - `Actividades` usa masivamente `SUMPRODUCT(...)` para ponderación de notas.
  - `Evaluaciones!C5 = SUMPRODUCT(F5:O5,F$50:O$50)` (nota evaluación ponderada por pesos RA).
- Boletín sugerido:
  - `Evaluaciones!E5 = IF(C5<1,1,IF(AND(C5<5,P5>0),TRUNC(C5,0),IF(AND(C5>5,P5>0),4,ROUND(C5,0))))`
  - Análogo en `T5`, `AI5`, `AX5` para otras evaluaciones.
  - `P5 = COUNTIF(F5:O5,"<5")` detecta RAs no superados.

Conclusión técnica:
- La plantilla Excel aplica cálculo ponderado por RA y regla de boletín con truncado/redondeo y penalización si hay RA suspenso.

## Paso 2: Extracción de reglas desde PDF de ayuda

Se realizó extracción textual técnica (la maquetación del PDF separa palabras por objetos de texto). Reglas claramente recuperadas:
- Introducir RAs y sus pesos; la suma debe ser 100.
- Introducir reparto UT/RA según programación didáctica; suma correcta por RA.
- Asociar cada UT a evaluación.
- En `Actividades`, pesos de ejercicios por actividad deben sumar 100.
- Cada ejercicio debe asociarse a un RA válido para esa UT.
- Cálculo automático de notas globales/RA/evaluación en la hoja.

Nota:
- El PDF está muy maquetado por capas; el texto sale fragmentado, pero las reglas de negocio principales se identifican con claridad.

## Paso 3: Validación comparativa API vs lógica oficial

Script de paridad añadido:
- `scripts/parity_excel_api.sh`

Qué valida:
- Casos de boletín sugerido:
  - `numeric > 5` y todos los RA superados -> redondeo.
  - `numeric < 1` -> boletín `1`.
  - `numeric > 5` y algún RA suspenso -> boletín `4`.
  - `numeric < 5` -> truncado.
- Reglas de consistencia:
  - suma pesos RA no puede superar 100.
  - suma reparto UT/RA por RA no puede superar 100.
  - suma pesos instrumentos por actividad no puede superar 100.
  - instrumento sólo puede asociarse a RAs permitidos por UT (percent > 0).
- Reporte final dinámico sin persistir cálculos.

## Estado

- La app ya pasó smoke E2E completo en tu entorno real (`scripts/e2e-db-smoke.sh`).
- Este entorno de agente no puede abrir sockets a `127.0.0.1:8080`, por lo que no puedo ejecutar aquí tu API local directamente.
- Para ejecutar la paridad completa en tu máquina:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
./scripts/parity_excel_api.sh
```

## Observación de borde (importante)

En la fórmula Excel de boletín aparece condición `C5>5` para penalizar con `4` cuando hay RA suspenso.  
Tu backend implementa la regla funcional pedida (`>=5` con RA no superado -> `4`), que es la especificación de negocio acordada.
