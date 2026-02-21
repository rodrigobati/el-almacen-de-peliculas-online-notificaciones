package unrn.notificaciones.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import unrn.model.notificaciones.EnvioEmailCompra;
import unrn.model.notificaciones.EstadoEnvio;
import unrn.notificaciones.event.CompraConfirmadaEvent;

public class ProcesarCompraConfirmadaTest {

    private static final UUID COMPRA_ID = UUID.fromString("f8ca7f2a-41d6-4fa7-acf0-305f2d4a3557");

    @Test
    @DisplayName("eventoConTypeInvalido lanzaExcepcion")
    void eventoConTypeInvalido_lanzaExcepcion() {
        // Setup: Preparar el escenario
        var repositorio = new RepositorioEnvioEmailCompraEnMemoria();
        var servicio = new ServicioEnvioEmailEnMemoria();
        var reloj = new ProveedorTiempoFijo(List.of(Instant.parse("2026-02-21T10:00:00Z")));
        var casoDeUso = new ProcesarCompraConfirmada(repositorio, servicio, reloj, 3);
        var evento = eventoValido("TipoInvalido", 1);

        // Ejercitación: Ejecutar la acción a probar
        RuntimeException ex = assertThrows(RuntimeException.class, () -> casoDeUso.ejecutar(evento),
                "Debe lanzar excepción cuando el eventType es inválido");

        // Verificación: Verificar el resultado esperado
        assertEquals(ProcesarCompraConfirmada.ERROR_EVENT_TYPE_INVALIDO, ex.getMessage(),
                "El mensaje de error debe indicar eventType inválido");
    }

    @Test
    @DisplayName("eventoConVersionNoSoportada lanzaExcepcion")
    void eventoConVersionNoSoportada_lanzaExcepcion() {
        // Setup: Preparar el escenario
        var repositorio = new RepositorioEnvioEmailCompraEnMemoria();
        var servicio = new ServicioEnvioEmailEnMemoria();
        var reloj = new ProveedorTiempoFijo(List.of(Instant.parse("2026-02-21T10:00:00Z")));
        var casoDeUso = new ProcesarCompraConfirmada(repositorio, servicio, reloj, 3);
        var evento = eventoValido(CompraConfirmadaEvent.EVENT_TYPE, 2);

        // Ejercitación: Ejecutar la acción a probar
        RuntimeException ex = assertThrows(RuntimeException.class, () -> casoDeUso.ejecutar(evento),
                "Debe lanzar excepción cuando la versión no es soportada");

        // Verificación: Verificar el resultado esperado
        assertEquals(ProcesarCompraConfirmada.ERROR_EVENT_VERSION_NO_SOPORTADA, ex.getMessage(),
                "El mensaje de error debe indicar versión no soportada");
    }

    @Test
    @DisplayName("eventoNuevo enviaYMarcaEnviado")
    void eventoNuevo_enviaYMarcaEnviado() {
        // Setup: Preparar el escenario
        var repositorio = new RepositorioEnvioEmailCompraEnMemoria();
        var servicio = new ServicioEnvioEmailEnMemoria();
        Instant creadoEn = Instant.parse("2026-02-21T10:00:00Z");
        Instant enviadoEn = Instant.parse("2026-02-21T10:00:05Z");
        var reloj = new ProveedorTiempoFijo(List.of(creadoEn, enviadoEn));
        var casoDeUso = new ProcesarCompraConfirmada(repositorio, servicio, reloj, 3);
        var evento = eventoValido(CompraConfirmadaEvent.EVENT_TYPE, 1);

        // Ejercitación: Ejecutar la acción a probar
        casoDeUso.ejecutar(evento);

        // Verificación: Verificar el resultado esperado
        EnvioEmailCompra envio = repositorio.buscarPorCompraId(COMPRA_ID);
        assertNotNull(envio, "Debe guardar un envío para la compra procesada");
        assertEquals(EstadoEnvio.ENVIADO, envio.estado(), "El estado final del envío debe ser ENVIADO");
        assertEquals(enviadoEn, envio.enviadoEn(), "La fecha de envío debe quedar registrada");
        assertEquals(1, servicio.cantidadEnvios(), "Debe invocar al servicio de envío una sola vez");
        assertEquals(2, repositorio.cantidadGuardados(),
                "Debe guardar el envío inicial y el envío marcado como enviado");
    }

    @Test
    @DisplayName("eventoRepetido noDuplicaNiReenvia")
    void eventoRepetido_noDuplicaNiReenvia() {
        // Setup: Preparar el escenario
        var repositorio = new RepositorioEnvioEmailCompraEnMemoria();
        var servicio = new ServicioEnvioEmailEnMemoria();
        Instant ahora = Instant.parse("2026-02-21T10:00:00Z");
        var reloj = new ProveedorTiempoFijo(List.of(ahora));
        var casoDeUso = new ProcesarCompraConfirmada(repositorio, servicio, reloj, 3);
        var envioExistente = new EnvioEmailCompra(
                UUID.randomUUID(),
                notificacionValida(),
                EstadoEnvio.ENVIADO,
                0,
                ahora,
                ahora,
                null);
        repositorio.guardar(envioExistente);
        int guardadosAntes = repositorio.cantidadGuardados();

        // Ejercitación: Ejecutar la acción a probar
        casoDeUso.ejecutar(eventoValido(CompraConfirmadaEvent.EVENT_TYPE, 1));

        // Verificación: Verificar el resultado esperado
        assertEquals(0, servicio.cantidadEnvios(), "No debe reenviar un evento ya procesado por compraId");
        assertEquals(guardadosAntes, repositorio.cantidadGuardados(),
                "No debe crear ni actualizar envíos cuando el evento está repetido");
    }

