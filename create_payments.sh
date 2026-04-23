#!/bin/bash

# Configuration
API_URL="http://localhost:8082/api/payments"
TEMP_JSON="/tmp/payment_body.json"

# ========================================
# Liste des UUIDs (ordre IDs)
# ========================================
UUIDS=(
    "11111111-1111-1111-1111-111111111111"
    "22222222-2222-2222-2222-222222222222"
    "33333333-3333-3333-3333-333333333333"
    "44444444-4444-4444-4444-444444444444"
    "55555555-5555-5555-5555-555555555555"
    "66666666-6666-6666-6666-666666666666"
    "77777777-7777-7777-7777-777777777777"
    "88888888-8888-8888-8888-888888888888"
    "99999999-9999-9999-9999-999999999999"
    "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
    "cccccccc-cccc-cccc-cccc-cccccccccccc"
    "dddddddd-dddd-dddd-dddd-dddddddddddd"
    "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"
    "ffffffff-ffff-ffff-ffff-ffffffffffff"
    "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    "b2c3d4e5-f6a7-8901-bcde-f12345678901"
    "c3d4e5f6-a7b8-9012-cdef-123456789012"
    "d4e5f6a7-b8c9-0123-defa-234567890123"
    "e5f6a7b8-c9d0-1234-efab-345678901234"
)

COUNT=${#UUIDS[@]}

# Vérifier si le service est accessible
echo "========================================"
echo "Verification du service..."
echo "========================================"
http_status=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL")
echo "HTTP Status: $http_status"
echo ""

if [ "$http_status" -eq 000 ]; then
    echo "Erreur: Impossible d'atteindre le service payment"
    echo "Verifiez que le service est demarre sur http://localhost:8082"
    exit 1
fi

echo ""
echo "========================================"
echo "Creation de $COUNT paiements..."
echo "========================================"
echo ""

for i in "${!UUIDS[@]}"; do
    index=$((i + 1))
    orderId="${UUIDS[$i]}"
    userId="user-$index"

    # Montant aléatoire (10-500 avec 2 décimales)
    amount_int=$(( (RANDOM % 491) + 10 ))
    amount_dec=$(( RANDOM % 100 ))
    amount=$(printf "%d.%02d" "$amount_int" "$amount_dec")

    # Écriture du JSON dans un fichier temp
    echo "{\"orderId\":\"$orderId\",\"userId\":\"$userId\",\"amount\":$amount}" > "$TEMP_JSON"

    echo "[$index/$COUNT] Envoi du paiement..."
    echo "OrderId: $orderId"
    echo "UserId:  $userId"
    echo "Amount:  $amount EUR"
    echo "JSON:"
    cat "$TEMP_JSON"
    echo ""

    curl -s -X POST "$API_URL" \
         -H "Content-Type: application/json" \
         -d @"$TEMP_JSON" \
         -w "\nHTTP Status: %{http_code}\n"

    echo ""
    echo "----------------------------------------"
    echo ""

    sleep 1
done

# Cleanup
[ -f "$TEMP_JSON" ] && rm "$TEMP_JSON"

echo ""
echo "========================================"
echo "Termine ! $COUNT paiements crees."
echo "========================================"