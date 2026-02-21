package unrn.model.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DestinatarioEmailTest {

    @Test
    @DisplayName("Email nulo lanza excepción")
    void EmailNulo_lanzaException() {
        // Setup: Preparar el escenario
        String email = null;
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new DestinatarioEmail(email));
        // Verificación: Verificar el resultado esperado
        assertEquals(DestinatarioEmail.ERROR_EMAIL_OBLIGATORIO, ex.getMessage());
    }

    @Test
    @DisplayName("Email en blanco lanza excepción")
    void EmailEnBlanco_lanzaException() {
        // Setup: Preparar el escenario
        String email = "  ";
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new DestinatarioEmail(email));
        // Verificación: Verificar el resultado esperado
        assertEquals(DestinatarioEmail.ERROR_EMAIL_OBLIGATORIO, ex.getMessage());
    }

    @Test
    @DisplayName("Email inválido lanza excepción")
    void EmailInvalido_lanzaException() {
        // Setup: Preparar el escenario
        String email = "no-at-symbol";
        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> new DestinatarioEmail(email));
        // Verificación: Verificar el resultado esperado
        assertEquals(DestinatarioEmail.ERROR_EMAIL_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("Email válido crea objeto")
    void EmailValido_creaObjeto() {
        // Setup: Preparar el escenario
        String email = "user@example.com";
        // Ejercitación: Ejecutar la acción a probar
        DestinatarioEmail d = new DestinatarioEmail(email);
        // Verificación: Verificar el resultado esperado
        assertEquals(email, d.valor(), "El email almacenado no coincide");
    }
}