    @Test
    @DisplayName("envioFalla conReintentosDisponibles marcaPendienteReintento")
    void envioFalla_conReintentosDisponibles_marcaPendienteReintento() {
        // Setup: Preparar el escenario
        var repositorio = new RepositorioEnvioEmailCompraEnMemoria();
        var servicio = new ServicioEnvioEmailEnMemoria();
        servicio.configurarFalla("SMTP temporalmente no disponible");
        var reloj = new ProveedorTiempoFijo(List.of(Instant.parse("2026-02-21T10:00:00Z")));
        var casoDeUso = new ProcesarCompraConfirmada(repositorio, servicio, reloj, 3);

        // Ejercitación: Ejecutar la acción a probar
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> casoDeUso.ejecutar(eventoValido(CompraConfirmadaEvent.EVENT_TYPE, 1)),
                "Debe propagar excepción cuando el envío falla");

        // Verificación: Verificar el resultado esperado
        EnvioEmailCompra envio = repositorio.buscarPorCompraId(COMPRA_ID);
        assertNotNull(envio, "Debe persistir el envío aunque falle la entrega");
        assertEquals(ProcesarCompraConfirmada.ERROR_ENVIO_EMAIL_FALLIDO, ex.getMessage(),
                "El error debe ser el definido por el caso de uso");
        assertEquals(EstadoEnvio.PENDIENTE_REINTENTO, envio.estado(),
                "Con reintentos disponibles el estado debe ser PENDIENTE_REINTENTO");
        assertEquals(1, envio.reintentos(), "Debe incrementar la cantidad de reintentos en uno");
        assertEquals("SMTP temporalmente no disponible", envio.ultimoError(),
                "Debe registrar el último error devuelto por el servicio");
    }

    @Test
    @DisplayName("envioFalla superaMaxReintentos marcaFalloPermanente")
    void envioFalla_superaMaxReintentos_marcaFalloPermanente() {
        // Setup: Preparar el escenario
        var repositorio = new RepositorioEnvioEmailCompraEnMemoria();
        var servicio = new ServicioEnvioEmailEnMemoria();
        servicio.configurarFalla("SMTP definitivamente caído");
        var reloj = new ProveedorTiempoFijo(List.of(Instant.parse("2026-02-21T10:00:00Z")));
        var casoDeUso = new ProcesarCompraConfirmada(repositorio, servicio, reloj, 1);

        // Ejercitación: Ejecutar la acción a probar
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> casoDeUso.ejecutar(eventoValido(CompraConfirmadaEvent.EVENT_TYPE, 1)),
                "Debe propagar excepción cuando supera el máximo de reintentos");

        // Verificación: Verificar el resultado esperado
        EnvioEmailCompra envio = repositorio.buscarPorCompraId(COMPRA_ID);
        assertNotNull(envio, "Debe existir envío persistido para la compra");
        assertEquals(ProcesarCompraConfirmada.ERROR_ENVIO_EMAIL_FALLIDO, ex.getMessage(),
                "El mensaje de error debe ser el definido por el caso de uso");
        assertEquals(EstadoEnvio.FALLO_PERMANENTE, envio.estado(),
                "Al superar maxReintentos el estado debe ser FALLO_PERMANENTE");
        assertEquals(1, envio.reintentos(), "Debe quedar registrado el intento fallido");
        assertEquals("SMTP definitivamente caído", envio.ultimoError(),
                "Debe conservar el motivo del fallo definitivo");
    }

    private CompraConfirmadaEvent eventoValido(String eventType, int eventVersion) {
        return new CompraConfirmadaEvent(
                UUID.randomUUID(),
                eventType,
                eventVersion,
                Instant.parse("2026-02-21T09:59:59Z"),
                "ventas-service",
                new CompraConfirmadaEvent.Data(
                        COMPRA_ID,
                        "cliente@correo.com",
                        Instant.parse("2026-02-21T09:59:00Z"),
                        List.of(
                                new CompraConfirmadaEvent.ItemCompraConfirmada("Inception", 1,
                                        new BigDecimal("12000.00")),
                                new CompraConfirmadaEvent.ItemCompraConfirmada("Interstellar", 1,
                                        new BigDecimal("10000.00"))),
                        new CompraConfirmadaEvent.TotalCompraConfirmada(
                                new BigDecimal("22000.00"),
                                new BigDecimal("2000.00"),
                                "Cupón BIENVENIDA10")));
    }

    private unrn.model.notificaciones.NotificacionCompraPorEmail notificacionValida() {
        return new unrn.model.notificaciones.NotificacionCompraPorEmail(
                COMPRA_ID,
                new unrn.model.notificaciones.DestinatarioEmail("cliente@correo.com"),
                new unrn.model.notificaciones.DetalleCompra(
                        List.of(new unrn.model.notificaciones.ItemCompra("Inception", 1,
                                new BigDecimal("12000.00"))),
                        new unrn.model.notificaciones.TotalCompra(new BigDecimal("12000.00"), BigDecimal.ZERO, null)),
                Instant.parse("2026-02-21T09:59:00Z"));
    }
}
