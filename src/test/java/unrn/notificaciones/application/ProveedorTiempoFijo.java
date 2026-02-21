package unrn.notificaciones.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import unrn.notificaciones.application.port.ProveedorTiempo;

public class ProveedorTiempoFijo implements ProveedorTiempo {

    private final List<Instant> instantes;
    private int indice;

    public ProveedorTiempoFijo(List<Instant> instantes) {
        this.instantes = new ArrayList<>(instantes);
    }

    @Override
    public Instant ahora() {
        if (instantes.isEmpty()) {
            throw new RuntimeException("Debe configurar al menos un instante");
        }

        if (indice >= instantes.size()) {
            return instantes.get(instantes.size() - 1);
        }

        Instant actual = instantes.get(indice);
        indice++;
        return actual;
    }
}
