package unrn.notificaciones.adapter.in.rabbit;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import unrn.notificaciones.application.ProcesarCompraConfirmada;
import unrn.notificaciones.event.CompraConfirmadaEvent;

class CompraConfirmadaRabbitListenerTest {

    private CompraConfirmadaEvent makeEvent(String email) {
        var compraId = UUID.randomUUID();
        var data = new CompraConfirmadaEvent.Data(compraId, email, Instant.now(), List.of(),
                new CompraConfirmadaEvent.TotalCompraConfirmada(BigDecimal.ZERO, BigDecimal.ZERO, ""));
        return new CompraConfirmadaEvent(UUID.randomUUID(), CompraConfirmadaEvent.EVENT_TYPE,
                CompraConfirmadaEvent.EVENT_VERSION, Instant.now(), "test", data);
    }

    @Test
    @DisplayName("accepts event with valid email")
    void acceptsEventWithValidEmail_doesInvokeUseCase() {
        var event = makeEvent("user@example.com");
        // create a simple ProcesarCompraConfirmada stub that records invocation
        final boolean[] called = { false };
        ProcesarCompraConfirmada procesar = new ProcesarCompraConfirmada(
                new unrn.notificaciones.adapter.out.inmemory.InMemoryRepositorioEnvioEmailCompra(),
                new unrn.notificaciones.adapter.out.simple.SimpleServicioEnvioEmail(),
                () -> Instant.now(), 3) {
            @Override
            public void ejecutar(CompraConfirmadaEvent evento) {
                called[0] = true;
            }
        };
        var listener = new CompraConfirmadaRabbitListener(procesar);
        assertDoesNotThrow(() -> listener.onMessage(event));
        assertTrue(called[0]);
    }

    @Test
    @DisplayName("ACKs invalid email and does not invoke use case")
    void acksInvalidEmail_doesNotInvokeUseCase() {
        var event = makeEvent("invalid-email");
        // use a no-op procesar to ensure it's not invoked
        ProcesarCompraConfirmada procesarNoop = new ProcesarCompraConfirmada(
                new unrn.notificaciones.adapter.out.inmemory.InMemoryRepositorioEnvioEmailCompra(),
                new unrn.notificaciones.adapter.out.simple.SimpleServicioEnvioEmail(),
                () -> Instant.now(), 3) {
            @Override
            public void ejecutar(CompraConfirmadaEvent evento) {
                throw new RuntimeException("SHOULD_NOT_BE_CALLED");
            }
        };
        var listener = new CompraConfirmadaRabbitListener(procesarNoop);
        assertDoesNotThrow(() -> listener.onMessage(event));
    }

    @Test
    @DisplayName("ACKs domain invalid email exceptions and does not propagate")
    void acksDomainInvalidEmailException_doesNotPropagate() {
        var event = makeEvent("user@example.com");
        RuntimeException ex = new RuntimeException("El email no tiene un formato válido");
        StackTraceElement domainFrame = new StackTraceElement("unrn.model.notificaciones.DestinatarioEmail", "<init>",
                "DestinatarioEmail.java", 10);
        ex.setStackTrace(new StackTraceElement[] { domainFrame });

        ProcesarCompraConfirmada procesarThrow = new ProcesarCompraConfirmada(
                new unrn.notificaciones.adapter.out.inmemory.InMemoryRepositorioEnvioEmailCompra(),
                new unrn.notificaciones.adapter.out.simple.SimpleServicioEnvioEmail(),
                () -> Instant.now(), 3) {
            @Override
            public void ejecutar(CompraConfirmadaEvent evento) {
                throw ex;
            }
        };
        var listener = new CompraConfirmadaRabbitListener(procesarThrow);
        assertDoesNotThrow(() -> listener.onMessage(event));
    }

    @Test
    @DisplayName("rethrows unexpected runtime exception to allow retry")
    void rethrowsUnexpectedRuntimeException_allowsRetry() {
        var event = makeEvent("user@example.com");
        RuntimeException ex = new RuntimeException("DB timeout");
        ProcesarCompraConfirmada procesarThrow = new ProcesarCompraConfirmada(
                new unrn.notificaciones.adapter.out.inmemory.InMemoryRepositorioEnvioEmailCompra(),
                new unrn.notificaciones.adapter.out.simple.SimpleServicioEnvioEmail(),
                () -> Instant.now(), 3) {
            @Override
            public void ejecutar(CompraConfirmadaEvent evento) {
                throw ex;
            }
        };
        var listener = new CompraConfirmadaRabbitListener(procesarThrow);
        assertThrows(RuntimeException.class, () -> listener.onMessage(event));
    }

    @Test
    @DisplayName("rejects null event to DLQ")
    void rejectsNullEvent_toDlq() {
        ProcesarCompraConfirmada procesarNoop = new ProcesarCompraConfirmada(
                new unrn.notificaciones.adapter.out.inmemory.InMemoryRepositorioEnvioEmailCompra(),
                new unrn.notificaciones.adapter.out.simple.SimpleServicioEnvioEmail(),
                () -> Instant.now(), 3) {
            @Override
            public void ejecutar(CompraConfirmadaEvent evento) {
                // no-op
            }
        };
        var listener = new CompraConfirmadaRabbitListener(procesarNoop);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onMessage(null));
    }
}
