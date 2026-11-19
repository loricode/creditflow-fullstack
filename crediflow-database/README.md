# crediflow-database

Esquema Oracle, lógica PL/SQL y capa de persistencia compartida por la API, el publisher y el worker.
Funciona como un *starter*: al agregarlo como dependencia se registran entidades, repositorios y gateways.

## Migraciones (Flyway)

| Versión | Contenido |
|---|---|
| V1 | Tablas `solicitud_credito`, `amortizacion`, `outbox_evento`, `solicitud_historial`, `intento_evaluacion`; checks e índices |
| V2 | Triggers: historial de estados y fecha de actualización |
| V3 | `PKG_CREDITO`: tasa base, tasa mensual, cuota, score, riesgo, amortización (FORALL) y evaluación |
| V4 | `PKG_ESTADISTICAS.RESUMEN`: escalares OUT y dos `SYS_REFCURSOR` |
| V5 | Estadísticas agrupadas en hora de Colombia |

La API aplica las migraciones al arrancar. El publisher y el worker tienen Flyway desactivado.

## PKG_CREDITO

```
score = 1000
      − endeudamiento (gastos/ingresos) × 400
      − carga de la cuota sobre la capacidad de pago (0 / 80 / 200 / 350; −500 sin capacidad)
      − monto frente a ingreso anual (> 3 veces: −60, > 5 veces: −120)
      − plazo > 120 meses fuera de vivienda (−60)
      + garantía o finalidad (vivienda +60, vehículo +30, educación +20)
```

Se aprueba si `score ≥ 550` y `cuota ≤ 40 %` de la capacidad de pago.

## Desde Java

`CreditoPlsqlGateway` y `EstadisticasPlsqlGateway` usan `CallableStatement` explícitos, sin `SimpleJdbcCall`,
para que el contrato con cada procedimiento quede a la vista y no dependa de leer el diccionario de datos.

## Pruebas

`PaquetesPlsqlIT` levanta `gvenzl/oracle-free` con Testcontainers, aplica las migraciones y prueba los paquetes
y triggers. También verifica que no queden objetos inválidos: Flyway no falla si un paquete compila con errores.
