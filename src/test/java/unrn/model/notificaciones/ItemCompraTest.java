package unrn.model.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ItemCompraTest {

    @Test
    @DisplayName("Titulo nulo lanza excepción")
    void TituloNulo_lanzaException() {
        // Setup: Preparar el escenario
        String titulo = null;
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new ItemCompra(titulo, 1, BigDecimal.ONE));
        // Verificación: Verificar el resultado esperado
        assertEquals(ItemCompra.ERROR_TITULO_OBLIGATORIO, ex.getMessage());
    }

    @Test
    @DisplayName("Cantidad no positiva lanza excepción")
    void CantidadNoPositiva_lanzaException() {
        // Setup: Preparar el escenario
        String titulo = "Movie";
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new ItemCompra(titulo, 0, BigDecimal.ONE));
        // Verificación: Verificar el resultado esperado
        assertEquals(ItemCompra.ERROR_CANTIDAD_INVALIDA, ex.getMessage());
    }

    @Test
    @DisplayName("Precio nulo lanza excepción")
    void PrecioNulo_lanzaException() {
        // Setup: Preparar el escenario
        String titulo = "Movie";
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new ItemCompra(titulo, 1, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(ItemCompra.ERROR_PRECIO_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("Precio negativo lanza excepción")
    void PrecioNegativo_lanzaException() {
        // Setup: Preparar el escenario
        String titulo = "Movie";
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new ItemCompra(titulo, 1, BigDecimal.valueOf(-1)));
        // Verificación: Verificar el resultado esperado
        assertEquals(ItemCompra.ERROR_PRECIO_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("Item valido calcula subtotal")
    void ItemValido_subtotalCorrecto() {
        // Setup: Preparar el escenario
        ItemCompra item = new ItemCompra("Movie", 3, BigDecimal.valueOf(10));
        // Ejercitación: Ejecutar la acción a probar
        var subtotal = item.subtotal();
        // Verificación: Verificar el resultado esperado
        assertEquals(BigDecimal.valueOf(30), subtotal, "El subtotal no coincide");
    }
}
