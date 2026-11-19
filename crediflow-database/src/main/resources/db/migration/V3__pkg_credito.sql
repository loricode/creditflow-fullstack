-- =====================================================================
-- CrediFlow · V3 · PKG_CREDITO
-- Motor de evaluación: scoring, tasa por riesgo, cuota (sistema francés)
-- y tabla de amortización generada con BULK (FORALL).
-- La decisión vive en la base de datos: una sola fuente de verdad para
-- la API (simulador) y el worker (evaluación).
-- =====================================================================

CREATE OR REPLACE PACKAGE pkg_credito AS

    c_score_minimo     CONSTANT PLS_INTEGER := 550;   -- por debajo se rechaza
    c_max_carga_cuota  CONSTANT NUMBER      := 0.40;  -- la cuota no puede superar el 40 % de la capacidad de pago
    c_tasa_usura       CONSTANT NUMBER      := 28.0;  -- techo legal de la tasa EA (%)

    e_solicitud_no_existe EXCEPTION;
    PRAGMA EXCEPTION_INIT(e_solicitud_no_existe, -20001);
    e_estado_invalido EXCEPTION;
    PRAGMA EXCEPTION_INIT(e_estado_invalido, -20002);
    e_producto_invalido EXCEPTION;
    PRAGMA EXCEPTION_INIT(e_producto_invalido, -20003);

    -- Tasa efectiva anual base del producto (%)
    FUNCTION tasa_base(p_producto IN VARCHAR2) RETURN NUMBER DETERMINISTIC;

    -- Conversión EA -> tasa mensual equivalente (fracción)
    FUNCTION tasa_mensual(p_tasa_ea IN NUMBER) RETURN NUMBER DETERMINISTIC;

    -- Cuota fija mensual (sistema francés)
    FUNCTION calcular_cuota(p_monto IN NUMBER, p_tasa_ea IN NUMBER, p_plazo IN NUMBER) RETURN NUMBER DETERMINISTIC;

    -- Puntaje 0..1000 a partir de capacidad de pago, endeudamiento y producto
    FUNCTION calcular_score(
        p_ingresos IN NUMBER,
        p_gastos   IN NUMBER,
        p_monto    IN NUMBER,
        p_plazo    IN NUMBER,
        p_producto IN VARCHAR2
    ) RETURN NUMBER;

    FUNCTION nivel_riesgo(p_score IN NUMBER) RETURN VARCHAR2 DETERMINISTIC;

    PROCEDURE generar_amortizacion(
        p_solicitud_id IN NUMBER,
        p_monto        IN NUMBER,
        p_tasa_ea      IN NUMBER,
        p_plazo        IN NUMBER,
        p_fecha_inicio IN DATE
    );

    -- Evalúa una solicitud EN_EVALUACION: guarda score, riesgo, tasa, cuota y
    -- amortización, y devuelve la decisión. No hace COMMIT: la transacción es
    -- del llamador. El estado final lo fija el worker tras el core bancario.
    PROCEDURE evaluar_solicitud(
        p_solicitud_id IN  NUMBER,
        o_decision     OUT VARCHAR2,
        o_score        OUT NUMBER,
        o_riesgo       OUT VARCHAR2,
        o_tasa         OUT NUMBER,
        o_cuota        OUT NUMBER
    );

END pkg_credito;
/

