#!/usr/bin/env bash
# End-to-end happy path through the gateway:
# organizer creates + publishes an event, attendee buys tickets, pays, and the
# organizer checks a ticket in. Requires curl and jq.
#
# Usage: scripts/smoke-test.sh [base-url]   (default http://localhost:8080)
set -euo pipefail

BASE=${1:-http://localhost:8080}
RUN=$(date +%s)
json() { curl -fsS -H 'Content-Type: application/json' "$@"; }

echo "==> Registering organizer and attendee"
ORG_TOKEN=$(json -X POST "$BASE/api/auth/register" \
  -d "{\"email\":\"org$RUN@example.com\",\"password\":\"password123\",\"fullName\":\"Olivia Organizer\",\"role\":\"ORGANIZER\"}" \
  | jq -r .accessToken)
FAN_TOKEN=$(json -X POST "$BASE/api/auth/register" \
  -d "{\"email\":\"fan$RUN@example.com\",\"password\":\"password123\",\"fullName\":\"Frank Fan\"}" \
  | jq -r .accessToken)

echo "==> Creating event"
STARTS=$(date -u -d '+14 days' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v+14d +%Y-%m-%dT%H:%M:%SZ)
ENDS=$(date -u -d '+14 days 3 hours' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v+14d -v+3H +%Y-%m-%dT%H:%M:%SZ)
EVENT_ID=$(json -X POST "$BASE/api/events" -H "Authorization: Bearer $ORG_TOKEN" \
  -d "{\"title\":\"Smoke Test Live $RUN\",\"description\":\"An evening of tests\",\"venue\":\"Main Hall\",\"city\":\"Austin\",\"category\":\"music\",\"startsAt\":\"$STARTS\",\"endsAt\":\"$ENDS\"}" \
  | jq -r .id)

TT_ID=$(json -X POST "$BASE/api/events/$EVENT_ID/ticket-types" -H "Authorization: Bearer $ORG_TOKEN" \
  -d '{"name":"General Admission","priceCents":2500,"currency":"USD","capacity":100}' | jq -r .id)
json -X POST "$BASE/api/events/$EVENT_ID/publish" -H "Authorization: Bearer $ORG_TOKEN" > /dev/null
echo "    event=$EVENT_ID ticketType=$TT_ID"

echo "==> Attendee places an order for 2 tickets"
ORDER_ID=$(json -X POST "$BASE/api/orders" -H "Authorization: Bearer $FAN_TOKEN" \
  -d "{\"ticketTypeId\":\"$TT_ID\",\"quantity\":2}" | jq -r .id)

echo "==> Paying (twice with the same Idempotency-Key; second call must be a no-op)"
for _ in 1 2; do
  json -X POST "$BASE/api/payments" -H "Authorization: Bearer $FAN_TOKEN" -H "Idempotency-Key: smoke-$RUN" \
    -d "{\"orderId\":\"$ORDER_ID\",\"cardToken\":\"tok_visa\"}" | jq -c '{id, status, amountCents}'
done

STATUS=$(json "$BASE/api/orders/$ORDER_ID" -H "Authorization: Bearer $FAN_TOKEN" | jq -r .status)
[ "$STATUS" = "CONFIRMED" ] || { echo "expected CONFIRMED, got $STATUS"; exit 1; }

CODE=$(json "$BASE/api/tickets" -H "Authorization: Bearer $FAN_TOKEN" | jq -r '.[0].code')
echo "==> Checking in ticket $CODE"
json -X POST "$BASE/api/tickets/$CODE/check-in" -H "Authorization: Bearer $ORG_TOKEN" | jq -c '{code, status}'

echo "==> Sales report"
json "$BASE/api/events/$EVENT_ID/sales" -H "Authorization: Bearer $ORG_TOKEN" | jq -c '{totalSold, grossRevenueCents}'

sleep 1
echo "==> Attendee notifications"
json "$BASE/api/notifications/unread-count" -H "Authorization: Bearer $FAN_TOKEN"
echo
echo "Smoke test passed."
