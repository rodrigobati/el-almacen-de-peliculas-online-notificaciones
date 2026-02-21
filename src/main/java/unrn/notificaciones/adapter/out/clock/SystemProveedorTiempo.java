package unrn.notificaciones.adapter.out.clock;

import java.time.Instant;

import unrn.notificaciones.application.port.ProveedorTiempo;

public class SystemProveedorTiempo implements ProveedorTiempo {

    @Override
    public Instant ahora() {
        return Instant.now();
    }
}
