package unrn.notificaciones.adapter.out.inmemory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import unrn.model.notificaciones.EnvioEmailCompra;
import unrn.notificaciones.application.port.RepositorioEnvioEmailCompra;

public class InMemoryRepositorioEnvioEmailCompra implements RepositorioEnvioEmailCompra {

    private final Map<UUID, EnvioEmailCompra> store = new ConcurrentHashMap<>();

    @Override
    public boolean existeParaCompra(UUID compraId) {
        return store.containsKey(compraId);
    }

    @Override
    public void guardar(EnvioEmailCompra envio) {
        if (envio != null && envio.compraId() != null) {
            store.put(envio.compraId(), envio);
        }
    }

    @Override
    public EnvioEmailCompra buscarPorCompraId(UUID compraId) {
        return store.get(compraId);
    }
}
