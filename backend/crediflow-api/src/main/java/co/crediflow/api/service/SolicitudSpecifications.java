package co.crediflow.api.service;

import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.model.TipoProducto;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Filtros combinables del listado. Un filtro nulo no restringe nada. */
final class SolicitudSpecifications {

    private SolicitudSpecifications() {
    }

    static Specification<SolicitudCredito> conEstado(EstadoSolicitud estado) {
        return (root, query, cb) -> estado == null ? null : cb.equal(root.get("estado"), estado);
    }

    static Specification<SolicitudCredito> conProducto(TipoProducto producto) {
        return (root, query, cb) -> producto == null ? null : cb.equal(root.get("tipoProducto"), producto);
    }

    /** Busca por nombre (sin distinguir mayúsculas), documento o clave externa. */
    static Specification<SolicitudCredito> contieneTexto(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) {
                return null;
            }
            String patron = "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nombreCompleto")), patron),
                    cb.like(root.get("numeroDocumento"), patron),
                    cb.like(cb.lower(root.get("externalId")), patron));
        };
    }
}
