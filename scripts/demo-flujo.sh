#!/usr/bin/env bash
# Demo de punta a punta contra el stack de docker compose.
#   bash scripts/demo-flujo.sh            (API en http://localhost:8088)
#   API=http://otro:8080 bash scripts/demo-flujo.sh
#
# Registra 8 solicitudes con perfiles distintos, espera a que el worker las
# evalúe y muestra resultado, outbox e intentos.
set -euo pipefail
API=${API:-http://localhost:8088}
TMP=$(mktemp)

productos=(VEHICULO VIVIENDA EDUCACION LIBRE_INVERSION VEHICULO VIVIENDA LIBRE_INVERSION EDUCACION)
montos=(45000000 180000000 18000000 12000000 90000000 350000000 60000000 25000000)
plazos=(60 240 36 24 72 300 48 60)
ingresos=(9500000 14000000 4200000 3800000 5200000 9000000 4500000 7000000)
gastos=(2500000 4000000 1200000 900000 3900000 6500000 3800000 1500000)
nombres=("Ana María Ríos" "Juan Esteban Ortiz" "Valentina Cárdenas" "Santiago Muñoz" "Daniela Restrepo" "Andrés Felipe Gil" "Camila Herrera" "Mateo Salazar")

lote=$(date +%s)
echo "Registrando 8 solicitudes..."
for i in "${!productos[@]}"; do
  # El cuerpo va por archivo: en Windows los argumentos no viajan en UTF-8
  printf '{"externalId":"demo-%s-%s","numeroDocumento":"%s","nombreCompleto":"%s","email":"cliente%s@correo.co","ingresosMensuales":%s,"gastosMensuales":%s,"tipoProducto":"%s","monto":%s,"plazoMeses":%s}' \
    "$lote" "$i" "$((1010000000 + i))" "${nombres[$i]}" "$i" "${ingresos[$i]}" "${gastos[$i]}" "${productos[$i]}" "${montos[$i]}" "${plazos[$i]}" > "$TMP"
  curl -s -o /dev/null -w "  %{http_code} ${nombres[$i]}\n" -X POST "$API/api/v1/solicitudes" \
       -H 'Content-Type: application/json; charset=utf-8' --data-binary "@$TMP"
done

en_proceso() { curl -s "$API/api/v1/estadisticas" | python -c "import json,sys; print(json.load(sys.stdin)['enProceso'])"; }
inicio=$(date +%s)
until [ "$(en_proceso)" = "0" ] || [ $(( $(date +%s) - inicio )) -gt 180 ]; do sleep 2; done
echo "Evaluadas en $(( $(date +%s) - inicio )) s"

curl -s "$API/api/v1/solicitudes?tamano=8" | python -c "
import json,sys
for s in json.load(sys.stdin)['contenido']:
    print('  #%-4s %-24s %-16s %-10s score=%s' % (s['id'], s['nombreCompleto'], s['tipoProducto'], s['estado'], s['score']))"
rm -f "$TMP"
echo "Abre http://localhost:${FRONTEND_PORT:-3100} para verlas en el panel."
