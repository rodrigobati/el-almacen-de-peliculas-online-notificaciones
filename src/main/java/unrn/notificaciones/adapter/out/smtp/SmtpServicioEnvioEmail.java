package unrn.notificaciones.adapter.out.smtp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import unrn.model.notificaciones.ItemCompra;
import unrn.model.notificaciones.NotificacionCompraPorEmail;
import unrn.notificaciones.application.port.ServicioEnvioEmail;

public class SmtpServicioEnvioEmail implements ServicioEnvioEmail {

    private static final Logger log = LoggerFactory.getLogger(SmtpServicioEnvioEmail.class);
    private static final String SUBJECT = "El Almacén de Películas - Compra Confirmada";

    private final JavaMailSender mailSender;

    public SmtpServicioEnvioEmail(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviar(NotificacionCompraPorEmail notificacion) {
        String to = notificacion.destinatario().valor();
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(SUBJECT);
            msg.setText(cuerpo(notificacion));
            mailSender.send(msg);
            log.info("event=CompraConfirmada action=email-sent compraId={} to={}", notificacion.compraId(), to);
        } catch (MailAuthenticationException authEx) {
            log.error("event=CompraConfirmada action=email-send-failed reason=auth compraId={} to={} message={}",
                    notificacion.compraId(), to, authEx.getMessage());
            throw authEx;
        } catch (MailException ex) {
            // Transient or other mail errors: rethrow so listener/use-case can decide retry
            log.warn("event=CompraConfirmada action=email-send-failed reason=transient compraId={} to={} message={}",
                    notificacion.compraId(), to, ex.getMessage());
            throw ex;
        }
    }

    private String cuerpo(NotificacionCompraPorEmail notificacion) {
        String peliculas = notificacion.detalle().itemsSoloLectura().stream()
                .map(this::formatearItem)
                .collect(Collectors.joining(", "));
        String totalFinal = formatearMoneda(notificacion.detalle().total().totalFinal());
        return "Tu compra de " + peliculas + " por el precio final de " + totalFinal
                + ", ha sido confirmada.";
    }

    private String formatearItem(ItemCompra item) {
        if (item.cantidad() == 1) {
            return item.titulo();
        }
        return item.titulo() + " (x" + item.cantidad() + ")";
    }

    private String formatearMoneda(BigDecimal valor) {
        return "$" + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
