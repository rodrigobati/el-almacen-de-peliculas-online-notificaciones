package unrn.model.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NotificacionCompraPorEmailTest {

    @Test
    @DisplayName("CompraId nulo lanza excepción")
    void CompraIdNulo_lanzaException() {
        // Setup: Preparar el escenario
        UUID id = null;
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class,
                () -> new NotificacionCompraPorEmail(id, new DestinatarioEmail("a@b.com"),
                        new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                                new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                        Instant.now()));
        // Verificación: Verificar el resultado esperado
        assertEquals(NotificacionCompraPorEmail.ERROR_COMPRA_ID_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("Destinatario nulo lanza excepción")
    void DestinatarioNulo_lanzaException() {
        // Setup: Preparar el escenario
        UUID id = UUID.randomUUID();
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new NotificacionCompraPorEmail(id, null,
                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                Instant.now()));
        // Verificación: Verificar el resultado esperado
        assertEquals(NotificacionCompraPorEmail.ERROR_DESTINATARIO_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("Detalle nulo lanza excepción")
    void DetalleNulo_lanzaException() {
        // Setup: Preparar el escenario
        UUID id = UUID.randomUUID();
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class,
                () -> new NotificacionCompraPorEmail(id, new DestinatarioEmail("a@b.com"), null, Instant.now()));
        // Verificación: Verificar el resultado esperado
        assertEquals(NotificacionCompraPorEmail.ERROR_DETALLE_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("Fecha nula lanza excepción")
    void FechaNula_lanzaException() {
        // Setup: Preparar el escenario
        UUID id = UUID.randomUUID();
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class,
                () -> new NotificacionCompraPorEmail(id, new DestinatarioEmail("a@b.com"),
                        new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                                new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                        null));
        // Verificación: Verificar el resultado esperado
        assertEquals(NotificacionCompraPorEmail.ERROR_FECHA_INVALIDA, ex.getMessage());
    }

    @Test
    @DisplayName("Igualdad por compraId funciona")
    void Igualdad_porCompraId() {
        // Setup: Preparar el escenario
        UUID id = UUID.randomUUID();
        var n1 = new NotificacionCompraPorEmail(id, new DestinatarioEmail("a@b.com"),
                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                Instant.now());
        var n2 = new NotificacionCompraPorEmail(id, new DestinatarioEmail("other@x.com"),
                new DetalleCompra(java.util.List.of(new ItemCompra("Y", 2, java.math.BigDecimal.valueOf(2))),
                        new TotalCompra(java.math.BigDecimal.valueOf(4), java.math.BigDecimal.ZERO, null)),
                Instant.now());
        // Ejercitación: Ejecutar la acción a probar
        boolean eq = n1.equals(n2);
        // Verificación: Verificar el resultado esperado
        assertTrue(eq, "Notificaciones con mismo compraId deben ser iguales");
        assertEquals(n1.hashCode(), n2.hashCode(), "hashCode debe basarse en compraId");
    }

    @Test
    @DisplayName("Distinto compraId no son iguales")
    void DistintoCompraId_noSonIguales() {
        // Setup: Preparar el escenario
        var n1 = new NotificacionCompraPorEmail(UUID.randomUUID(), new DestinatarioEmail("a@b.com"),
                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                Instant.now());
        var n2 = new NotificacionCompraPorEmail(UUID.randomUUID(), new DestinatarioEmail("a@b.com"),
                new DetalleCompra(java.util.List.of(new ItemCompra("X", 1, java.math.BigDecimal.ONE)),
                        new TotalCompra(java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, null)),
                Instant.now());
        // Ejercitación: Ejecutar la acción a probar
        boolean eq = n1.equals(n2);
        // Verificación: Verificar el resultado esperado
        assertFalse(eq, "Notificaciones con distinto compraId no deben ser iguales");
    }
}
