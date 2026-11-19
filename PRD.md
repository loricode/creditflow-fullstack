# PRD · CrediFlow

## Problema

Pedir un crédito de consumo suele tomar días: formularios en papel, analistas revisando a mano y respuestas
sin explicación. El banco pierde clientes en el proceso y el cliente rechazado no sabe qué mejorar.

## Objetivo del MVP

Que una persona pida un crédito en línea y reciba **en segundos** una decisión explicada, con tasa, cuota y
tabla de amortización, y que el banco tenga trazabilidad completa de cada evaluación.

| Métrica | Meta del MVP | Medido |
|---|---|---|
| Tiempo de registro a decisión (local) | < 10 s sin fallos | 8 solicitudes en 4 s sin fallos del core; unos 30 s cuando el core simulado falla (cada reintento espera el visibility timeout de 15 s) |
| Solicitudes perdidas ante fallos del core | 0 | Reintentos automáticos + DLQ + reintento manual (`scripts/demo-fallos.sh`) |
| Solicitudes duplicadas por reenvío | 0 | Idempotencia por `externalId` |
| Decisiones con explicación | 100 % | Claude o, como respaldo, reglas |

## Usuarios

- **Solicitante**: quiere saber rápido si le prestan, cuánto pagará y, si no, por qué.
- **Analista de crédito**: revisa el panel, el detalle y el historial; reintenta solicitudes con error.
- **Equipo de TI**: opera la plataforma (DLQ, logs, salud de los servicios) y despliega con Terraform.

## Alcance

**Incluye:** registro con validación, simulador, evaluación automática (score 0–1000, tasa por riesgo, regla
del 40 % de capacidad de pago), amortización, explicación con IA, registro en el core por SOAP, panel con
indicadores, listado con filtros, detalle con historial e intentos, reintento manual y archivo en S3.

**No incluye (siguientes fases):** autenticación y roles, consulta a centrales de riesgo reales, carga de
documentos del solicitante, firma electrónica, notificaciones por correo y despliegue en ECS/RDS.

## Reglas de negocio

| Regla | Valor |
|---|---|
| Monto | $1.000.000 a $2.000.000.000 COP |
| Plazo | 6 a 360 meses (máximo por producto: vivienda 360, educación 120, resto 84 en la interfaz) |
| Tasa base E.A. | Vivienda 12,5 % · Educación 14 % · Vehículo 16,9 % · Libre inversión 22,9 % |
| Ajuste por riesgo | Bajo +0 · Medio +3 · Alto +6 puntos; tope (usura) 28 % |
| Riesgo | Score ≥ 750 bajo · ≥ 550 medio · < 550 alto |
| Aprobación | Score ≥ 550 **y** cuota ≤ 40 % de (ingresos − gastos) |
| Cuota | Sistema francés con tasa mensual equivalente a la E.A. |

## Requisitos no funcionales

- **Consistencia**: patrón outbox; nunca hay solicitud sin evento ni evento sin solicitud.
- **Resiliencia**: 3 intentos por mensaje (SQS + DLQ); la IA nunca bloquea una decisión.
- **Seguridad**: credenciales en Secrets Manager, IAM de mínimo privilegio, S3 privado y cifrado, sin datos
  personales hacia la IA.
- **Trazabilidad**: historial de estados por trigger e intentos con duración y error.
- **Mantenibilidad**: capas verificadas con ArchUnit, migraciones versionadas e infraestructura como código.
