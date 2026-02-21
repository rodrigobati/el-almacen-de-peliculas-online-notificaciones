package unrn.notificaciones.adapter.out.smtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import unrn.model.notificaciones.NotificacionCompraPorEmail;
import unrn.notificaciones.application.port.ServicioEnvioEmail;

public class SmtpServicioEnvioEmail implements ServicioEnvioEmail {

    private static final Logger log = LoggerFactory.getLogger(SmtpServicioEnvioEmail.class);

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
            msg.setSubject("Compra confirmada");
            msg.setText("Tu compra " + notificacion.compraId() + " fue confirmada.");
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
}
