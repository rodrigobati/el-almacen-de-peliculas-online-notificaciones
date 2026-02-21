package unrn.model.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DetalleCompraTest {

    @Test
    @DisplayName("Compra sin items lanza excepción")
    void CompraSinItems_lanzaException() {
        // Setup: Preparar el escenario
        List<ItemCompra> items = new ArrayList<>();
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class,
                () -> new DetalleCompra(items, new TotalCompra(BigDecimal.ONE, BigDecimal.ZERO, null)));
        // Verificación: Verificar el resultado esperado
        assertEquals(DetalleCompra.ERROR_ITEMS_OBLIGATORIOS, ex.getMessage());
    }

    @Test
    @DisplayName("Total nulo lanza excepción")
    void TotalNulo_lanzaException() {
        // Setup: Preparar el escenario
        List<ItemCompra> items = List.of(new ItemCompra("A", 1, BigDecimal.ONE));
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new DetalleCompra(items, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(DetalleCompra.ERROR_TOTAL_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("Detalle valido itemsReadOnly lanza UnsupportedOperationException al modificar")
    void DetalleValido_itemsSoloLecturaInmutable() {
        // Setup: Preparar el escenario
        List<ItemCompra> items = List.of(new ItemCompra("A", 1, BigDecimal.ONE));
        DetalleCompra d = new DetalleCompra(items, new TotalCompra(BigDecimal.ONE, BigDecimal.ZERO, null));
        // Ejercitación: Ejecutar la acción a probar
        var readOnly = d.itemsSoloLectura();
        // Verificación: Verificar el resultado esperado
        assertThrows(UnsupportedOperationException.class,
                () -> ((List<ItemCompra>) readOnly).add(new ItemCompra("B", 1, BigDecimal.ONE)));
    }
}
