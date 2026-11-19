# CrediFlow

Plataforma de **originación de créditos**: un cliente pide un crédito y en segundos recibe la decisión,
la tasa, la cuota, la tabla de amortización y una explicación en lenguaje claro.

**Java 17 · Spring Boot 3 · Oracle PL/SQL · Angular 19 · AWS (SNS, SQS, S3, Secrets Manager, IAM) · Terraform · SOAP · Claude**

```
 Angular ──REST──▶ API ──┬─ INSERT solicitud ─┐  una transacción
 (nginx)                 └─ INSERT outbox ────┘  (Oracle)
                                  │
                       Publisher ─┘ lee outbox ──▶ SNS ──▶ SQS ──(3 fallos)──▶ DLQ
                                                            │
                                                         Worker
                              ┌───────────────┬─────────────┼──────────────┬────────────────┐
                              ▼               ▼             ▼              ▼                ▼
                         S3 (original)  PKG_CREDITO     Claude (IA)   Core bancario    Oracle: estado,
                         cifrado,       score, tasa,    explicación   SOAP (WSDL)      historial (trigger),
                         particionado   cuota, FORALL   + respaldo    referencia       intentos
```

🎬 **Video del camino feliz (4:35, narrado):** [`docs/presentacion/videos/01-camino-feliz.mp4`](docs/presentacion/videos/01-camino-feliz.mp4)
· [cómo se graba](docs/video/grabador/LEEME.md)

## Cómo cubre los requisitos de la vacante

| Requisito | Dónde está |
|---|---|
| **Java** | `backend/`: Maven multimódulo (API, publisher, worker, core-bancario, shared), Spring Boot 3, JPA, MapStruct, Bean Validation, `ProblemDetail` (RFC 9457), pruebas con JUnit 5, Mockito y ArchUnit |
| **PL/SQL** | `crediflow-database/`: paquetes `PKG_CREDITO` (scoring, tasa por riesgo, cuota del sistema francés, amortización con `FORALL`, `RAISE_APPLICATION_ERROR`, `SELECT … FOR UPDATE`) y `PKG_ESTADISTICAS` (`SYS_REFCURSOR`), triggers de auditoría, migraciones Flyway versionadas, pruebas contra Oracle real con Testcontainers |
| **Angular** | `frontend/`: Angular 19 con componentes standalone, signals, carga diferida (lazy loading), formularios reactivos, interceptor HTTP, Angular Material con tema propio, animaciones y modales |
| **AWS** | SNS → SQS con DLQ y redrive policy, S3 cifrado y versionado, Secrets Manager (credenciales leídas al arrancar), IAM con un rol de mínimo privilegio por servicio |
| **IaC** | `infrastructure/terraform/`: módulos reutilizables (`almacenamiento`, `mensajeria`, `secretos`, `iam`) y entornos `local` (LocalStack) y `produccion` |
| **Git** | Conventional Commits (convención en `CLAUDE.md`) y CI en GitHub Actions (`.github/workflows/ci.yml`): backend con Oracle real, frontend, `terraform fmt/validate` e imágenes Docker |
| **REST y SOAP** | REST documentado con OpenAPI/Swagger. SOAP *contract-first*: XSD → WSDL publicado por el core bancario simulado; el worker lo consume con `WebServiceTemplate` y JAXB |
| **IA en el desarrollo** | En el producto: el worker explica cada decisión con Claude (SDK oficial `anthropic-java`), con respaldo por reglas y sin enviar datos personales. En el proceso: [`CLAUDE.md`](CLAUDE.md) y specs en [`openspec/`](openspec/project.md) |

## Puesta en marcha

Requisitos: **Docker Desktop** abierto, con unos 6 GB de RAM disponibles (Oracle Free usa alrededor de 2 GB).

```powershell
Copy-Item .env.example .env        # opcional: pon ANTHROPIC_API_KEY para explicaciones con Claude
docker compose up -d --build       # el primer arranque tarda entre 3 y 5 minutos (Oracle + compilación)
docker compose ps                  # todo "healthy"; terraform aparece como "Exited (0)"
start http://localhost:3100
```

| Servicio | URL |
|---|---|
| Aplicación | http://localhost:3100 |
| Swagger (REST) | http://localhost:8088/swagger-ui.html |
| WSDL del core bancario | http://localhost:8090/ws/core-bancario.wsdl |
| Oracle (SQL Developer, DBeaver) | `localhost:1522/FREEPDB1` · usuario `crediflow` |

Los puertos se cambian en `.env` si chocan con otros proyectos.

### Demos guiadas

