package unrn.model.notificaciones;

import java.math.BigDecimal;
import java.util.List;

public final class DetalleCompra {

    static final String ERROR_ITEMS_OBLIGATORIOS = "La compra debe tener al menos un ítem";
    static final String ERROR_TOTAL_INVALIDO = "El total debe ser mayor o igual a cero";

    private final List<ItemCompra> items;
    private final TotalCompra total;

    public DetalleCompra(List<ItemCompra> items, TotalCompra total) {
        this.items = List.copyOf(items == null ? List.of() : items);
        this.total = total;

        assertItemsObligatorios();
        assertTotalValido();
    }

    public List<ItemCompra> itemsSoloLectura() {
        return items; // ya es inmutable por copyOf
    }

    public TotalCompra total() {
        return total;
    }

    private void assertItemsObligatorios() {
        if (items.isEmpty())
            throw new RuntimeException(ERROR_ITEMS_OBLIGATORIOS);
    }

    private void assertTotalValido() {
        if (total == null)
            throw new RuntimeException(ERROR_TOTAL_INVALIDO);
        total.assertValido();
    }
}