package unrn.notificaciones.adapter.in.rabbit;

/**
 * Exception signaling a non-retryable invalid event payload detected by the
 * adapter.
 */
public class InvalidEventPayloadException extends RuntimeException {
    public InvalidEventPayloadException(String message) {
        super(message);
    }

    public InvalidEventPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
