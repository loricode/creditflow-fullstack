-- =====================================================================
-- CrediFlow · V5 · Estadísticas por día en hora de Colombia
-- Las fechas se guardan en UTC. Agrupar por fecha UTC ponía en "mañana"
-- las solicitudes creadas después de las 7:00 p. m. hora local.
-- (V4 ya está aplicada: los cambios van en una migración nueva.)
-- =====================================================================

CREATE OR REPLACE PACKAGE BODY pkg_estadisticas AS

    -- Zona de negocio. Va como literal: AT TIME ZONE no admite variables PL/SQL
    -- dentro de un cursor (ORA-02000).
    PROCEDURE resumen(
        o_total           OUT NUMBER,
        o_aprobadas       OUT NUMBER,
        o_rechazadas      OUT NUMBER,
        o_en_proceso      OUT NUMBER,
        o_errores         OUT NUMBER,
        o_monto_aprobado  OUT NUMBER,
        o_score_promedio  OUT NUMBER,
        o_por_producto    OUT SYS_REFCURSOR,
        o_ultimos_dias    OUT SYS_REFCURSOR
    ) IS
        v_hoy DATE := TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Bogota' AS DATE));
    BEGIN
        SELECT COUNT(*),
               NVL(SUM(CASE WHEN estado = 'APROBADA' THEN 1 ELSE 0 END), 0),
               NVL(SUM(CASE WHEN estado = 'RECHAZADA' THEN 1 ELSE 0 END), 0),
               NVL(SUM(CASE WHEN estado IN ('RECIBIDA', 'EN_EVALUACION') THEN 1 ELSE 0 END), 0),
               NVL(SUM(CASE WHEN estado = 'ERROR' THEN 1 ELSE 0 END), 0),
               NVL(SUM(CASE WHEN estado = 'APROBADA' THEN monto END), 0),
               NVL(ROUND(AVG(score)), 0)
          INTO o_total, o_aprobadas, o_rechazadas, o_en_proceso, o_errores, o_monto_aprobado, o_score_promedio
          FROM solicitud_credito;

        OPEN o_por_producto FOR
            SELECT tipo_producto,
                   COUNT(*)                                                   AS total,
                   SUM(CASE WHEN estado = 'APROBADA' THEN 1 ELSE 0 END)       AS aprobadas,
                   NVL(SUM(CASE WHEN estado = 'APROBADA' THEN monto END), 0)  AS monto_aprobado
              FROM solicitud_credito
             GROUP BY tipo_producto
             ORDER BY total DESC;

        OPEN o_ultimos_dias FOR
            WITH dias AS (
                SELECT v_hoy - (LEVEL - 1) AS dia
                  FROM dual
               CONNECT BY LEVEL <= 7
            ),
            locales AS (
                SELECT id, estado, CAST(creado_en AT TIME ZONE 'America/Bogota' AS DATE) AS creado_local
                  FROM solicitud_credito
                 WHERE creado_en >= SYSTIMESTAMP - INTERVAL '8' DAY
            )
            SELECT d.dia,
                   COUNT(l.id) AS total,
                   NVL(SUM(CASE WHEN l.estado = 'APROBADA' THEN 1 ELSE 0 END), 0) AS aprobadas
              FROM dias d
              LEFT JOIN locales l
                ON l.creado_local >= d.dia
               AND l.creado_local <  d.dia + 1
             GROUP BY d.dia
             ORDER BY d.dia;
    END resumen;

END pkg_estadisticas;
/
