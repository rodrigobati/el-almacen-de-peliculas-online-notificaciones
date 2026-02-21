package unrn.model.notificaciones;

import java.time.Instant;
import java.util.UUID;

public final class EnvioEmailCompra {

    static final String ERROR_ID_ENVIO_OBLIGATORIO = "El id de envío es obligatorio";
    static final String ERROR_NOTIFICACION_OBLIGATORIA = "La notificación es obligatoria";
    static final String ERROR_ESTADO_OBLIGATORIO = "El estado es obligatorio";
    static final String ERROR_FECHA_CREACION_OBLIGATORIA = "La fecha de creación es obligatoria";
    static final String ERROR_REINTENTOS_NEGATIVOS = "Los reintentos no pueden ser negativos";

    private final UUID envioId;
    private final NotificacionCompraPorEmail notificacion;
    private final EstadoEnvio estado;
    private final int reintentos;
    private final Instant creadoEn;
    private final Instant enviadoEn; // puede ser null
    private final String ultimoError; // puede ser null

    public EnvioEmailCompra(
            UUID envioId,
            NotificacionCompraPorEmail notificacion,
            EstadoEnvio estado,
            int reintentos,
            Instant creadoEn,
            Instant enviadoEn,
            String ultimoError) {
        this.envioId = envioId;
        this.notificacion = notificacion;
        this.estado = estado;
        this.reintentos = reintentos;
        this.creadoEn = creadoEn;
        this.enviadoEn = enviadoEn;
        this.ultimoError = ultimoError;

        assertEnvioId();
        assertNotificacion();
        assertEstado();
        assertReintentos();
        assertCreadoEn();
    }

    public UUID envioId() {
        return envioId;
    }

    public UUID compraId() {
        return notificacion.compraId();
    }

    public boolean correspondeACompra(UUID compraId) {
        return notificacion.compraId().equals(compraId);
    }

    public EnvioEmailCompra marcarEnviado(Instant cuando) {
        return new EnvioEmailCompra(
                envioId,
                notificacion,
                EstadoEnvio.ENVIADO,
                reintentos,
                creadoEn,
                cuando,
                null);
    }

    public EnvioEmailCompra marcarFallido(String error, int maxReintentos) {
        var nuevosReintentos = reintentos + 1;
        var nuevoEstado = (nuevosReintentos >= maxReintentos) ? EstadoEnvio.FALLO_PERMANENTE
                : EstadoEnvio.PENDIENTE_REINTENTO;

        return new EnvioEmailCompra(
                envioId,
                notificacion,
                nuevoEstado,
                nuevosReintentos,
                creadoEn,
                enviadoEn,
                error);
    }

    public EstadoEnvio estado() {
        return estado;
    }

    public int reintentos() {
        return reintentos;
    }

    public Instant enviadoEn() {
        return enviadoEn;
    }

    public String ultimoError() {
        return ultimoError;
    }

    private void assertEnvioId() {
        if (envioId == null)
            throw new RuntimeException(ERROR_ID_ENVIO_OBLIGATORIO);
    }

    private void assertNotificacion() {
        if (notificacion == null)
            throw new RuntimeException(ERROR_NOTIFICACION_OBLIGATORIA);
    }

    private void assertEstado() {
        if (estado == null)
            throw new RuntimeException(ERROR_ESTADO_OBLIGATORIO);
    }

    private void assertReintentos() {
        if (reintentos < 0)
            throw new RuntimeException(ERROR_REINTENTOS_NEGATIVOS);
    }

    private void assertCreadoEn() {
        if (creadoEn == null)
            throw new RuntimeException(ERROR_FECHA_CREACION_OBLIGATORIA);
    }
}