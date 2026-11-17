#!/usr/bin/env bash
set -euo pipefail

: "${SLIDEBOLT_API_URL:?set SLIDEBOLT_API_URL}"
: "${SLIDEBOLT_ADMIN_TOKEN:?set SLIDEBOLT_ADMIN_TOKEN}"

api="${SLIDEBOLT_API_URL%/}"
name="android-live-${BUILD_TAG:-manual}-$$"
temporary_token=""
temporary_id=""

cleanup() {
  if [[ -n "$temporary_id" ]]; then
    curl -fsS -X DELETE -H "Authorization: Bearer $SLIDEBOLT_ADMIN_TOKEN" "$api/tokens/$temporary_id" >/dev/null || true
  fi
}
trap cleanup EXIT

known=$(curl -fsS "$api/.well-known/slidebolt")
jq -e '.pairing == true and (.id | length > 0) and (.apiUrl | length > 0)' <<<"$known" >/dev/null

create=$(curl -fsS -X POST -H 'Content-Type: application/json' \
  --data "$(jq -nc --arg name "$name" '{clientName:$name,clientType:"android-live-verifier",requestedAccess:"control",requestedSecurityGroups:["*"]}')" \
  "$api/pairing/requests")
pairing_id=$(jq -er '.id' <<<"$create")
code=$(jq -er '.code' <<<"$create")
verifier=$(jq -er '.verifier' <<<"$create")

curl -fsS -X POST -H "Authorization: Bearer $SLIDEBOLT_ADMIN_TOKEN" -H 'Content-Type: application/json' \
  --data '{"access":"control","securityGroups":["*"]}' "$api/pairing/requests/$code/approve" >/dev/null

exchange=$(curl -fsS -X POST -H 'Content-Type: application/json' \
  --data "$(jq -nc --arg verifier "$verifier" '{verifier:$verifier}')" \
  "$api/pairing/requests/$pairing_id/exchange")
temporary_token=$(jq -er 'select(.status == "approved") | .token' <<<"$exchange")

curl -fsS -H "Authorization: Bearer $temporary_token" "$api/definitions" | jq -e 'type == "object"' >/dev/null
curl -fsS -H "Authorization: Bearer $temporary_token" "$api/devices" | jq -e 'type == "array" or (.items | type == "array")' >/dev/null
curl -fsS -H "Authorization: Bearer $temporary_token" "$api/entities" | jq -e 'type == "array" or (.items | type == "array")' >/dev/null

temporary_id=$(curl -fsS -H "Authorization: Bearer $SLIDEBOLT_ADMIN_TOKEN" "$api/tokens" | jq -er --arg name "$name" '.[] | select(.name == $name) | .id')
curl -fsS -X DELETE -H "Authorization: Bearer $SLIDEBOLT_ADMIN_TOKEN" "$api/tokens/$temporary_id" >/dev/null
temporary_id=""

status=$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $temporary_token" "$api/devices")
test "$status" = "401"
printf 'PASS: discovery contract, pairing, authenticated inventory, revocation\n'

