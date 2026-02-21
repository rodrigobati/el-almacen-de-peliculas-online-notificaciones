package unrn.notificaciones.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import unrn.notificaciones.application.ProcesarCompraConfirmada;
import unrn.notificaciones.application.port.ProveedorTiempo;
import unrn.notificaciones.application.port.RepositorioEnvioEmailCompra;
import unrn.notificaciones.application.port.ServicioEnvioEmail;
import unrn.notificaciones.adapter.out.inmemory.InMemoryRepositorioEnvioEmailCompra;
import unrn.notificaciones.adapter.out.simple.SimpleServicioEnvioEmail;
import unrn.notificaciones.adapter.out.clock.SystemProveedorTiempo;

@Configuration
public class NotificacionesApplicationConfig {

    @Value("${notificaciones.maxReintentos:3}")
    private int defaultMaxReintentos;

    @Bean
    public RepositorioEnvioEmailCompra repositorioEnvioEmailCompra() {
        return new InMemoryRepositorioEnvioEmailCompra();
    }

    @Bean
    public ServicioEnvioEmail servicioEnvioEmail(ObjectProvider<JavaMailSender> mailSenderProvider) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            return new unrn.notificaciones.adapter.out.smtp.SmtpServicioEnvioEmail(mailSender);
        }
        return new SimpleServicioEnvioEmail();
    }

    @Bean
    public ProveedorTiempo proveedorTiempo() {
        return new SystemProveedorTiempo();
    }

    @Bean
    public ProcesarCompraConfirmada procesarCompraConfirmada(RepositorioEnvioEmailCompra repositorio,
            ServicioEnvioEmail servicioEnvioEmail, ProveedorTiempo proveedorTiempo) {
        return new ProcesarCompraConfirmada(repositorio, servicioEnvioEmail, proveedorTiempo, defaultMaxReintentos);
    }
}
