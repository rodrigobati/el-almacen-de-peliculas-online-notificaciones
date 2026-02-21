package unrn.notificaciones.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqNotificacionesConfig {

    @Value("${rabbitmq.compras.events.exchange}")
    private String comprasEventsExchangeName;

    @Value("${rabbitmq.compras.compra.confirmada.routing-key}")
    private String compraConfirmadaRoutingKey;

    @Value("${rabbitmq.notificaciones.compra.confirmada.queue}")
    private String compraConfirmadaQueueName;

    @Value("${rabbitmq.notificaciones.dlx.exchange}")
    private String deadLetterExchangeName;

    @Value("${rabbitmq.notificaciones.compra.confirmada.dlq}")
    private String compraConfirmadaDlqName;

    @Value("${rabbitmq.notificaciones.compra.confirmada.dlq.routing-key}")
    private String compraConfirmadaDlqRoutingKey;

    @Bean
    public TopicExchange comprasEventsExchange() {
        return new TopicExchange(comprasEventsExchangeName, true, false);
    }

    @Bean
    public DirectExchange notificacionesDeadLetterExchange() {
        return new DirectExchange(deadLetterExchangeName, true, false);
    }

    @Bean
    public Queue compraConfirmadaQueue() {
        return QueueBuilder.durable(compraConfirmadaQueueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(compraConfirmadaDlqRoutingKey)
                .build();
    }

    @Bean
    public Queue compraConfirmadaDlqQueue() {
        return QueueBuilder.durable(compraConfirmadaDlqName).build();
    }

    @Bean
    public Binding compraConfirmadaBinding() {
        return BindingBuilder.bind(compraConfirmadaQueue())
                .to(comprasEventsExchange())
                .with(compraConfirmadaRoutingKey);
    }

    @Bean
    public Binding compraConfirmadaDlqBinding() {
        return BindingBuilder.bind(compraConfirmadaDlqQueue())
                .to(notificacionesDeadLetterExchange())
                .with(compraConfirmadaDlqRoutingKey);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
