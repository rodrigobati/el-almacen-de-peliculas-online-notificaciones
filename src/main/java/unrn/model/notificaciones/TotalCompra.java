package unrn.model.notificaciones;

import java.math.BigDecimal;

public final class TotalCompra {

    static final String ERROR_TOTAL_OBLIGATORIO = "El total es obligatorio";
    static final String ERROR_TOTAL_NEGATIVO = "El total no puede ser negativo";
    static final String ERROR_DESCUENTO_NEGATIVO = "El descuento no puede ser negativo";
    static final String ERROR_TOTAL_FINAL_INVALIDO = "El total final no puede ser negativo";

    private final BigDecimal totalBruto;
    private final BigDecimal descuento;
    private final String descuentoDescripcion; // opcional, puede ser null

    public TotalCompra(BigDecimal totalBruto, BigDecimal descuento, String descuentoDescripcion) {
        this.totalBruto = totalBruto;
        this.descuento = descuento == null ? BigDecimal.ZERO : descuento;
        this.descuentoDescripcion = descuentoDescripcion;

        assertValido();
    }

    void assertValido() {
        assertTotalObligatorio();
        assertTotalNoNegativo();
        assertDescuentoNoNegativo();
        assertTotalFinalNoNegativo();
    }

    public BigDecimal totalBruto() {
        return totalBruto;
    }

    public BigDecimal descuento() {
        return descuento;
    }

    public String descuentoDescripcion() {
        return descuentoDescripcion;
    }

    public BigDecimal totalFinal() {
        return totalBruto.subtract(descuento);
    }

    private void assertTotalObligatorio() {
        if (totalBruto == null)
            throw new RuntimeException(ERROR_TOTAL_OBLIGATORIO);
    }

    private void assertTotalNoNegativo() {
        if (totalBruto.signum() < 0)
            throw new RuntimeException(ERROR_TOTAL_NEGATIVO);
    }

    private void assertDescuentoNoNegativo() {
        if (descuento.signum() < 0)
            throw new RuntimeException(ERROR_DESCUENTO_NEGATIVO);
    }

    private void assertTotalFinalNoNegativo() {
        if (totalFinal().signum() < 0)
            throw new RuntimeException(ERROR_TOTAL_FINAL_INVALIDO);
    }
}