package unrn.notificaciones.adapter.in.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import unrn.notificaciones.application.ProcesarCompraConfirmada;
import unrn.notificaciones.event.CompraConfirmadaEvent;

@Component
public class CompraConfirmadaRabbitListener {

    private static final Logger log = LoggerFactory.getLogger(CompraConfirmadaRabbitListener.class);

    private final ProcesarCompraConfirmada procesarCompraConfirmada;

    public CompraConfirmadaRabbitListener(ProcesarCompraConfirmada procesarCompraConfirmada) {
        this.procesarCompraConfirmada = procesarCompraConfirmada;
    }

    @RabbitListener(queues = "${rabbitmq.notificaciones.compra.confirmada.queue}")
    public void onMessage(CompraConfirmadaEvent event) {
        String rawEmail = event != null && event.data() != null ? event.data().clienteEmail() : null;

        // Structured reception log including clienteEmail if available
        log.info(
                "event=CompraConfirmada action=received eventId={} compraId={} eventType={} eventVersion={} clienteEmail={}",
                event != null ? event.eventId() : null,
                event != null && event.data() != null ? event.data().compraId() : null,
                event != null ? event.eventType() : null,
                event != null ? event.eventVersion() : null,
                rawEmail);

        try {
            validateEventOrThrow(event);
        } catch (InvalidEventPayloadException ie) {
            log.warn(
                    "event=CompraConfirmada action=reject-dlq reason=contract-invalid eventId={} compraId={} clienteEmail={} message={}",
                    event != null ? event.eventId() : null,
                    event != null && event.data() != null ? event.data().compraId() : null,
                    rawEmail,
                    ie.getMessage());
            throw new AmqpRejectAndDontRequeueException(ie.getMessage(), ie);
        }

        // Defensive adapter-level email validation: invalid -> ACK and stop processing
        if (isEmailInvalid(rawEmail)) {
            log.warn(
                    "event=CompraConfirmada action=invalid-payload reason=invalid-email eventId={} compraId={} clienteEmail={}",
                    event != null ? event.eventId() : null,
                    event != null && event.data() != null ? event.data().compraId() : null,
                    rawEmail);
            return; // ACK
        }

        try {
            procesarCompraConfirmada.ejecutar(event);
        } catch (RuntimeException ex) {
            if (isDomainInvalidEmail(ex)) {
                log.warn(
                        "event=CompraConfirmada action=invalid-payload reason=domain-invalid-email eventId={} compraId={} clienteEmail={} message={}",
                        event != null ? event.eventId() : null,
                        event != null && event.data() != null ? event.data().compraId() : null,
                        rawEmail,
                        ex.getMessage());
                return; // ACK
            }

            if (isNonRetryableContract(ex)) {
                log.warn(
                        "event=CompraConfirmada action=reject-dlq reason=contract-violation eventId={} compraId={} message={}",
                        event != null ? event.eventId() : null,
                        event != null && event.data() != null ? event.data().compraId() : null,
                        ex.getMessage());
                throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
            }

            log.error("event=CompraConfirmada action=processing-error eventId={} compraId={} message={}",
                    event != null ? event.eventId() : null,
                    event != null && event.data() != null ? event.data().compraId() : null,
                    ex.getMessage());
            throw ex; // let retry happen for transient/system errors
        }

        log.info("event=CompraConfirmada action=processed eventId={} compraId={} clienteEmail={}",
                event != null ? event.eventId() : null,
                event != null && event.data() != null ? event.data().compraId() : null,
                rawEmail);
    }

    private void validateEventOrThrow(CompraConfirmadaEvent event) {
        if (event == null) {
            throw new InvalidEventPayloadException("El evento es nulo");
        }
        if (event.data() == null) {
            throw new InvalidEventPayloadException("Los datos del evento son obligatorios");
        }
        if (event.data().compraId() == null) {
            throw new InvalidEventPayloadException("El compraId del evento es obligatorio");
        }
        if (event.eventType() == null || event.eventType().isBlank()) {
            throw new InvalidEventPayloadException("El eventType es inválido");
        }
        // eventVersion is int primitive; if you need to validate supported versions do
        // it here
    }

    private boolean isEmailInvalid(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        return !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private boolean isDomainInvalidEmail(RuntimeException ex) {
        if (ex == null)
            return false;
        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        boolean msgSuggestsEmail = msg.contains("email")
                && (msg.contains("válid") || msg.contains("valid") || msg.contains("oblig"));
        if (!msgSuggestsEmail)
            return false;
        return originatesFrom(ex, "unrn.model.notificaciones.DestinatarioEmail");
    }

    private boolean originatesFrom(Throwable ex, String className) {
        for (StackTraceElement el : ex.getStackTrace()) {
            if (el.getClassName() != null && el.getClassName().equals(className)) {
                return true;
            }
        }
        Throwable cause = ex.getCause();
        if (cause != null && cause != ex) {
            return originatesFrom(cause, className);
        }
        return false;
    }

    private boolean isNonRetryableContract(Throwable ex) {
        // Narrow set: message conversion / mapping / explicit
        // InvalidEventPayloadException
        if (ex instanceof org.springframework.messaging.converter.MessageConversionException)
            return true;
        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (msg.contains("missing") || msg.contains("falta") || msg.contains("invalid event")
                || msg.contains("datos del evento"))
            return true;
        if (ex instanceof InvalidEventPayloadException)
            return true;
        return false;
    }
}
