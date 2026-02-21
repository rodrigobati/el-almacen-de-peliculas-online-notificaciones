package unrn.notificaciones.adapter.in.rabbit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import unrn.notificaciones.application.ProcesarCompraConfirmada;
import unrn.notificaciones.application.ProveedorTiempoFijo;
import unrn.notificaciones.application.RepositorioEnvioEmailCompraEnMemoria;
import unrn.notificaciones.application.ServicioEnvioEmailEnMemoria;
import unrn.notificaciones.event.CompraConfirmadaEvent;

public class CompraConfirmadaRabbitListenerUnitTest {

    private static final UUID COMPRA_ID = UUID.fromString("f8ca7f2a-41d6-4fa7-acf0-305f2d4a3557");

    @Test
    @DisplayName("listener acepta evento con email válido")
    void listener_accepts_valid_email() {
        var repositorio = new RepositorioEnvioEmailCompraEnMemoria();
        var servicio = new ServicioEnvioEmailEnMemoria();
        var reloj = new ProveedorTiempoFijo(
                List.of(Instant.parse("2026-02-21T10:00:00Z"), Instant.parse("2026-02-21T10:00:05Z")));

        class SpyProcesar extends ProcesarCompraConfirmada {
            boolean invoked = false;

            SpyProcesar() {
                super(repositorio, servicio, reloj, 3);
            }

            @Override
            public void ejecutar(CompraConfirmadaEvent evento) {
                invoked = true;
                super.ejecutar(evento);
            }
        }

        var casoDeUso = new SpyProcesar();
        var listener = new CompraConfirmadaRabbitListener(casoDeUso);

        var evento = new CompraConfirmadaEvent(
                UUID.randomUUID(),
                CompraConfirmadaEvent.EVENT_TYPE,
                CompraConfirmadaEvent.EVENT_VERSION,
                Instant.now(),
                "ventas-service",
                new CompraConfirmadaEvent.Data(
                        COMPRA_ID,
                        "cliente@correo.com",
                        Instant.now(),
                        List.of(new CompraConfirmadaEvent.ItemCompraConfirmada("Peli", 1, new BigDecimal("100.00"))),
                        new CompraConfirmadaEvent.TotalCompraConfirmada(new BigDecimal("100.00"), BigDecimal.ZERO,
                                null)));

        assertDoesNotThrow(() -> listener.onMessage(evento));
        if (!casoDeUso.invoked) {
            throw new RuntimeException("use case not invoked for valid email");
        }
    }

    @Test
    @DisplayName("listener ACKs invalid email and does not invoke use case")
    void listener_acks_invalid_email_and_skips_use_case() {
        var repositorio = new RepositorioEnvioEmailCompraEnMemoria();
        var servicio = new ServicioEnvioEmailEnMemoria();
        var reloj = new ProveedorTiempoFijo(List.of(Instant.parse("2026-02-21T10:00:00Z")));

        class SpyProcesar extends ProcesarCompraConfirmada {
            boolean invoked = false;

            SpyProcesar() {
                super(repositorio, servicio, reloj, 3);
            }

            @Override
            public void ejecutar(CompraConfirmadaEvent evento) {
                invoked = true;
                super.ejecutar(evento);
            }
        }

        var casoDeUso = new SpyProcesar();
        var listener = new CompraConfirmadaRabbitListener(casoDeUso);

        var evento = new CompraConfirmadaEvent(
                UUID.randomUUID(),
                CompraConfirmadaEvent.EVENT_TYPE,
                CompraConfirmadaEvent.EVENT_VERSION,
                Instant.now(),
                "ventas-service",
                new CompraConfirmadaEvent.Data(
                        COMPRA_ID,
                        "invalid-email",
                        Instant.now(),
                        List.of(new CompraConfirmadaEvent.ItemCompraConfirmada("Peli", 1, new BigDecimal("100.00"))),
                        new CompraConfirmadaEvent.TotalCompraConfirmada(new BigDecimal("100.00"), BigDecimal.ZERO,
                                null)));

        // listener must ACK invalid payload and not throw
        assertDoesNotThrow(() -> listener.onMessage(evento));
        // and the use case must NOT have been invoked
        if (casoDeUso.invoked) {
            throw new RuntimeException("Use case should not be invoked for invalid email");
        }
    }
}
