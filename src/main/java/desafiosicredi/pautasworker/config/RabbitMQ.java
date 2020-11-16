package desafiosicredi.pautasworker.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQ {

	public final static String FILA_CONTABILIZAR_VOTO = "pautas.contabilizar-voto";
	public final static String FILA_VERIFICAR_STATUS_PAUTA = "pautas.verificar-status-pauta";
	public final static String FILA_STATUS_PAUTA = "pautas.status-pauta";

	@Bean
	Queue contabilizarVotoQueue() {
		return QueueBuilder.durable(FILA_CONTABILIZAR_VOTO).build();
	}

	@Bean
	Queue verificarStatusPautaQueue() {
		return QueueBuilder.durable(FILA_VERIFICAR_STATUS_PAUTA).build();
	}

	@Bean
	Queue statusPautaQueue() {
		return QueueBuilder.durable(FILA_STATUS_PAUTA).build();
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		return rabbitTemplate;
	}
	
}
