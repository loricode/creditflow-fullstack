# Historias de usuario

Cada historia indica la prueba automatizada o la demo que la verifica.

## Solicitante

**HU-01 · Pedir un crédito en pocos pasos**
Como solicitante quiero completar mis datos en un asistente de 3 pasos para no enfrentarme a un formulario largo.
- *Dado* el asistente abierto, *cuando* dejo un campo inválido, *entonces* no puedo avanzar y veo el error en el campo.
- *Cuando* el backend rechaza un dato, *entonces* el asistente vuelve al paso del campo y lo marca.
- Verifica: `SolicitudControllerTest.datosInvalidosDevuelvenProblemDetailConCampos`, `NuevaSolicitudDialogComponent`.

**HU-02 · Ver mi cuota antes de enviar**
Como solicitante quiero ver la cuota y la parte de mi capacidad de pago que ocupa mientras muevo monto y plazo.
- *Cuando* cambio monto, plazo o producto, *entonces* la cuota se recalcula con `PKG_CREDITO` en menos de 1 s.
- Verifica: `PaquetesPlsqlIT.simuladorCalculaCuotaFrancesa`, pantalla Simulador.

**HU-03 · No duplicar mi solicitud**
Como solicitante quiero que un doble clic o un corte de red no cree dos solicitudes.
- *Dado* un `externalId` ya registrado, *cuando* reenvío, *entonces* recibo 200 con la solicitud existente y no se publica otro evento.
- Verifica: `SolicitudServiceTest.esIdempotentePorExternalId`, `scripts/demo-flujo.sh`.

**HU-04 · Entender la decisión**
Como solicitante quiero una explicación clara del resultado y, si me rechazan, qué puedo mejorar.
- *Dado* un rechazo por carga de cuota, *entonces* la explicación menciona el límite del 40 % y sugiere un monto menor o un plazo mayor.
- *Si* la IA falla o se niega, *entonces* recibo igualmente una explicación generada por reglas.
- Verifica: `ExplicacionIaServiceTest` (4 casos de respaldo y uno de privacidad).

**HU-05 · Conocer mi plan de pagos**
Como solicitante aprobado quiero ver mi tabla de amortización.
- *Entonces* tiene tantas cuotas como meses, la suma del capital es igual al monto y el saldo final es 0.
- Verifica: `PaquetesPlsqlIT.perfilSolventeSeApruebaYGeneraAmortizacionCompleta`.

## Analista

**HU-06 · Panel del día**
Como analista quiero ver total, tasa de aprobación, monto aprobado, score promedio, la serie de 7 días y el desglose por producto.
- *Entonces* los días se agrupan en hora de Colombia, no en UTC.
- Verifica: `PaquetesPlsqlIT.estadisticasDevuelvenSerieDeSieteDias`, `SolicitudMapperTest.tasaDeAprobacionSobreDecididas`.

**HU-07 · Buscar y filtrar**
Como analista quiero filtrar por estado y producto, buscar por nombre, documento o clave, y ordenar y paginar.
- Verifica: `SolicitudesService.spec` (filtros), `SolicitudControllerTest.estadoDesconocidoEnElFiltroDevuelve400`.

**HU-08 · Seguir una evaluación en vivo**
Como analista quiero ver avanzar la solicitud por cada etapa sin recargar la página.
- *Mientras* está en proceso, *entonces* el detalle se refresca cada 1,5 s y se detiene al terminar.
- Verifica: pantalla de detalle (recorrido de 6 etapas).

**HU-09 · Auditoría**
Como analista quiero el historial completo de estados con fecha y hora.
- *Entonces* lo registra la base de datos (trigger), no la aplicación.
- Verifica: `PaquetesPlsqlIT.elTriggerAuditaCadaCambioDeEstado`.

**HU-10 · Reintentar solicitudes con error**
Como analista quiero reenviar a evaluación una solicitud que agotó sus intentos.
- *Dado* estado `ERROR`, *cuando* reintento, *entonces* vuelve a `RECIBIDA` y se publica un nuevo evento (202).
- *Dado* otro estado, *entonces* recibo 409.
- Verifica: `SolicitudServiceTest.reintentar*`, `SolicitudCreditoTest.soloSeReintentaDesdeError`, `scripts/demo-fallos.sh`.

## Operación (TI)

**HU-11 · No perder eventos**
- *Si* SNS falla, *entonces* el evento queda pendiente y se reintenta hasta 5 veces antes de marcarse `FALLIDO`.
- *Entonces* el lote se publica dentro de una transacción (regresión de un bug real).
- Verifica: `OutboxPublisherTest` (4 casos).

**HU-12 · Reintentos y DLQ**
- *Cuando* el core falla, *entonces* hay 3 intentos espaciados; al tercero la solicitud queda en `ERROR` y el mensaje en la DLQ.
- *Cuando* hay un error de negocio de PL/SQL (`ORA-20xxx`), *entonces* no se reintenta.
- Verifica: `EvaluacionServiceTest`, `SolicitudEstadoServiceTest`, `scripts/demo-fallos.sh`.

**HU-13 · Un solo worker por solicitud**
- *Dado* un mensaje duplicado, *cuando* otro worker ya la evalúa, *entonces* se descarta. Si el worker anterior murió, se retoma después de 120 s.
- Verifica: `SolicitudCreditoTest.unaSolicitudEnEvaluacionRecienteNoSePuedeTomarDosVeces`.

**HU-14 · Infraestructura reproducible**
- *Cuando* ejecuto `docker compose up`, *entonces* Terraform crea S3, SNS, SQS, la DLQ, los secretos y los roles IAM en LocalStack.
- Verifica: job `terraform` del CI (`fmt` + `validate` de ambos entornos).

**HU-15 · Contrato SOAP**
- *Entonces* el core publica un WSDL generado desde el XSD y sus respuestas validan contra él.
- Verifica: `CoreBancarioEndpointTest.registraCreditoYRespondeSegunElContratoXsd`.
