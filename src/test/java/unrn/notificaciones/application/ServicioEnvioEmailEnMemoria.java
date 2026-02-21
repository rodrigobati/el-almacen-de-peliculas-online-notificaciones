package unrn.notificaciones.application;

import unrn.model.notificaciones.NotificacionCompraPorEmail;
import unrn.notificaciones.application.port.ServicioEnvioEmail;

public class ServicioEnvioEmailEnMemoria implements ServicioEnvioEmail {

    private boolean debeFallar;
    private String mensajeError;
    private int cantidadEnvios;
    private NotificacionCompraPorEmail ultimaNotificacion;

    @Override
    public void enviar(NotificacionCompraPorEmail notificacion) {
        cantidadEnvios++;
        ultimaNotificacion = notificacion;
        if (debeFallar) {
            throw new RuntimeException(mensajeError);
        }
    }

    public void configurarFalla(String mensajeError) {
        this.debeFallar = true;
        this.mensajeError = mensajeError;
    }

    public int cantidadEnvios() {
        return cantidadEnvios;
    }

    public NotificacionCompraPorEmail ultimaNotificacion() {
        return ultimaNotificacion;
    }
}
