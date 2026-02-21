package unrn.model.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TotalCompraTest {

    @Test
    @DisplayName("Total bruto nulo lanza excepción")
    void TotalBrutoNulo_lanzaException() {
        // Setup: Preparar el escenario
        BigDecimal totalBruto = null;
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new TotalCompra(totalBruto, BigDecimal.ZERO, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(TotalCompra.ERROR_TOTAL_OBLIGATORIO, ex.getMessage());
    }

    @Test
    @DisplayName("Total bruto negativo lanza excepción")
    void TotalBrutoNegativo_lanzaException() {
        // Setup: Preparar el escenario
        BigDecimal totalBruto = BigDecimal.valueOf(-1);
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new TotalCompra(totalBruto, BigDecimal.ZERO, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(TotalCompra.ERROR_TOTAL_NEGATIVO, ex.getMessage());
    }

    @Test
    @DisplayName("Descuento negativo lanza excepción")
    void DescuentoNegativo_lanzaException() {
        // Setup: Preparar el escenario
        BigDecimal totalBruto = BigDecimal.TEN;
        BigDecimal descuento = BigDecimal.valueOf(-1);
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new TotalCompra(totalBruto, descuento, "x"));
        // Verificación: Verificar el resultado esperado
        assertEquals(TotalCompra.ERROR_DESCUENTO_NEGATIVO, ex.getMessage());
    }

    @Test
    @DisplayName("Total final negativo lanza excepción")
    void TotalFinalNegativo_lanzaException() {
        // Setup: Preparar el escenario
        BigDecimal totalBruto = BigDecimal.TEN;
        BigDecimal descuento = BigDecimal.valueOf(20);
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new TotalCompra(totalBruto, descuento, null));
        // Verificación: Verificar el resultado esperado
        assertEquals(TotalCompra.ERROR_TOTAL_FINAL_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("Total valido calcula totalFinal y acepta descuento null")
    void TotalValido_totalFinalCorrecto() {
        // Setup: Preparar el escenario
        TotalCompra t1 = new TotalCompra(BigDecimal.valueOf(100), null, null);
        TotalCompra t2 = new TotalCompra(BigDecimal.valueOf(100), BigDecimal.valueOf(30), "promo");
        // Ejercitación: Ejecutar la acción a probar
        var tf1 = t1.totalFinal();
        var tf2 = t2.totalFinal();
        // Verificación: Verificar el resultado esperado
        assertEquals(BigDecimal.valueOf(100), tf1, "Total final con descuento null debe ser totalBruto");
        assertEquals(BigDecimal.valueOf(70), tf2, "Total final incorrecto con descuento");
    }
}
