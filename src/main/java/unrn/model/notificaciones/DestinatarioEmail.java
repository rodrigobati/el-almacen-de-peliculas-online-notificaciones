package unrn.model.notificaciones;

import java.util.regex.Pattern;

public final class DestinatarioEmail {

    static final String ERROR_EMAIL_OBLIGATORIO = "El email es obligatorio";
    static final String ERROR_EMAIL_INVALIDO = "El email no tiene un formato válido";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String email;

    public DestinatarioEmail(String email) {
        this.email = email;

        assertEmailObligatorio();
        assertEmailValido();
    }

    public String valor() {
        return email;
    }

    private void assertEmailObligatorio() {
        if (email == null || email.isBlank())
            throw new RuntimeException(ERROR_EMAIL_OBLIGATORIO);
    }

    private void assertEmailValido() {
        if (!EMAIL.matcher(email).matches())
            throw new RuntimeException(ERROR_EMAIL_INVALIDO);
    }
}