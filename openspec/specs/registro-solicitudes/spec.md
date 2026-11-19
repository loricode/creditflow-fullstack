# Registro de solicitudes

## Requisito: validar los datos de entrada
La API DEBE rechazar con 400 y `application/problem+json` cualquier solicitud inválida, indicando el error de cada campo.

### Escenario: datos inválidos
- **Dado** un documento con menos de 6 dígitos, un correo mal formado, un monto menor a 1.000.000 y un plazo menor a 6
- **Cuando** se envía `POST /api/v1/solicitudes`
- **Entonces** la respuesta es 400 con `title = "Datos inválidos"` y `errores` contiene `numeroDocumento`, `email`, `monto` y `plazoMeses`

## Requisito: idempotencia por clave externa
Reenviar una solicitud con el mismo `externalId` NO DEBE crear otra ni publicar otro evento.

### Escenario: reenvío
- **Dado** una solicitud registrada con `externalId = k`
- **Cuando** se envía otra con `externalId = k`
- **Entonces** la respuesta es 200 con la solicitud original y el outbox no cambia

### Escenario: alta nueva
- **Cuando** se registra una solicitud nueva
- **Entonces** la respuesta es 201 con cabecera `Location` y estado `RECIBIDA`

## Requisito: outbox transaccional
La solicitud y su evento `SolicitudRecibida` DEBEN guardarse en la misma transacción.

### Escenario: publicación
- **Dado** un evento `PENDIENTE`
- **Cuando** el publisher lo envía a SNS
- **Entonces** queda `PUBLICADO` con fecha, y lo bloquea con `FOR UPDATE SKIP LOCKED` para que dos réplicas no lo publiquen a la vez

### Escenario: SNS no disponible
- **Cuando** la publicación falla 5 veces
- **Entonces** el evento queda `FALLIDO` con el último error
