package com.nisum.productservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nisum.productservice.event.ProductUpdatedEvent;
import com.nisum.productservice.producer.ProductEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ProductEventProducer productEventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByStatusOrderByCreatedAtAsc("PENDING");

        if (events.isEmpty()) {
            return;
        }

        log.info(
                "Found {} pending outbox event(s)",
                events.size()
        );

        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {

        try {

            ProductUpdatedEvent productUpdatedEvent =
                    objectMapper.readValue(
                            event.getPayload(),
                            ProductUpdatedEvent.class
                    );

            log.info(
                    "Publishing outbox event: eventId={}, aggregateId={}, eventType={}",
                    event.getId(),
                    event.getAggregateId(),
                    event.getEventType()
            );

            productEventProducer.publishProductUpdated(
                    productUpdatedEvent.productId()
            );

            event.markPublished();

            outboxEventRepository.save(event);

            log.info(
                    "Outbox event marked PUBLISHED: eventId={}",
                    event.getId()
            );

        } catch (Exception ex) {

            event.recordFailure(ex.getMessage());

            outboxEventRepository.save(event);

            log.error(
                    "Failed to publish outbox event: eventId={}, attempts={}",
                    event.getId(),
                    event.getAttempts(),
                    ex
            );
        }
    }
}