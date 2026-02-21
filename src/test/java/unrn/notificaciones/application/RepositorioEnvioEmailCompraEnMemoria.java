package unrn.notificaciones.application;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import unrn.model.notificaciones.EnvioEmailCompra;
import unrn.notificaciones.application.port.RepositorioEnvioEmailCompra;

public class RepositorioEnvioEmailCompraEnMemoria implements RepositorioEnvioEmailCompra {

    private final Map<UUID, EnvioEmailCompra> enviosPorCompraId = new HashMap<>();
    private int cantidadGuardados;

    @Override
    public boolean existeParaCompra(UUID compraId) {
        return enviosPorCompraId.containsKey(compraId);
    }

    @Override
    public void guardar(EnvioEmailCompra envio) {
        enviosPorCompraId.put(envio.compraId(), envio);
        cantidadGuardados++;
    }

    @Override
    public EnvioEmailCompra buscarPorCompraId(UUID compraId) {
        return enviosPorCompraId.get(compraId);
    }

    int cantidadGuardados() {
        return cantidadGuardados;
    }
}
