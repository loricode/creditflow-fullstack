-- =====================================================================
-- CrediFlow · V2 · Triggers
-- * Historial de estados: cada cambio de estado queda auditado en la base,
--   sin depender de que la aplicación lo recuerde.
-- * Fecha de actualización mantenida por la base de datos.
-- =====================================================================

CREATE OR REPLACE TRIGGER trg_solicitud_historial
AFTER INSERT OR UPDATE OF estado ON solicitud_credito
FOR EACH ROW
WHEN (NEW.estado IS NOT NULL)
DECLARE
    -- INSERTING/UPDATING son predicados PL/SQL: no se pueden usar dentro del INSERT
    v_anterior solicitud_credito.estado%TYPE;
BEGIN
    IF UPDATING THEN
        v_anterior := :OLD.estado;
    END IF;
    IF INSERTING OR :OLD.estado <> :NEW.estado THEN
        INSERT INTO solicitud_historial (solicitud_id, estado_anterior, estado_nuevo, cambiado_en)
        VALUES (:NEW.id, v_anterior, :NEW.estado, SYSTIMESTAMP);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_solicitud_actualizado
BEFORE UPDATE ON solicitud_credito
FOR EACH ROW
BEGIN
    :NEW.actualizado_en := SYSTIMESTAMP;
END;
/
