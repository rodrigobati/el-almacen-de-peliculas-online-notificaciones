package unrn.notificaciones.application.port;

import java.time.Instant;

public interface ProveedorTiempo {

    Instant ahora();
}
