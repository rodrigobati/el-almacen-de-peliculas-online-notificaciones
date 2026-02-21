package unrn.model.notificaciones;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class NotificacionCompraPorEmail {

    static final String ERROR_COMPRA_ID_INVALIDO = "El id de compra es obligatorio";
    static final String ERROR_DESTINATARIO_INVALIDO = "El destinatario es obligatorio";
    static final String ERROR_DETALLE_INVALIDO = "El detalle de compra es obligatorio";
    static final String ERROR_FECHA_INVALIDA = "La fecha de confirmación es obligatoria";

    private final UUID compraId;
    private final DestinatarioEmail destinatario;
    private final DetalleCompra detalle;
    private final Instant fechaConfirmacion;

    public NotificacionCompraPorEmail(
            UUID compraId,
            DestinatarioEmail destinatario,
            DetalleCompra detalle,
            Instant fechaConfirmacion) {
        this.compraId = compraId;
        this.destinatario = destinatario;
        this.detalle = detalle;
        this.fechaConfirmacion = fechaConfirmacion;

        assertCompraId();
        assertDestinatario();
        assertDetalle();
        assertFechaConfirmacion();
    }

    public UUID compraId() {
        return compraId;
    }

    public DestinatarioEmail destinatario() {
        return destinatario;
    }

    public DetalleCompra detalle() {
        return detalle;
    }

    public Instant fechaConfirmacion() {
        return fechaConfirmacion;
    }

    private void assertCompraId() {
        if (compraId == null)
            throw new RuntimeException(ERROR_COMPRA_ID_INVALIDO);
    }

    private void assertDestinatario() {
        if (destinatario == null)
            throw new RuntimeException(ERROR_DESTINATARIO_INVALIDO);
    }

    private void assertDetalle() {
        if (detalle == null)
            throw new RuntimeException(ERROR_DETALLE_INVALIDO);
    }

    private void assertFechaConfirmacion() {
        if (fechaConfirmacion == null)
            throw new RuntimeException(ERROR_FECHA_INVALIDA);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NotificacionCompraPorEmail other))
            return false;
        return Objects.equals(compraId, other.compraId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(compraId);
    }
}