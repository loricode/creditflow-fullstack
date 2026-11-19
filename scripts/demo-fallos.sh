#!/usr/bin/env bash
# Demo de resiliencia: el core bancario falla siempre ->
#   3 intentos (visibility timeout de SQS) -> estado ERROR -> mensaje en la DLQ
#   -> se arregla el core -> "Reintentar" desde la API -> APROBADA.
set -euo pipefail
API=${API:-http://localhost:8088}
cd "$(dirname "$0")/.."
TMP=$(mktemp)

sano() { until docker inspect -f '{{.State.Health.Status}}' crediflow-core-bancario | grep -q '^healthy'; do sleep 2; done; }
estado() { curl -s "$API/api/v1/solicitudes/$1" | python -c "import json,sys; print(json.load(sys.stdin)['estado'])"; }

echo "1) Core bancario fallando al 100 %"
CORE_TASA_FALLO=1 docker compose up -d core-bancario >/dev/null 2>&1; sano

printf '{"externalId":"fallo-%s","numeroDocumento":"1098765432","nombreCompleto":"Sofía Vargas","email":"sofia@correo.co","ingresosMensuales":9000000,"gastosMensuales":2000000,"tipoProducto":"VEHICULO","monto":40000000,"plazoMeses":60}' "$(date +%s)" > "$TMP"
ID=$(curl -s -X POST "$API/api/v1/solicitudes" -H 'Content-Type: application/json; charset=utf-8' --data-binary "@$TMP" \
     | python -c "import json,sys; print(json.load(sys.stdin)['id'])")
echo "2) Solicitud #$ID registrada; esperando los 3 intentos..."
until [ "$(estado "$ID")" = "ERROR" ]; do sleep 2; done
curl -s "$API/api/v1/solicitudes/$ID" | python -c "
import json,sys; d=json.load(sys.stdin)
print('   historial:', ' -> '.join(h['estadoNuevo'] for h in d['historial']))
print('   error    :', d['ultimoError'])"

echo "3) Mensajes en la DLQ:"
docker exec crediflow-localstack awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/crediflow-evaluacion-dlq \
  --attribute-names ApproximateNumberOfMessages --query 'Attributes.ApproximateNumberOfMessages' --output text

echo "4) Core bancario sano otra vez; reintentando desde la API"
docker compose up -d core-bancario >/dev/null 2>&1; sano
curl -s -o /dev/null -w "   POST /reintentar -> HTTP %{http_code}\n" -X POST "$API/api/v1/solicitudes/$ID/reintentar"
until [ "$(estado "$ID")" = "APROBADA" ]; do sleep 2; done
echo "   Solicitud #$ID APROBADA. Mírala en http://localhost:${FRONTEND_PORT:-3100}/solicitudes/$ID"
rm -f "$TMP"
