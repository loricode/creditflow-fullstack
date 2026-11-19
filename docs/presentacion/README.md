# Presentación de CrediFlow

## Video

| Archivo | Duración | Contenido |
|---|---|---|
| [01-camino-feliz.mp4](videos/01-camino-feliz.mp4) | 4:35 | Camino feliz de punta a punta: panel, simulador, solicitud en 3 pasos, evaluación en vivo, resultado PL/SQL, explicación con IA, historial por trigger, intentos, amortización, original en S3, búsqueda y panel actualizado. |
| [01-camino-feliz-narracion.mp3](videos/01-camino-feliz-narracion.mp3) | 4:35 | Solo la narración. |

Narrado en español (voz `es-CO-SalomeNeural`) con subtítulos. Se graba desde la aplicación real con
[el grabador](../video/grabador/LEEME.md): `node docs/video/grabador/grabar.js`.

## Guion para una demo en vivo (5 minutos)

1. **Panel** (30 s): indicadores y el recorrido de la arquitectura (API → Outbox → SNS → SQS → Worker).
2. **Simulador** (30 s): cambiar de producto; explicar que la cuota sale de `PKG_CREDITO`, igual que la decisión.
3. **Nueva solicitud** (1 min): los 3 pasos, la capacidad de pago y la regla del 40 %. Al enviar: respuesta 201,
   outbox en la misma transacción e idempotencia por `externalId`.
4. **Detalle** (1,5 min): recorrido en vivo, score y tasa por riesgo, explicación (Claude o reglas), historial
   escrito por el trigger, amortización generada con `FORALL` y original cifrado en S3.
5. **Resiliencia** (1 min): `bash scripts/demo-fallos.sh`. El core falla y se ven 3 reintentos, la DLQ y luego
   «Reintentar» → APROBADA.
6. **Código** (30 s): `ArquitecturaTest`, `PaquetesPlsqlIT` contra Oracle real y los módulos de Terraform.
