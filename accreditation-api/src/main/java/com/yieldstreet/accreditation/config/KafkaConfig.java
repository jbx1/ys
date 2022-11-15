package com.yieldstreet.accreditation.config;

import com.yieldstreet.accreditation.audit.AccreditationStateChange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Sinks;

@Configuration
public class KafkaConfig {

  @Bean
  public Sinks.Many<Message<AccreditationStateChange>> many() {
    return Sinks.many().unicast().onBackpressureBuffer();
  }
}
