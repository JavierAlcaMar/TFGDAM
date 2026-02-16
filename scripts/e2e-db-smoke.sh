#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@admin.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-adminmiralmonte}"

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required" >&2
  exit 1
fi

RESPONSE_BODY=""
RESPONSE_CODE=""

json_request() {
  local method="$1"
  local url="$2"
  local payload="${3:-}"
  local token="${4:-}"
  local tmp
  tmp="$(mktemp)"

  if [[ -n "$token" ]]; then
    if [[ -n "$payload" ]]; then
      RESPONSE_CODE="$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$url" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "$payload")"
    else
      RESPONSE_CODE="$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$url" \
        -H "Authorization: Bearer $token")"
    fi
  else
    if [[ -n "$payload" ]]; then
      RESPONSE_CODE="$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -d "$payload")"
    else
      RESPONSE_CODE="$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$url")"
    fi
  fi

  RESPONSE_BODY="$(cat "$tmp")"
  rm -f "$tmp"
}

multipart_request() {
  local url="$1"
  local module_id="$2"
  local file_path="$3"
  local token="$4"
  local tmp
  tmp="$(mktemp)"

  RESPONSE_CODE="$(curl -sS -o "$tmp" -w "%{http_code}" -X POST "$url" \
    -H "Authorization: Bearer $token" \
    -F "moduleId=$module_id" \
    -F "file=@$file_path")"

  RESPONSE_BODY="$(cat "$tmp")"
  rm -f "$tmp"
}

assert_code() {
  local expected="$1"
  local context="$2"
  if [[ "$RESPONSE_CODE" != "$expected" ]]; then
    echo "ERROR [$context]: expected HTTP $expected, got $RESPONSE_CODE" >&2
    echo "$RESPONSE_BODY" >&2
    exit 1
  fi
}

assert_jq() {
  local expr="$1"
  local context="$2"
  if ! echo "$RESPONSE_BODY" | jq -e "$expr" >/dev/null; then
    echo "ERROR [$context]: jq assertion failed: $expr" >&2
    echo "$RESPONSE_BODY" >&2
    exit 1
  fi
}

TS="$(date +%s)"
DIRECTOR_EMAIL="director.${TS}@colegiomiralmonte.es"
DIRECTOR_PASSWORD="Director123!"
TEACHER_EMAIL="teacher.${TS}@colegiomiralmonte.es"
TEACHER_PASSWORD="Teacher123!"
MODULE_NAME="SMOKE-MODULE-${TS}"

echo "[1/10] Login SUPERADMIN"
json_request POST "$BASE_URL/auth/login" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"
assert_code 200 "admin login"
assert_jq '.accessToken != null and .accessToken != ""' "admin login token"
ADMIN_TOKEN="$(echo "$RESPONSE_BODY" | jq -r '.accessToken')"

