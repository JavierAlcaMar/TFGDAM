#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@admin.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-adminmiralmonte}"

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required" >&2
  exit 1
fi

RESP_BODY=""
RESP_CODE=""

json_request() {
  local method="$1"
  local url="$2"
  local payload="${3:-}"
  local token="${4:-}"
  local tmp
  tmp="$(mktemp)"

  if [[ -n "$token" ]]; then
    if [[ -n "$payload" ]]; then
      RESP_CODE="$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$url" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "$payload")"
    else
      RESP_CODE="$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$url" \
        -H "Authorization: Bearer $token")"
    fi
  else
    if [[ -n "$payload" ]]; then
      RESP_CODE="$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -d "$payload")"
    else
      RESP_CODE="$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$url")"
    fi
  fi

  RESP_BODY="$(cat "$tmp")"
  rm -f "$tmp"
}

assert_code() {
  local expected="$1"
  local label="$2"
  if [[ "$RESP_CODE" != "$expected" ]]; then
    echo "ERROR [$label]: expected HTTP $expected, got $RESP_CODE" >&2
    echo "$RESP_BODY" >&2
    exit 1
  fi
}

assert_jq() {
  local expr="$1"
  local label="$2"
  if ! echo "$RESP_BODY" | jq -e "$expr" >/dev/null; then
    echo "ERROR [$label]: assertion failed: $expr" >&2
    echo "$RESP_BODY" >&2
    exit 1
  fi
}

assert_close() {
  local actual="$1"
  local expected="$2"
  local tolerance="$3"
  local label="$4"
  python3 - <<PY
actual=float("$actual")
expected=float("$expected")
tolerance=float("$tolerance")
if abs(actual-expected) > tolerance:
    raise SystemExit(f"{actual} != {expected} (+/-{tolerance})")
PY
  if [[ $? -ne 0 ]]; then
    echo "ERROR [$label]: actual=$actual expected=$expected tolerance=$tolerance" >&2
    exit 1
  fi
}

echo "[0/6] Login superadmin"
json_request POST "$BASE_URL/auth/login" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"
assert_code 200 "admin login"
TOKEN="$(echo "$RESP_BODY" | jq -r '.accessToken')"
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "ERROR: empty accessToken" >&2
  exit 1
fi

TS="$(date +%s)"

echo "[1/6] Caso A (numeric > 5 y todos RA superados => redondeo)"
json_request POST "$BASE_URL/modules" "{\"name\":\"PARITY-A-$TS\",\"academicYear\":\"2025-2026\",\"teacherName\":\"Parity Teacher A\"}" "$TOKEN"
assert_code 201 "create module A"
MOD_A="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MOD_A/ras" "{\"code\":\"RAA1-$TS\",\"name\":\"RA A1\",\"weightPercent\":100}" "$TOKEN"
assert_code 201 "create RA A1"
RA_A1="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MOD_A/uts" "{\"name\":\"UTA1-$TS\",\"evaluationPeriod\":1}" "$TOKEN"
assert_code 201 "create UT A1"
UT_A1="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MOD_A/ut-ra" "{\"utId\":$UT_A1,\"raId\":$RA_A1,\"percent\":100}" "$TOKEN"
assert_code 201 "link UT A1 -> RA A1"

json_request POST "$BASE_URL/uts/$UT_A1/instruments" "{\"name\":\"IA1-$TS\",\"weightPercent\":60}" "$TOKEN"
assert_code 201 "create IA1"
I_A1="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/uts/$UT_A1/instruments" "{\"name\":\"IA2-$TS\",\"weightPercent\":40}" "$TOKEN"
assert_code 201 "create IA2"
I_A2="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/instruments/$I_A1/ras" "{\"raIds\":[$RA_A1]}" "$TOKEN"
assert_code 200 "set IA1 ras"

json_request POST "$BASE_URL/instruments/$I_A2/ras" "{\"raIds\":[$RA_A1]}" "$TOKEN"
assert_code 200 "set IA2 ras"

json_request POST "$BASE_URL/students" "{\"moduleId\":$MOD_A,\"studentCode\":\"PA-$TS\",\"fullName\":\"Parity Alumno A\"}" "$TOKEN"
assert_code 201 "create student A"
ST_A="$(echo "$RESP_BODY" | jq -r '.id')"

# expected numeric = 8.25*0.60 + 4.75*0.40 = 6.85
json_request POST "$BASE_URL/grades" "{\"grades\":[{\"studentId\":$ST_A,\"instrumentId\":$I_A1,\"gradeValue\":8.25},{\"studentId\":$ST_A,\"instrumentId\":$I_A2,\"gradeValue\":4.75}]}" "$TOKEN"
assert_code 201 "grades A"

