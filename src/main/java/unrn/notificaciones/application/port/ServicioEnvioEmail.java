package unrn.notificaciones.application.port;

import unrn.model.notificaciones.NotificacionCompraPorEmail;

public interface ServicioEnvioEmail {

    void enviar(NotificacionCompraPorEmail notificacion);
}
