package org.louisjohns32.personal.exchange.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("exchange.kafka.topics.order-events")
    private String ordersTopic;

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ordersTopic).partitions(3).build();
    }

}
