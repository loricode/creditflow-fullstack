-- =====================================================================
-- CrediFlow · V4 · PKG_ESTADISTICAS
-- Indicadores del panel en una sola ida a la base: escalares por OUT y
-- desgloses por SYS_REFCURSOR.
-- =====================================================================

CREATE OR REPLACE PACKAGE pkg_estadisticas AS

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
    );

END pkg_estadisticas;
/

CREATE OR REPLACE PACKAGE BODY pkg_estadisticas AS

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

        -- Serie completa de 7 días (incluye días sin solicitudes)
        OPEN o_ultimos_dias FOR
            WITH dias AS (
                SELECT TRUNC(SYSDATE) - (LEVEL - 1) AS dia
                  FROM dual
               CONNECT BY LEVEL <= 7
            )
            SELECT d.dia,
                   COUNT(s.id) AS total,
                   NVL(SUM(CASE WHEN s.estado = 'APROBADA' THEN 1 ELSE 0 END), 0) AS aprobadas
              FROM dias d
              LEFT JOIN solicitud_credito s
                ON CAST(s.creado_en AS DATE) >= d.dia
               AND CAST(s.creado_en AS DATE) <  d.dia + 1
             GROUP BY d.dia
             ORDER BY d.dia;
    END resumen;

END pkg_estadisticas;
/
