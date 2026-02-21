package unrn.model.notificaciones;

import java.math.BigDecimal;

public final class ItemCompra {

    static final String ERROR_TITULO_OBLIGATORIO = "El título es obligatorio";
    static final String ERROR_CANTIDAD_INVALIDA = "La cantidad debe ser mayor a cero";
    static final String ERROR_PRECIO_INVALIDO = "El precio unitario debe ser mayor o igual a cero";

    private final String titulo;
    private final int cantidad;
    private final BigDecimal precioUnitario;

    public ItemCompra(String titulo, int cantidad, BigDecimal precioUnitario) {
        this.titulo = titulo;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;

        assertTituloObligatorio();
        assertCantidadValida();
        assertPrecioValido();
    }

    public String titulo() {
        return titulo;
    }

    public int cantidad() {
        return cantidad;
    }

    public BigDecimal precioUnitario() {
        return precioUnitario;
    }

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    private void assertTituloObligatorio() {
        if (titulo == null || titulo.isBlank())
            throw new RuntimeException(ERROR_TITULO_OBLIGATORIO);
    }

    private void assertCantidadValida() {
        if (cantidad <= 0)
            throw new RuntimeException(ERROR_CANTIDAD_INVALIDA);
    }

    private void assertPrecioValido() {
        if (precioUnitario == null || precioUnitario.signum() < 0)
            throw new RuntimeException(ERROR_PRECIO_INVALIDO);
    }
}