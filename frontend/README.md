# CrediFlow · Frontend

Angular 19 con componentes standalone, signals y Angular Material (Material 3) con un tema verde azulado propio.

```
src/app/
├── core/        modelos, catálogos, servicios HTTP, interceptor de errores, notificaciones
├── shared/      componentes (estado, KPI, vacío), directiva de contador animado, pipe COP, animaciones
└── features/    pantallas con carga diferida (lazy loading)
    ├── panel/            KPIs, gráfico de 7 días, desglose por producto, actividad reciente (refresco cada 5 s)
    ├── solicitudes/      filtros, búsqueda con debounce, orden y paginación en servidor, refresco en vivo
    ├── detalle/          recorrido por etapas, medidor de score, explicación IA, historial, intentos
    │                     + modales de amortización y de original en S3
    ├── nueva-solicitud/  asistente de 3 pasos con simulación en vivo y errores del servidor por campo
    └── simulador/        cálculo de cuota con PL/SQL
```

```bash
npm start            # http://localhost:4200 (proxy /api -> http://localhost:8088)
npm run test:ci      # pruebas en ChromeHeadless
npx ng build         # producción (dist/frontend/browser)
```

Diseño: los colores salen de los tokens `--cf-*` de `src/styles.scss`. Las animaciones respetan
`prefers-reduced-motion`.
