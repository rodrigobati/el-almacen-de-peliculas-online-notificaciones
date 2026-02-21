package unrn.notificaciones.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unrn.model.notificaciones.DestinatarioEmail;
import unrn.model.notificaciones.DetalleCompra;
import unrn.model.notificaciones.EnvioEmailCompra;
import unrn.model.notificaciones.EstadoEnvio;
import unrn.model.notificaciones.ItemCompra;
import unrn.model.notificaciones.NotificacionCompraPorEmail;
import unrn.model.notificaciones.TotalCompra;
import unrn.notificaciones.application.port.ProveedorTiempo;
import unrn.notificaciones.application.port.RepositorioEnvioEmailCompra;
import unrn.notificaciones.application.port.ServicioEnvioEmail;
import unrn.notificaciones.event.CompraConfirmadaEvent;

public class ProcesarCompraConfirmada {

    private static final Logger log = LoggerFactory.getLogger(ProcesarCompraConfirmada.class);

    static final String EVENT_TYPE_COMPRA_CONFIRMADA = "CompraConfirmada";
    static final int EVENT_VERSION_SOPORTADA = 1;

    static final String ERROR_EVENTO_OBLIGATORIO = "El evento es obligatorio";
    static final String ERROR_EVENTO_DATA_OBLIGATORIA = "Los datos del evento son obligatorios";
    static final String ERROR_COMPRA_ID_OBLIGATORIO = "El compraId del evento es obligatorio";
    static final String ERROR_EVENT_TYPE_INVALIDO = "El eventType es inválido";
    static final String ERROR_EVENT_VERSION_NO_SOPORTADA = "La eventVersion no es soportada";
    static final String ERROR_ENVIO_EMAIL_FALLIDO = "Falló el envío del email de compra";
    static final String ERROR_MENSAJE_ERROR_DESCONOCIDO = "Error desconocido";

    private final RepositorioEnvioEmailCompra repositorio;
    private final ServicioEnvioEmail servicioEnvioEmail;
    private final ProveedorTiempo reloj;
    private final int maxReintentos;

    public ProcesarCompraConfirmada(
            RepositorioEnvioEmailCompra repositorio,
            ServicioEnvioEmail servicioEnvioEmail,
            ProveedorTiempo reloj,
            int maxReintentos) {
        this.repositorio = repositorio;
        this.servicioEnvioEmail = servicioEnvioEmail;
        this.reloj = reloj;
        this.maxReintentos = maxReintentos <= 0 ? 3 : maxReintentos;
    }

    public void ejecutar(CompraConfirmadaEvent evento) {
        assertEventoCompatible(evento);

        UUID compraId = evento.data().compraId();
        UUID eventId = evento != null ? evento.eventId() : null;
        // start processing log
        log.info("event=CompraConfirmada action=processing-start compraId={} eventId={}", compraId, eventId);

        if (repositorio.existeParaCompra(compraId)) {
            log.info("event=CompraConfirmada action=idempotent-skip compraId={} eventId={}", compraId,
                    evento != null ? evento.eventId() : null);
            return;
        }

        NotificacionCompraPorEmail notificacion = mapearANotificacion(evento);
        EnvioEmailCompra envioInicial = new EnvioEmailCompra(
                UUID.randomUUID(),
                notificacion,
                EstadoEnvio.PENDIENTE,
                0,
                reloj.ahora(),
                null,
                null);

        repositorio.guardar(envioInicial);
        log.info("event=CompraConfirmada action=envio-created compraId={} estado={} reintentos={}",
                compraId, envioInicial.estado(), envioInicial.reintentos());

        try {
            log.info("event=CompraConfirmada action=sending-email compraId={} destinatario={}", compraId,
                    notificacion.destinatario().valor());
            servicioEnvioEmail.enviar(notificacion);
            EnvioEmailCompra envioMarcadoEnviado = envioInicial.marcarEnviado(reloj.ahora());
            repositorio.guardar(envioMarcadoEnviado);
            log.info(
                    "event=CompraConfirmada action=sent compraId={} estado={} reintentos={}",
                    compraId,
                    envioMarcadoEnviado.estado(),
                    envioMarcadoEnviado.reintentos());
        } catch (Exception ex) {
            String error = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? ERROR_MENSAJE_ERROR_DESCONOCIDO
                    : ex.getMessage();
            EnvioEmailCompra envioFallido = envioInicial.marcarFallido(error, maxReintentos);
            repositorio.guardar(envioFallido);
            // Log according to resulting state
            if (envioFallido.estado() == EstadoEnvio.PENDIENTE_REINTENTO) {
                log.warn(
                        "event=CompraConfirmada action=send-failed compraId={} estado={} reintentos={} error={}",
                        compraId,
                        envioFallido.estado(),
                        envioFallido.reintentos(),
                        error);
            } else {
                log.error(
                        "event=CompraConfirmada action=send-failed-permanent compraId={} estado={} reintentos={} error={}",
                        compraId,
                        envioFallido.estado(),
                        envioFallido.reintentos(),
                        error);
            }
            throw new RuntimeException(ERROR_ENVIO_EMAIL_FALLIDO, ex);
        }
    }

    private void assertEventoCompatible(CompraConfirmadaEvent evento) {
        if (evento == null) {
            throw new RuntimeException(ERROR_EVENTO_OBLIGATORIO);
        }
        if (evento.data() == null) {
            throw new RuntimeException(ERROR_EVENTO_DATA_OBLIGATORIA);
        }
        if (evento.data().compraId() == null) {
            throw new RuntimeException(ERROR_COMPRA_ID_OBLIGATORIO);
        }
        if (!EVENT_TYPE_COMPRA_CONFIRMADA.equals(evento.eventType())) {
            throw new RuntimeException(ERROR_EVENT_TYPE_INVALIDO);
        }
        if (evento.eventVersion() != EVENT_VERSION_SOPORTADA) {
            throw new RuntimeException(ERROR_EVENT_VERSION_NO_SOPORTADA);
        }
    }

    private NotificacionCompraPorEmail mapearANotificacion(CompraConfirmadaEvent evento) {
        DestinatarioEmail destinatario = new DestinatarioEmail(evento.data().clienteEmail());
        List<ItemCompra> items = evento.data().items().stream()
                .map(item -> new ItemCompra(item.titulo(), item.cantidad(), item.precioUnitario()))
                .toList();
        TotalCompra total = new TotalCompra(
                evento.data().total().totalBruto(),
                evento.data().total().descuento(),
                evento.data().total().descuentoDescripcion());
        DetalleCompra detalle = new DetalleCompra(items, total);
        Instant fechaConfirmacion = evento.data().fechaConfirmacion();

        return new NotificacionCompraPorEmail(
                evento.data().compraId(),
                destinatario,
                detalle,
                fechaConfirmacion);
    }
}