json_request GET "$BASE_URL/modules/$MOD_A/reports/evaluation/1" "" "$TOKEN"
assert_code 200 "eval report A"
NUM_A="$(echo "$RESP_BODY" | jq -r '.students[0].numericGrade')"
BUL_A="$(echo "$RESP_BODY" | jq -r '.students[0].suggestedBulletinGrade')"
PASS_A="$(echo "$RESP_BODY" | jq -r '.students[0].allRAsPassed')"
assert_close "$NUM_A" "6.85" "0.0001" "numeric A"
[[ "$BUL_A" == "7" ]] || { echo "ERROR [bulletin A]: expected 7 got $BUL_A" >&2; exit 1; }
[[ "$PASS_A" == "true" ]] || { echo "ERROR [allRAsPassed A]: expected true got $PASS_A" >&2; exit 1; }

echo "[2/6] Caso B (numeric < 1 => boletin 1)"
json_request POST "$BASE_URL/grades" "{\"grades\":[{\"studentId\":$ST_A,\"instrumentId\":$I_A1,\"gradeValue\":0},{\"studentId\":$ST_A,\"instrumentId\":$I_A2,\"gradeValue\":0.5}]}" "$TOKEN"
assert_code 201 "grades A low"

json_request GET "$BASE_URL/modules/$MOD_A/reports/evaluation/1" "" "$TOKEN"
assert_code 200 "eval report A low"
NUM_B="$(echo "$RESP_BODY" | jq -r '.students[0].numericGrade')"
BUL_B="$(echo "$RESP_BODY" | jq -r '.students[0].suggestedBulletinGrade')"
assert_close "$NUM_B" "0.2" "0.0001" "numeric B"
[[ "$BUL_B" == "1" ]] || { echo "ERROR [bulletin B]: expected 1 got $BUL_B" >&2; exit 1; }

echo "[3/6] Caso C (numeric > 5 y algun RA suspenso => boletin 4) y Caso D (<5 truncado)"
json_request POST "$BASE_URL/modules" "{\"name\":\"PARITY-B-$TS\",\"academicYear\":\"2025-2026\",\"teacherName\":\"Parity Teacher B\"}" "$TOKEN"
assert_code 201 "create module B"
MOD_C="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MOD_C/ras" "{\"code\":\"RAB1-$TS\",\"name\":\"RA B1\",\"weightPercent\":50}" "$TOKEN"
assert_code 201 "create RA B1"
RA_B1="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MOD_C/ras" "{\"code\":\"RAB2-$TS\",\"name\":\"RA B2\",\"weightPercent\":50}" "$TOKEN"
assert_code 201 "create RA B2"
RA_B2="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MOD_C/uts" "{\"name\":\"UTB1-$TS\",\"evaluationPeriod\":1}" "$TOKEN"
assert_code 201 "create UT B1"
UT_B1="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MOD_C/ut-ra" "{\"utId\":$UT_B1,\"raId\":$RA_B1,\"percent\":100}" "$TOKEN"
assert_code 201 "link UT B1 -> RA B1"
json_request POST "$BASE_URL/modules/$MOD_C/ut-ra" "{\"utId\":$UT_B1,\"raId\":$RA_B2,\"percent\":100}" "$TOKEN"
assert_code 201 "link UT B1 -> RA B2"

json_request POST "$BASE_URL/uts/$UT_B1/instruments" "{\"name\":\"IB1-$TS\",\"weightPercent\":50}" "$TOKEN"
assert_code 201 "create IB1"
I_B1="$(echo "$RESP_BODY" | jq -r '.id')"
json_request POST "$BASE_URL/uts/$UT_B1/instruments" "{\"name\":\"IB2-$TS\",\"weightPercent\":50}" "$TOKEN"
assert_code 201 "create IB2"
I_B2="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/instruments/$I_B1/ras" "{\"raIds\":[$RA_B1]}" "$TOKEN"
assert_code 200 "set IB1 ras"
json_request POST "$BASE_URL/instruments/$I_B2/ras" "{\"raIds\":[$RA_B2]}" "$TOKEN"
assert_code 200 "set IB2 ras"

json_request POST "$BASE_URL/students" "{\"moduleId\":$MOD_C,\"studentCode\":\"PB-$TS\",\"fullName\":\"Parity Alumno B\"}" "$TOKEN"
assert_code 201 "create student B"
ST_B="$(echo "$RESP_BODY" | jq -r '.id')"

# numeric = 8.4*0.5 + 2*0.5 = 5.2 ; RA2 suspenso => boletin 4
json_request POST "$BASE_URL/grades" "{\"grades\":[{\"studentId\":$ST_B,\"instrumentId\":$I_B1,\"gradeValue\":8.4},{\"studentId\":$ST_B,\"instrumentId\":$I_B2,\"gradeValue\":2.0}]}" "$TOKEN"
assert_code 201 "grades C"
json_request GET "$BASE_URL/modules/$MOD_C/reports/evaluation/1" "" "$TOKEN"
assert_code 200 "eval report C"
NUM_C="$(echo "$RESP_BODY" | jq -r '.students[0].numericGrade')"
BUL_C="$(echo "$RESP_BODY" | jq -r '.students[0].suggestedBulletinGrade')"
PASS_C="$(echo "$RESP_BODY" | jq -r '.students[0].allRAsPassed')"
assert_close "$NUM_C" "5.2" "0.0001" "numeric C"
[[ "$BUL_C" == "4" ]] || { echo "ERROR [bulletin C]: expected 4 got $BUL_C" >&2; exit 1; }
[[ "$PASS_C" == "false" ]] || { echo "ERROR [allRAsPassed C]: expected false got $PASS_C" >&2; exit 1; }