echo "[2/10] Crear DIRECTOR, login DIRECTOR, crear+activar/desactivar TEACHER"
json_request POST "$BASE_URL/users" "{\"email\":\"$DIRECTOR_EMAIL\",\"password\":\"$DIRECTOR_PASSWORD\",\"roles\":[\"ROLE_DIRECTOR\"],\"status\":\"ACTIVE\"}" "$ADMIN_TOKEN"
assert_code 201 "create director"
DIRECTOR_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/auth/login" "{\"email\":\"$DIRECTOR_EMAIL\",\"password\":\"$DIRECTOR_PASSWORD\"}"
assert_code 200 "director login"
DIRECTOR_TOKEN="$(echo "$RESPONSE_BODY" | jq -r '.accessToken')"

json_request POST "$BASE_URL/users" "{\"email\":\"$TEACHER_EMAIL\",\"password\":\"$TEACHER_PASSWORD\"}" "$DIRECTOR_TOKEN"
assert_code 201 "create teacher"
TEACHER_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request PATCH "$BASE_URL/users/$TEACHER_ID/disable" "" "$DIRECTOR_TOKEN"
assert_code 200 "disable teacher"
assert_jq '.status == "DISABLED"' "teacher disabled"

json_request PATCH "$BASE_URL/users/$TEACHER_ID/activate" "" "$DIRECTOR_TOKEN"
assert_code 200 "activate teacher"
assert_jq '.status == "ACTIVE"' "teacher active"

echo "[3/10] Crear modulo, RAs, UTs y reparto UT-RA"
json_request POST "$BASE_URL/modules" "{\"name\":\"$MODULE_NAME\",\"academicYear\":\"2025-2026\",\"teacherName\":\"Smoke Teacher\"}" "$ADMIN_TOKEN"
assert_code 201 "create module"
MODULE_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MODULE_ID/ras" "{\"code\":\"RA1-${TS}\",\"name\":\"Resultado 1\",\"weightPercent\":50}" "$ADMIN_TOKEN"
assert_code 201 "create ra1"
RA1_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MODULE_ID/ras" "{\"code\":\"RA2-${TS}\",\"name\":\"Resultado 2\",\"weightPercent\":50}" "$ADMIN_TOKEN"
assert_code 201 "create ra2"
RA2_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MODULE_ID/uts" "{\"name\":\"UT1-${TS}\",\"evaluationPeriod\":1}" "$ADMIN_TOKEN"
assert_code 201 "create ut1"
UT1_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MODULE_ID/uts" "{\"name\":\"UT2-${TS}\",\"evaluationPeriod\":2}" "$ADMIN_TOKEN"
assert_code 201 "create ut2"
UT2_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MODULE_ID/ut-ra" "{\"utId\":$UT1_ID,\"raId\":$RA1_ID,\"percent\":60}" "$ADMIN_TOKEN"
assert_code 201 "link ut1-ra1"
LINK_UT1_RA1="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MODULE_ID/ut-ra" "{\"utId\":$UT2_ID,\"raId\":$RA1_ID,\"percent\":40}" "$ADMIN_TOKEN"
assert_code 201 "link ut2-ra1"

json_request POST "$BASE_URL/modules/$MODULE_ID/ut-ra" "{\"utId\":$UT1_ID,\"raId\":$RA2_ID,\"percent\":20}" "$ADMIN_TOKEN"
assert_code 201 "link ut1-ra2"

json_request POST "$BASE_URL/modules/$MODULE_ID/ut-ra" "{\"utId\":$UT2_ID,\"raId\":$RA2_ID,\"percent\":80}" "$ADMIN_TOKEN"
assert_code 201 "link ut2-ra2"

echo "[4/10] Crear instrumentos y asociar RAs"
json_request POST "$BASE_URL/uts/$UT1_ID/instruments" "{\"name\":\"I1-${TS}\",\"weightPercent\":50}" "$ADMIN_TOKEN"
assert_code 201 "create instrument 1"
I1_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/uts/$UT1_ID/instruments" "{\"name\":\"I2-${TS}\",\"weightPercent\":50}" "$ADMIN_TOKEN"
assert_code 201 "create instrument 2"
I2_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/uts/$UT2_ID/instruments" "{\"name\":\"I3-${TS}\",\"weightPercent\":100}" "$ADMIN_TOKEN"
assert_code 201 "create instrument 3"
I3_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/instruments/$I1_ID/ras" "{\"raIds\":[$RA1_ID]}" "$ADMIN_TOKEN"
assert_code 200 "instrument 1 ras"

json_request POST "$BASE_URL/instruments/$I2_ID/ras" "{\"raIds\":[$RA1_ID,$RA2_ID]}" "$ADMIN_TOKEN"
assert_code 200 "instrument 2 ras"

json_request POST "$BASE_URL/instruments/$I3_ID/ras" "{\"raIds\":[$RA2_ID]}" "$ADMIN_TOKEN"
assert_code 200 "instrument 3 ras"

echo "[5/10] Crear alumno y cargar notas base"
json_request POST "$BASE_URL/students" "{\"moduleId\":$MODULE_ID,\"studentCode\":\"S-${TS}\",\"fullName\":\"Alumno Smoke ${TS}\"}" "$ADMIN_TOKEN"
assert_code 201 "create student"
STUDENT_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/grades" "{\"grades\":[{\"studentId\":$STUDENT_ID,\"instrumentId\":$I1_ID,\"gradeValue\":10},{\"studentId\":$STUDENT_ID,\"instrumentId\":$I2_ID,\"gradeValue\":2},{\"studentId\":$STUDENT_ID,\"instrumentId\":$I3_ID,\"gradeValue\":8}]}" "$ADMIN_TOKEN"
assert_code 201 "upsert grades"
assert_jq 'length == 3' "grades response size"

echo "[6/10] Reportes (alumno, evaluaciones, final)"
json_request GET "$BASE_URL/students/$STUDENT_ID/report?moduleId=$MODULE_ID" "" "$ADMIN_TOKEN"
assert_code 200 "student report"
assert_jq '.finalGrade != null and (.raGrades | length) >= 2 and (.activityGrades | length) >= 2' "student report shape"

json_request GET "$BASE_URL/modules/$MODULE_ID/reports/evaluation/1" "" "$ADMIN_TOKEN"
assert_code 200 "evaluation 1 report"
assert_jq '(.students | length) >= 1' "evaluation 1 rows"

json_request GET "$BASE_URL/modules/$MODULE_ID/reports/evaluation/2" "" "$ADMIN_TOKEN"
assert_code 200 "evaluation 2 report"
assert_jq '(.students | length) >= 1' "evaluation 2 rows"

json_request GET "$BASE_URL/modules/$MODULE_ID/reports/final" "" "$ADMIN_TOKEN"
assert_code 200 "final report"
FINAL_BEFORE="$(echo "$RESPONSE_BODY" | jq -r '.students[0].finalGrade')"

echo "[7/10] Edicion dinamica y recalculo"
json_request PATCH "$BASE_URL/ras/$RA2_ID" "{\"weightPercent\":40}" "$ADMIN_TOKEN"
assert_code 200 "patch ra2 weight"

json_request PATCH "$BASE_URL/ras/$RA1_ID" "{\"weightPercent\":60}" "$ADMIN_TOKEN"
assert_code 200 "patch ra1 weight"

json_request GET "$BASE_URL/modules/$MODULE_ID/reports/final" "" "$ADMIN_TOKEN"
assert_code 200 "final report after patch"
FINAL_AFTER="$(echo "$RESPONSE_BODY" | jq -r '.students[0].finalGrade')"

if [[ "$FINAL_BEFORE" == "$FINAL_AFTER" ]]; then
  echo "ERROR: expected final grade to change after RA weight edit (before=$FINAL_BEFORE after=$FINAL_AFTER)" >&2
  exit 1
fi

echo "[8/10] Validacion de regla negocio (instrumento solo RA permitidos por UT)"
json_request POST "$BASE_URL/modules/$MODULE_ID/ras" "{\"code\":\"RA3-${TS}\",\"name\":\"Resultado 3\",\"weightPercent\":0}" "$ADMIN_TOKEN"
assert_code 201 "create ra3 with zero weight"
RA3_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/instruments/$I1_ID/ras" "{\"raIds\":[$RA3_ID]}" "$ADMIN_TOKEN"
if [[ "$RESPONSE_CODE" != "400" ]]; then
  echo "ERROR: expected HTTP 400 when linking instrument to RA not allowed by UT, got $RESPONSE_CODE" >&2
  echo "$RESPONSE_BODY" >&2
  exit 1
fi

echo "[9/10] Importacion RA mock (job + consulta + confirmacion)"
TMP_IMPORT_FILE="$(mktemp "${TMPDIR:-/tmp}/ra-import-XXXXXX")"
printf "Documento oficial RA - smoke %s\n" "$TS" > "$TMP_IMPORT_FILE"

multipart_request "$BASE_URL/imports/ra" "$MODULE_ID" "$TMP_IMPORT_FILE" "$ADMIN_TOKEN"
assert_code 201 "create import job"
assert_jq '.id != null and .status != null' "import job response"
JOB_ID="$(echo "$RESPONSE_BODY" | jq -r '.id')"

json_request GET "$BASE_URL/imports/ra/$JOB_ID" "" "$ADMIN_TOKEN"
assert_code 200 "get import job"
assert_jq '.status == "PARSED" or .status == "FAILED" or .status == "PROCESSING" or .status == "UPLOADED"' "import job status"

json_request POST "$BASE_URL/modules/$MODULE_ID/ras/import" "{\"ras\":[{\"code\":\"RAIMP-${TS}\",\"name\":\"Importado Smoke\",\"weightPercent\":0}]}" "$ADMIN_TOKEN"
assert_code 200 "confirm import ras"
assert_jq 'length == 1' "imported ras count"

rm -f "$TMP_IMPORT_FILE"

echo "[10/10] CRUD extra rapido (PATCH/PUT/DELETE)"
json_request PATCH "$BASE_URL/modules/$MODULE_ID/ut-ra/$LINK_UT1_RA1" "{\"percent\":60}" "$ADMIN_TOKEN"
assert_code 200 "patch ut-ra"

json_request PUT "$BASE_URL/instruments/$I3_ID" "{\"name\":\"I3-renamed-${TS}\",\"weightPercent\":100}" "$ADMIN_TOKEN"
assert_code 200 "put instrument"

json_request DELETE "$BASE_URL/instruments/$I2_ID/ras/$RA2_ID" "" "$ADMIN_TOKEN"
assert_code 200 "delete instrument-ra single"

json_request POST "$BASE_URL/instruments/$I2_ID/ras" "{\"raIds\":[$RA1_ID,$RA2_ID]}" "$ADMIN_TOKEN"
assert_code 200 "restore instrument-ra"

echo
printf 'OK: smoke test completado. moduleId=%s studentId=%s finalBefore=%s finalAfter=%s\n' \
  "$MODULE_ID" "$STUDENT_ID" "$FINAL_BEFORE" "$FINAL_AFTER"
