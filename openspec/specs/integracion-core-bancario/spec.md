# Integración con el core bancario (SOAP)

## Requisito: contrato primero
El servicio DEBE publicar un WSDL en `/ws/core-bancario.wsdl` generado desde `core-bancario.xsd`
(espacio de nombres `http://crediflow.co/core-bancario`).

### Escenario: registro de crédito aprobado
- **Cuando** el worker envía `RegistrarCreditoRequest` con los datos y condiciones del crédito
- **Entonces** recibe `RegistrarCreditoResponse` con `referencia`, `numeroCuenta` y `fechaDesembolso` (siguiente día hábil)
- **Y** la respuesta valida contra el XSD

### Escenario: idempotencia
- **Cuando** se registra dos veces la misma `solicitudId`
- **Entonces** se devuelve la misma referencia

### Escenario: indisponibilidad
- **Dado** el core no disponible
- **Entonces** responde un SOAP Fault de tipo Server y el worker reintenta