CREATE OR REPLACE PACKAGE BODY pkg_credito AS

    FUNCTION tasa_base(p_producto IN VARCHAR2) RETURN NUMBER DETERMINISTIC IS
    BEGIN
        RETURN CASE p_producto
                   WHEN 'VIVIENDA'        THEN 12.5
                   WHEN 'EDUCACION'       THEN 14.0
                   WHEN 'VEHICULO'        THEN 16.9
                   WHEN 'LIBRE_INVERSION' THEN 22.9
               END;
    END tasa_base;

    FUNCTION tasa_mensual(p_tasa_ea IN NUMBER) RETURN NUMBER DETERMINISTIC IS
    BEGIN
        RETURN POWER(1 + p_tasa_ea / 100, 1 / 12) - 1;
    END tasa_mensual;

    FUNCTION calcular_cuota(p_monto IN NUMBER, p_tasa_ea IN NUMBER, p_plazo IN NUMBER) RETURN NUMBER DETERMINISTIC IS
        v_i NUMBER := tasa_mensual(p_tasa_ea);
    BEGIN
        IF v_i = 0 THEN
            RETURN ROUND(p_monto / p_plazo, 2);
        END IF;
        RETURN ROUND(p_monto * v_i / (1 - POWER(1 + v_i, -p_plazo)), 2);
    END calcular_cuota;

    FUNCTION calcular_score(
        p_ingresos IN NUMBER,
        p_gastos   IN NUMBER,
        p_monto    IN NUMBER,
        p_plazo    IN NUMBER,
        p_producto IN VARCHAR2
    ) RETURN NUMBER IS
        v_score          NUMBER := 1000;
        v_capacidad      NUMBER := p_ingresos - p_gastos;
        v_endeudamiento  NUMBER := p_gastos / p_ingresos;
        v_cuota          NUMBER;
        v_carga          NUMBER;
        v_veces_ingreso  NUMBER := p_monto / (p_ingresos * 12);
        v_tasa           NUMBER := tasa_base(p_producto);
    BEGIN
        IF v_tasa IS NULL THEN
            RAISE_APPLICATION_ERROR(-20003, 'Producto no soportado: ' || p_producto);
        END IF;

        -- 1. Endeudamiento actual (hasta -400)
        v_score := v_score - LEAST(v_endeudamiento, 1) * 400;

        -- 2. Carga de la nueva cuota sobre la capacidad de pago
        IF v_capacidad <= 0 THEN
            v_score := v_score - 500;
        ELSE
            v_cuota := calcular_cuota(p_monto, v_tasa, p_plazo);
            v_carga := v_cuota / v_capacidad;
            v_score := v_score - CASE
                                     WHEN v_carga > 0.60 THEN 350
                                     WHEN v_carga > 0.40 THEN 200
                                     WHEN v_carga > 0.25 THEN 80
                                     ELSE 0
                                 END;
        END IF;

        -- 3. Monto frente a ingreso anual
        v_score := v_score - CASE
                                 WHEN v_veces_ingreso > 5 THEN 120
                                 WHEN v_veces_ingreso > 3 THEN 60
                                 ELSE 0
                             END;

        -- 4. Plazos largos fuera de vivienda
        IF p_plazo > 120 AND p_producto <> 'VIVIENDA' THEN
            v_score := v_score - 60;
        END IF;

        -- 5. Productos con garantía real o finalidad productiva
        v_score := v_score + CASE p_producto
                                 WHEN 'VIVIENDA'  THEN 60
                                 WHEN 'VEHICULO'  THEN 30
                                 WHEN 'EDUCACION' THEN 20
                                 ELSE 0
                             END;

        RETURN GREATEST(0, LEAST(1000, ROUND(v_score)));
    END calcular_score;

    FUNCTION nivel_riesgo(p_score IN NUMBER) RETURN VARCHAR2 DETERMINISTIC IS
    BEGIN
        RETURN CASE
                   WHEN p_score >= 750 THEN 'BAJO'
                   WHEN p_score >= c_score_minimo THEN 'MEDIO'
                   ELSE 'ALTO'
               END;
    END nivel_riesgo;

    PROCEDURE generar_amortizacion(
        p_solicitud_id IN NUMBER,
        p_monto        IN NUMBER,
        p_tasa_ea      IN NUMBER,
        p_plazo        IN NUMBER,
        p_fecha_inicio IN DATE
    ) IS
        TYPE t_cuotas IS TABLE OF amortizacion%ROWTYPE INDEX BY PLS_INTEGER;
        v_cuotas   t_cuotas;
        v_i        NUMBER := tasa_mensual(p_tasa_ea);
        v_cuota    NUMBER := calcular_cuota(p_monto, p_tasa_ea, p_plazo);
        v_saldo    NUMBER := p_monto;
        v_interes  NUMBER;
        v_capital  NUMBER;
    BEGIN
        FOR k IN 1 .. p_plazo LOOP
            v_interes := ROUND(v_saldo * v_i, 2);
            -- La última cuota absorbe el redondeo acumulado
            v_capital := CASE WHEN k = p_plazo THEN v_saldo ELSE v_cuota - v_interes END;
            v_saldo   := v_saldo - v_capital;

            v_cuotas(k).solicitud_id := p_solicitud_id;
            v_cuotas(k).numero_cuota := k;
            v_cuotas(k).fecha_pago   := ADD_MONTHS(p_fecha_inicio, k);
            v_cuotas(k).cuota        := v_capital + v_interes;
            v_cuotas(k).capital      := v_capital;
            v_cuotas(k).interes      := v_interes;
            v_cuotas(k).saldo        := GREATEST(v_saldo, 0);
        END LOOP;

        FORALL k IN 1 .. v_cuotas.COUNT
            INSERT INTO amortizacion (solicitud_id, numero_cuota, fecha_pago, cuota, capital, interes, saldo)
            VALUES (v_cuotas(k).solicitud_id, v_cuotas(k).numero_cuota, v_cuotas(k).fecha_pago,
                    v_cuotas(k).cuota, v_cuotas(k).capital, v_cuotas(k).interes, v_cuotas(k).saldo);
    END generar_amortizacion;

    PROCEDURE evaluar_solicitud(
        p_solicitud_id IN  NUMBER,
        o_decision     OUT VARCHAR2,
        o_score        OUT NUMBER,
        o_riesgo       OUT VARCHAR2,
        o_tasa         OUT NUMBER,
        o_cuota        OUT NUMBER
    ) IS
        v_sol       solicitud_credito%ROWTYPE;
        v_capacidad NUMBER;
    BEGIN
        BEGIN
            SELECT * INTO v_sol
              FROM solicitud_credito
             WHERE id = p_solicitud_id
               FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20001, 'La solicitud ' || p_solicitud_id || ' no existe');
        END;

        IF v_sol.estado <> 'EN_EVALUACION' THEN
            RAISE_APPLICATION_ERROR(-20002,
                'La solicitud ' || p_solicitud_id || ' está en estado ' || v_sol.estado || ', se esperaba EN_EVALUACION');
        END IF;

        o_score  := calcular_score(v_sol.ingresos_mensuales, v_sol.gastos_mensuales,
                                   v_sol.monto, v_sol.plazo_meses, v_sol.tipo_producto);
        o_riesgo := nivel_riesgo(o_score);
        o_tasa   := LEAST(tasa_base(v_sol.tipo_producto)
                          + CASE o_riesgo WHEN 'BAJO' THEN 0 WHEN 'MEDIO' THEN 3 ELSE 6 END,
                          c_tasa_usura);
        o_cuota  := calcular_cuota(v_sol.monto, o_tasa, v_sol.plazo_meses);

        v_capacidad := v_sol.ingresos_mensuales - v_sol.gastos_mensuales;
        IF o_score >= c_score_minimo AND o_cuota <= v_capacidad * c_max_carga_cuota THEN
            o_decision := 'APROBADA';
        ELSE
            o_decision := 'RECHAZADA';
        END IF;

        -- Idempotente: un reintento recalcula desde cero
        DELETE FROM amortizacion WHERE solicitud_id = p_solicitud_id;
        IF o_decision = 'APROBADA' THEN
            generar_amortizacion(p_solicitud_id, v_sol.monto, o_tasa, v_sol.plazo_meses, TRUNC(SYSDATE));
        END IF;

        UPDATE solicitud_credito
           SET score         = o_score,
               nivel_riesgo  = o_riesgo,
               tasa_anual    = o_tasa,
               cuota_mensual = o_cuota,
               version       = version + 1
         WHERE id = p_solicitud_id;
    END evaluar_solicitud;

END pkg_credito;
/
