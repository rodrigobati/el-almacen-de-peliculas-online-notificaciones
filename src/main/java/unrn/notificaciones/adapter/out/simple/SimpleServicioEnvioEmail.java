package unrn.notificaciones.adapter.out.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unrn.model.notificaciones.NotificacionCompraPorEmail;
import unrn.notificaciones.application.port.ServicioEnvioEmail;

public class SimpleServicioEnvioEmail implements ServicioEnvioEmail {

    private static final Logger log = LoggerFactory.getLogger(SimpleServicioEnvioEmail.class);

    @Override
    public void enviar(NotificacionCompraPorEmail notificacion) {
        // Minimal implementation: log the action. Keeps behavior deterministic for
        // tests.
        if (notificacion == null) {
            throw new RuntimeException("Notificacion vacía");
        }
        log.info("[SimulatedEmail] Enviando email a {} para compra {}", notificacion.destinatario().valor(),
                notificacion.compraId());
    }
}
