# el-almacen-de-peliculas-online-notificaciones

## Documentacion corta de vertical

### Proposito

La vertical Notificaciones procesa compras confirmadas y envia la notificacion por email al cliente. Modela el contenido de la compra, valida el destinatario y delega el envio a un adaptador SMTP/simple segun configuracion.

### Servicios HTTP que expone

No expone endpoints de negocio. Solo quedan disponibles endpoints de actuator segun configuracion, por ejemplo `/actuator/health` y `/actuator/info`.

### Eventos que publica

No publica eventos de dominio actualmente.

### Eventos que consume

| Exchange | Cola | Routing key / tipo | Proposito |
| --- | --- | --- | --- |
| `peliculas.eventos.compras` | `notificaciones.compra-confirmada.v1` | `compra.confirmada.v1` / `CompraConfirmada` | Enviar email cuando Ventas confirma una compra. |
| `peliculas.eventos.dlx` | `notificaciones.compra-confirmada.v1.dlq` | `notificaciones.compra-confirmada.v1.dlq` | DLQ para eventos invalidos o no procesables. |