# numeric = 4.9 => trunc(4.9)=4
json_request POST "$BASE_URL/grades" "{\"grades\":[{\"studentId\":$ST_B,\"instrumentId\":$I_B1,\"gradeValue\":7.8},{\"studentId\":$ST_B,\"instrumentId\":$I_B2,\"gradeValue\":2.0}]}" "$TOKEN"
assert_code 201 "grades D"
json_request GET "$BASE_URL/modules/$MOD_C/reports/evaluation/1" "" "$TOKEN"
assert_code 200 "eval report D"
NUM_D="$(echo "$RESP_BODY" | jq -r '.students[0].numericGrade')"
BUL_D="$(echo "$RESP_BODY" | jq -r '.students[0].suggestedBulletinGrade')"
assert_close "$NUM_D" "4.9" "0.0001" "numeric D"
[[ "$BUL_D" == "4" ]] || { echo "ERROR [bulletin D]: expected 4 got $BUL_D" >&2; exit 1; }

echo "[4/6] Validaciones de consistencia (sumas y restricciones)"
# RA weights > 100
json_request POST "$BASE_URL/modules/$MOD_C/ras" "{\"code\":\"RAB3-$TS\",\"name\":\"RA B3\",\"weightPercent\":1}" "$TOKEN"
[[ "$RESP_CODE" == "400" ]] || { echo "ERROR [RA weight >100]: expected 400 got $RESP_CODE" >&2; echo "$RESP_BODY" >&2; exit 1; }

# create UT2 to force RA distribution > 100
json_request POST "$BASE_URL/modules/$MOD_C/uts" "{\"name\":\"UTB2-$TS\",\"evaluationPeriod\":1}" "$TOKEN"
assert_code 201 "create UT B2"
UT_B2="$(echo "$RESP_BODY" | jq -r '.id')"

json_request POST "$BASE_URL/modules/$MOD_C/ut-ra" "{\"utId\":$UT_B2,\"raId\":$RA_B1,\"percent\":1}" "$TOKEN"
[[ "$RESP_CODE" == "400" ]] || { echo "ERROR [UT-RA >100]: expected 400 got $RESP_CODE" >&2; echo "$RESP_BODY" >&2; exit 1; }

# instrument weights > 100 in same activity
json_request POST "$BASE_URL/uts/$UT_B1/instruments" "{\"name\":\"IB3-$TS\",\"weightPercent\":10}" "$TOKEN"
[[ "$RESP_CODE" == "400" ]] || { echo "ERROR [Instrument weights >100]: expected 400 got $RESP_CODE" >&2; echo "$RESP_BODY" >&2; exit 1; }

# Instrument can only link RAs allowed by UT distribution (>0)
json_request POST "$BASE_URL/modules/$MOD_C/ras" "{\"code\":\"RAB4-$TS\",\"name\":\"RA B4\",\"weightPercent\":0}" "$TOKEN"
assert_code 201 "create RA B4 (0%)"
RA_B4="$(echo "$RESP_BODY" | jq -r '.id')"
json_request POST "$BASE_URL/instruments/$I_B1/ras" "{\"raIds\":[$RA_B4]}" "$TOKEN"
[[ "$RESP_CODE" == "400" ]] || { echo "ERROR [Instrument RA allowed-by-UT]: expected 400 got $RESP_CODE" >&2; echo "$RESP_BODY" >&2; exit 1; }

# cleanup: remove temporary UT used for failing UT-RA validation
json_request DELETE "$BASE_URL/uts/$UT_B2" "" "$TOKEN"
assert_code 204 "delete temporary UT_B2"

# cleanup: remove temporary RA used only for validation, so module remains report-ready
json_request DELETE "$BASE_URL/ras/$RA_B4" "" "$TOKEN"
assert_code 204 "delete temporary RA_B4"

echo "[5/6] Reporte final dinamico"
json_request GET "$BASE_URL/students/$ST_B/report?moduleId=$MOD_C" "" "$TOKEN"
assert_code 200 "student report parity"
assert_jq '.finalGrade != null and (.raGrades|length) >= 2 and (.evaluationGrades|length) >= 1' "student report parity structure"

echo "[6/6] OK parity Excel/API validada para reglas clave"
echo "moduleA=$MOD_A moduleB=$MOD_C studentA=$ST_A studentB=$ST_B"
