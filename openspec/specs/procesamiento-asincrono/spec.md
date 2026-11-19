# Procesamiento asíncrono

## Requisito: reintentos con límite
Un fallo recuperable NO DEBE borrar el mensaje de SQS. Tras `maxReceiveCount = 3` el mensaje DEBE ir a la DLQ.

### Escenario: el core falla siempre
- **Dado** el core bancario respondiendo SOAP Fault
- **Cuando** se registra una solicitud que se aprobaría
- **Entonces** el historial muestra `RECIBIDA → EN_EVALUACION → RECIBIDA → EN_EVALUACION → RECIBIDA → EN_EVALUACION → ERROR`
- **Y** hay 3 intentos con resultado FALLO y la DLQ tiene 1 mensaje

### Escenario: reintento manual
- **Dado** una solicitud en ERROR y el core sano
- **Cuando** se envía `POST /api/v1/solicitudes/{id}/reintentar`
- **Entonces** la respuesta es 202 y la solicitud termina APROBADA con un cuarto intento EXITO

### Escenario: reintentar una decidida
- **Entonces** la respuesta es 409

## Requisito: un solo worker por solicitud
El worker DEBE tomar la solicitud con `SELECT … FOR UPDATE` y solo si está RECIBIDA, o EN_EVALUACION hace más de 120 s.

### Escenario: mensaje duplicado
- **Dado** una solicitud ya decidida
- **Cuando** llega otro mensaje para ella
- **Entonces** se descarta y se borra de la cola
