package unrn.notificaciones.application.port;

import java.util.UUID;

import unrn.model.notificaciones.EnvioEmailCompra;

public interface RepositorioEnvioEmailCompra {

    boolean existeParaCompra(UUID compraId);

    void guardar(EnvioEmailCompra envio);

    EnvioEmailCompra buscarPorCompraId(UUID compraId);
}
