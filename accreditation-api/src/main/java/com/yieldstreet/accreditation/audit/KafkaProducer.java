package com.yieldstreet.accreditation.audit;

import com.yieldstreet.model.CreateAccreditationRequest;
import com.yieldstreet.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class KafkaProducer {
  private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

  /** The Spring Reactor Sink used for Kafka */
  private final Sinks.Many<Message<AccreditationStateChange>> many;

  public KafkaProducer(Sinks.Many<Message<AccreditationStateChange>> many) {
    this.many = many;
  }

  public void notifyCreate(
      CreateAccreditationRequest request, UUID accreditationId, Status status) {
    notify(AccreditationStateChange.Action.CREATE, request, accreditationId, status, null);
  }

  public void notifyFinalise(
      CreateAccreditationRequest request, UUID accreditationId, Status status, Status oldStatus) {
    notify(AccreditationStateChange.Action.FINALISE, request, accreditationId, status, oldStatus);
  }

  public void notifyScheduledExpire(
      CreateAccreditationRequest request, UUID accreditationId, Status oldStatus) {
    notify(
        AccreditationStateChange.Action.SCHEDULED_EXPIRE,
        request,
        accreditationId,
        Status.EXPIRED,
        oldStatus);
  }

  private void notify(
      AccreditationStateChange.Action action,
      CreateAccreditationRequest request,
      UUID accreditationId,
      Status status,
      Status oldStatus) {
    String userId = request.getUserId();
    OffsetDateTime timestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);

    AccreditationStateChange stateChange =
        new AccreditationStateChange(
            timestamp,
            action,
            userId,
            accreditationId.toString(),
            status,
            oldStatus,
            request);

    Message<AccreditationStateChange> message =
        MessageBuilder.withPayload(stateChange)
            .setHeader(KafkaHeaders.MESSAGE_KEY, userId.getBytes(StandardCharsets.UTF_8))
            .build();

    // try to send the message, if it fails we want an exception to stop the database transaction
    many.tryEmitNext(message).orThrow();
  }

  @Bean
  public Supplier<Flux<Message<AccreditationStateChange>>> producer(
      Sinks.Many<Message<AccreditationStateChange>> many) {
    return () ->
        many.asFlux()
            .doOnNext(
                m ->
                    logger.info(
                        "Sending message to Kafka for user {} accreditation ID {}",
                        m.getPayload().userId(),
                        m.getPayload().accreditationId()))
            .doOnError(t -> logger.error("Error encountered while sending message to Kafka", t));
  }
}
