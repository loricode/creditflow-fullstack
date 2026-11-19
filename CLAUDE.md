# CLAUDE.md

Guía para asistentes de IA (y para personas) que trabajan en este repositorio.

## Qué es

CrediFlow: originación de créditos. La API registra la solicitud y su evento en el outbox; el publisher lo
envía a SNS; el worker consume de SQS, archiva en S3, evalúa con PL/SQL, explica con Claude y registra en el
core bancario por SOAP. Ver `README.md` para el diagrama.

## Comandos

```powershell
cd backend; .\mvnw.cmd test                 # unitarias (rápidas, sin Docker)
cd backend; .\mvnw.cmd verify               # + integración PL/SQL con Testcontainers (requiere Docker)
cd frontend; npm run test:ci                # pruebas Angular (ChromeHeadless)
cd frontend; npx ng build                   # build de producción
docker compose up -d --build                # sistema completo -> http://localhost:3100
bash scripts/demo-flujo.sh                  # verificación de punta a punta
```

## Reglas del proyecto

- **Migraciones**: nunca edites una migración que ya se aplicó en un entorno compartido; crea `V<n+1>__descripcion.sql`.
  Los bloques PL/SQL terminan con `/` en su propia línea.
- **PL/SQL**: los procedimientos no hacen `COMMIT`; la transacción es del llamador (Java).
  Los errores de negocio usan `RAISE_APPLICATION_ERROR(-20xxx, ...)`; el worker no los reintenta.
  `AT TIME ZONE` dentro de un cursor no acepta variables PL/SQL: usa un literal.
- **Capas de la API** (las verifica `ArquitecturaTest`): `web` → `service` → repositorios/gateways.
  Los controladores no usan repositorios ni gateways PL/SQL; los servicios no conocen HTTP.
- **Transacciones**: un método `@Transactional` llamado desde la misma clase no abre transacción
  (no pasa por el proxy). Usa otro bean o `TransactionTemplate`, como en `OutboxPublisher`.
- **Llamadas externas** (S3, SOAP, IA) fuera de transacciones de base de datos.
- **IA**: solo se envían a Claude cifras de la decisión, nunca datos personales. Cualquier fallo cae a
  `ExplicadorReglas`. Modelo por defecto: `claude-opus-5-5` (propiedad `crediflow.ia.modelo`).
- **Idempotencia**: cada paso del worker debe poder repetirse sin efectos duplicados.
- **Frontend**: componentes standalone, signals e `inject()`; colores solo con los tokens `--cf-*` de `styles.scss`.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`).

## Specs

Los comportamientos esperados están en `openspec/specs/`. Si un cambio altera un comportamiento, actualiza
la spec correspondiente en el mismo commit.
