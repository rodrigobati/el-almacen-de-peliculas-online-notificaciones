package unrn.model.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EnvioEmailCompraTest {

    @Test
    @DisplayName("EnvioId nulo lanza excepción")
    void EnvioIdNulo_lanzaException() {
        // Setup: Preparar el escenario
        UUID envioId = null;
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class,
                () -> new EnvioEmailCompra(envioId,
                        new NotificacionCompraPorEmail(UUID.randomUUID(), new DestinatarioEmail("a@b.com"),
                                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                                Instant.now()),
                        EstadoEnvio.PENDIENTE, 0, Instant.now(), null, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(EnvioEmailCompra.ERROR_ID_ENVIO_OBLIGATORIO, ex.getMessage());
    }

    @Test
    @DisplayName("Notificacion nula lanza excepción")
    void NotificacionNula_lanzaException() {
        // Setup: Preparar el escenario
        UUID envioId = UUID.randomUUID();
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class,
                () -> new EnvioEmailCompra(envioId, null, EstadoEnvio.PENDIENTE, 0, Instant.now(), null, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(EnvioEmailCompra.ERROR_NOTIFICACION_OBLIGATORIA, ex.getMessage());
    }

    @Test
    @DisplayName("Estado nulo lanza excepción")
    void EstadoNulo_lanzaException() {
        // Setup: Preparar el escenario
        UUID envioId = UUID.randomUUID();
        var not = new NotificacionCompraPorEmail(UUID.randomUUID(), new DestinatarioEmail("a@b.com"),
                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                Instant.now());
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class,
                () -> new EnvioEmailCompra(envioId, not, null, 0, Instant.now(), null, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(EnvioEmailCompra.ERROR_ESTADO_OBLIGATORIO, ex.getMessage());
    }

    @Test
    @DisplayName("Reintentos negativos lanza excepción")
    void ReintentosNegativos_lanzaException() {
        // Setup: Preparar el escenario
        UUID envioId = UUID.randomUUID();
        var not = new NotificacionCompraPorEmail(UUID.randomUUID(), new DestinatarioEmail("a@b.com"),
                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                Instant.now());
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class,
                () -> new EnvioEmailCompra(envioId, not, EstadoEnvio.PENDIENTE, -1, Instant.now(), null, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(EnvioEmailCompra.ERROR_REINTENTOS_NEGATIVOS, ex.getMessage());
    }

    @Test
    @DisplayName("CreadoEn nulo lanza excepción")
    void CreadoEnNulo_lanzaException() {
        // Setup: Preparar el escenario
        UUID envioId = UUID.randomUUID();
        var not = new NotificacionCompraPorEmail(UUID.randomUUID(), new DestinatarioEmail("a@b.com"),
                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                Instant.now());
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class,
                () -> new EnvioEmailCompra(envioId, not, EstadoEnvio.PENDIENTE, 0, null, null, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(EnvioEmailCompra.ERROR_FECHA_CREACION_OBLIGATORIA, ex.getMessage());
    }

    @Test
    @DisplayName("Marcar enviado actualiza estado y enviadoEn y borra ultimoError")
    void MarcarEnviado_cambiaEstadoYFecha() {
        // Setup: Preparar el escenario
        UUID envioId = UUID.randomUUID();
        var not = new NotificacionCompraPorEmail(UUID.randomUUID(), new DestinatarioEmail("a@b.com"),
                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                Instant.now());
        var envio = new EnvioEmailCompra(envioId, not, EstadoEnvio.PENDIENTE, 0, Instant.now(), null, "error");
        Instant cuando = Instant.now();
        // Ejercitación: Ejecutar la acción a probar
        var marcado = envio.marcarEnviado(cuando);
        // Verificación: Verificar el resultado esperado
        assertEquals(EstadoEnvio.ENVIADO, marcado.estado());
        assertEquals(cuando, marcado.enviadoEn());
        assertNull(marcado.ultimoError());
        assertEquals(envio.reintentos(), marcado.reintentos());
    }

    @Test
    @DisplayName("Marcar fallido incrementa reintentos y setea estado apropiado")
    void MarcarFallido_incrementaYSeteaEstado() {
        // Setup: Preparar el escenario
        UUID envioId = UUID.randomUUID();
        var not = new NotificacionCompraPorEmail(UUID.randomUUID(), new DestinatarioEmail("a@b.com"),
                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                Instant.now());
        var envio = new EnvioEmailCompra(envioId, not, EstadoEnvio.PENDIENTE, 0, Instant.now(), null, null);
        // Ejercitación: Ejecutar la acción a probar (no alcanza max)
        var fallido = envio.marcarFallido("err", 3);
        // Verificación: Verificar el resultado esperado
        assertEquals(1, fallido.reintentos());
        assertEquals(EstadoEnvio.PENDIENTE_REINTENTO, fallido.estado());
        assertEquals("err", fallido.ultimoError());

        // Ejercitación: Ejecutar la acción a probar (alcanza max)
        var envio2 = new EnvioEmailCompra(envioId, not, EstadoEnvio.PENDIENTE, 2, Instant.now(), null, null);
        var fallido2 = envio2.marcarFallido("fatal", 3);
        // Verificación: Verificar el resultado esperado
        assertEquals(3, fallido2.reintentos());
        assertEquals(EstadoEnvio.FALLO_PERMANENTE, fallido2.estado());
        assertEquals("fatal", fallido2.ultimoError());
    }
}
