package ru.trushkov.worker.configuration;

import lombok.Setter;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MarshallingMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
@Setter
public class RabbitConfiguration {

    @Value("${queue.response}")
    private String responseQueueName;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${exchange.name}")
    private String exchangeName;

    @Bean("responseQueue")
    public Queue responseQueue() {
        return new Queue(responseQueueName, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding binding1(Queue responseQueue, DirectExchange exchange) {
        return BindingBuilder.bind(responseQueue).to(exchange).with("task.manager");
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        System.out.println("pered connecting");
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("rabbitmq2", 5672);
        System.out.println("posle connecting");
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin() {
        try {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
            rabbitAdmin.declareExchange(exchange());
            return rabbitAdmin;
        } catch (Exception e) {
            System.err.println("RabbitAdmin init failed: " + e.getMessage());
            return new RabbitAdmin(connectionFactory());
        }
    }


    @Bean
    public MessageConverter receiveConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        return converter;
    }

    @Bean(name = "receiveConnectionFactory")
    public CachingConnectionFactory receiveConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory("rabbitmq1", 5672);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            @Qualifier("receiveConnectionFactory") CachingConnectionFactory receiveConnectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(receiveConnectionFactory);
        factory.setMessageConverter(receiveConverter());
        return factory;
    }

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse.class);
        return marshaller;
    }

    @Bean
    public MarshallingMessageConverter xmlMessageConverter(Jaxb2Marshaller marshaller) {
        return new MarshallingMessageConverter(marshaller, marshaller);
    }

    @Bean
    public AmqpTemplate amqpTemplate(
            @Qualifier("connectionFactory") CachingConnectionFactory connectionFactory,
            @Qualifier("xmlMessageConverter") MarshallingMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
