# OpenSpec · CrediFlow

Las specs describen **comportamiento observable** con escenarios Dado/Cuando/Entonces. Sirven de contrato
entre el negocio, el código y los asistentes de IA: antes de cambiar un comportamiento se actualiza su spec.

| Spec | Qué cubre |
|---|---|
| [registro-solicitudes](specs/registro-solicitudes/spec.md) | Validación, idempotencia, outbox |
| [evaluacion-credito](specs/evaluacion-credito/spec.md) | Scoring PL/SQL, tasa, cuota, amortización, explicación |
| [procesamiento-asincrono](specs/procesamiento-asincrono/spec.md) | SNS/SQS, reintentos, DLQ, concurrencia |
| [integracion-core-bancario](specs/integracion-core-bancario/spec.md) | Contrato SOAP |

Convenciones: Java 17, Spring Boot 3, Oracle 23ai, Angular 19. Español en el dominio y la interfaz.