```bash
bash scripts/demo-flujo.sh     # 8 solicitudes con perfiles distintos, evaluadas en segundos
bash scripts/demo-fallos.sh    # el core falla: 3 reintentos -> ERROR -> DLQ -> «Reintentar» -> APROBADA
```

## Qué demuestra cada parte

**Consistencia sin transacciones distribuidas (patrón outbox).** La API guarda la solicitud y su evento en
la misma transacción de Oracle. Un proceso aparte (`crediflow-publisher`) publica en SNS con
`FOR UPDATE SKIP LOCKED`, de modo que se puede escalar a varias réplicas sin publicar dos veces el mismo evento.

**Idempotencia en cada salto.** Hay idempotencia en cuatro puntos:
- `externalId`: reenviar la misma solicitud devuelve 200 con la ya creada.
- El worker toma la solicitud con bloqueo de fila y descarta mensajes duplicados.
- La amortización se recalcula desde cero en cada intento.
- El core bancario devuelve la misma referencia ante un reintento.

**Fallos visibles y recuperables.** Un fallo recuperable (SOAP Fault, timeout, S3) deja el mensaje en SQS y
la solicitud vuelve a `RECIBIDA`. El siguiente intento llega cuando vence el visibility timeout. Al tercer
fallo la solicitud pasa a `ERROR` y el mensaje va a la DLQ. Un error de negocio de PL/SQL (`ORA-20xxx`) no
se reintenta. Cada transición queda auditada por el trigger `TRG_SOLICITUD_HISTORIAL`.

**La regla de negocio vive en un solo lugar.** El simulador de la API y la evaluación del worker usan el
mismo `PKG_CREDITO`.

**La IA aporta valor sin ser un punto de falla.** El worker solo envía a Claude las cifras de la decisión:
nunca el nombre, el documento ni el correo. Si no hay clave, falla la llamada o el modelo se niega a
responder, se usa una explicación generada por reglas. La pantalla indica cuál de las dos se usó.

## Estructura

```
├── backend/                       Maven multimódulo (mvnw incluido)
│   ├── crediflow-shared/          clientes AWS, Secrets Manager → DataSource, eventos, contrato SOAP (JAXB)
│   ├── crediflow-api/             REST: web → service → repository, MapStruct, ProblemDetail, OpenAPI
│   ├── crediflow-publisher/       outbox → SNS
│   ├── crediflow-worker/          SQS → S3 → PL/SQL → IA → SOAP
│   └── crediflow-core-bancario/   mock SOAP del core (XSD → WSDL)
├── crediflow-database/            Flyway V1..V5, PL/SQL, entidades JPA, gateways JDBC, pruebas con Oracle real
├── frontend/                      Angular: core/ (servicios, interceptor) · shared/ · features/ (lazy)
├── infrastructure/
│   ├── terraform/                 modules/ · envs/local · envs/produccion
│   └── docker/                    Dockerfiles multi-stage y nginx
├── openspec/                      specs de comportamiento
├── scripts/                       demos y run_all_tests.ps1
├── PRD.md · HISTORIAS_DE_USUARIO.md · CLAUDE.md
└── docker-compose.yml
```

## Pruebas

| Suite | Comando | Cantidad |
|---|---|---|
| Unitarias del backend (dominio, servicios, controladores, mapper, arquitectura, outbox, worker, SOAP) | `cd backend; .\mvnw.cmd test` | 45 |
| Integración PL/SQL contra Oracle Free (Testcontainers) | `cd backend; .\mvnw.cmd verify` | 8 |
| Frontend (pipes, componentes, servicio HTTP, interceptor) | `cd frontend; npm run test:ci` | 11 |
| Todas | `.\scripts\run_all_tests.ps1` | 64 |

La suite de integración incluye una prueba que falla si queda algún paquete o trigger inválido en Oracle.
Flyway no detecta ese caso por sí solo.

## Desarrollo local sin Docker para la app

```powershell
docker compose up -d oracle localstack terraform core-bancario   # solo la infraestructura
cd backend; .\mvnw.cmd -pl crediflow-api -am spring-boot:run      # API en :8080 (con las variables de .env)
cd frontend; npm start                                            # http://localhost:4200, proxy a la API
```

## Producción

`infrastructure/terraform/envs/produccion` crea los mismos recursos en AWS real, con estado remoto en S3 y
una ventana de recuperación de secretos de 30 días. El siguiente paso natural es agregar VPC, Amazon RDS
for Oracle y ECS Fargate (un servicio por módulo) usando los roles IAM que ya genera el módulo `iam`.
