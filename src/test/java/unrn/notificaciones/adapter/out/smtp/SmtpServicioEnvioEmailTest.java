package unrn.notificaciones.adapter.out.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import unrn.model.notificaciones.DestinatarioEmail;
import unrn.model.notificaciones.NotificacionCompraPorEmail;
import unrn.model.notificaciones.DetalleCompra;
import unrn.model.notificaciones.ItemCompra;
import unrn.model.notificaciones.TotalCompra;

class TestJavaMailSender implements JavaMailSender {
    SimpleMailMessage last;

    @Override
    public void send(SimpleMailMessage simpleMessage) {
        this.last = simpleMessage;
    }

    // other methods not needed for this test
    @Override
    public void send(SimpleMailMessage... simpleMessages) {
        throw new UnsupportedOperationException();
    }

    @Override
    public jakarta.mail.internet.MimeMessage createMimeMessage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public jakarta.mail.internet.MimeMessage createMimeMessage(java.io.InputStream contentStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(jakarta.mail.internet.MimeMessage mimeMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(jakarta.mail.internet.MimeMessage... mimeMessages) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(org.springframework.mail.javamail.MimeMessagePreparator mimeMessagePreparator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(org.springframework.mail.javamail.MimeMessagePreparator... mimeMessagePreparators) {
        throw new UnsupportedOperationException();
    }
}

public class SmtpServicioEnvioEmailTest {

    @Test
    @DisplayName("SMTP adapter sends simple mail message with expected fields")
    void smtp_sends_email() {
        TestJavaMailSender mailSender = new TestJavaMailSender();
        SmtpServicioEnvioEmail adapter = new SmtpServicioEnvioEmail(mailSender);

        var detalle = new DetalleCompra(List.of(new ItemCompra("Peli", 1, new BigDecimal("100.00"))),
                new TotalCompra(new BigDecimal("100.00"), BigDecimal.ZERO, null));
        var notificacion = new NotificacionCompraPorEmail(
                java.util.UUID.fromString("f8ca7f2a-41d6-4fa7-acf0-305f2d4a3557"),
                new DestinatarioEmail("cliente@correo.com"),
                detalle,
                Instant.now());

        adapter.enviar(notificacion);

        SimpleMailMessage sent = mailSender.last;
        assertEquals("cliente@correo.com", sent.getTo()[0]);
        assertEquals("Compra confirmada", sent.getSubject());
    }
}
