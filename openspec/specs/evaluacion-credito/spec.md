# Evaluación de crédito

## Requisito: decisión calculada en la base de datos
`PKG_CREDITO.EVALUAR_SOLICITUD` DEBE calcular score, nivel de riesgo, tasa y cuota, y generar la amortización
si la solicitud se aprueba. No hace COMMIT.

### Escenario: perfil solvente
- **Dado** ingresos de 8.000.000, gastos de 3.000.000, libre inversión por 30.000.000 a 48 meses
- **Entonces** el score es 850, el riesgo BAJO, la tasa 22,9 % E.A. y la decisión APROBADA
- **Y** hay 48 cuotas, la suma del capital es 30.000.000 y el saldo final es 0

### Escenario: sobreendeudado
- **Dado** ingresos de 3.000.000, gastos de 2.500.000, libre inversión por 50.000.000 a 36 meses
- **Entonces** el riesgo es ALTO, la decisión RECHAZADA y no se genera amortización

### Escenario: estado incorrecto
- **Dado** una solicitud que no está EN_EVALUACION
- **Entonces** el procedimiento lanza `ORA-20002` y el worker no reintenta

### Escenario: reevaluación
- **Cuando** se evalúa dos veces la misma solicitud
- **Entonces** la amortización no se duplica

## Requisito: explicación en lenguaje claro
Toda decisión DEBE tener explicación. Primero se intenta con Claude; si no hay clave, falla o se niega, se usan reglas.

### Escenario: privacidad
- **Entonces** el mensaje enviado a la IA no contiene nombre, documento ni correo

### Escenario: respaldo
- **Dado** que la IA responde con error o con `stop_reason = refusal`
- **Entonces** `proveedorIa = "reglas"` y la evaluación continúa

## Requisito: estadísticas en hora local
La serie de los últimos 7 días DEBE agruparse por fecha de `America/Bogota`.
