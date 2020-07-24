package com.github.faizal.libraryeventskafka.consumer;

import com.github.faizal.libraryeventskafka.domain.Book;
import com.github.faizal.libraryeventskafka.domain.LibraryEvent;
import com.github.faizal.libraryeventskafka.domain.LibraryEventType;
import com.github.faizal.libraryeventskafka.model.LibraryEventModel;
import com.github.faizal.libraryeventskafka.repository.LibraryEventRepository;
import com.github.faizal.libraryeventskafka.service.LibraryEventService;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class LibraryEventsConsumerIntegrationTest {

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, String> template;

    @Autowired
    KafkaListenerEndpointRegistry registry;

    @Autowired
    LibraryEventRepository repository;

    @SpyBean
    LibraryEventsConsumer spyConsumer;

    @SpyBean
    LibraryEventService spyService;

    @BeforeEach
    void setUp() {
        for(MessageListenerContainer container: registry.getAllListenerContainers()){
            ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());;
        }
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void consumerNewEvent() throws ExecutionException, InterruptedException {
        //given
        Book book = Book.builder()
                .bookId(111)
                .bookName("Learn Kafka")
                .bookAuthor("Faizal")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .libraryEventType(LibraryEventType.NEW)
                .book(book)
                .build();

        String value = new Gson().toJson(libraryEvent);

        template.sendDefault(value).get();

        //when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        //then
        verify(spyConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(spyService, times(1)).processLibraryEvent(isA(ConsumerRecord.class));
    }

    @Test
    void consumerUpdateEvent() throws ExecutionException, InterruptedException {
        //given
        Book book = Book.builder()
                .bookId(111)
                .bookName("Learn Kafka With Me")
                .bookAuthor("Faizal")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .libraryEventType(LibraryEventType.NEW)
                .book(book)
                .build();

        LibraryEvent savedLe = repository.save(libraryEvent);

        Book book2 = Book.builder()
                .bookId(41285)
                .bookName("Learn Kafka With Me")
                .bookAuthor("Faizal Zafri")
                .build();
        savedLe.setBook(book2);
        savedLe.setLibraryEventType(LibraryEventType.UPDATE);

        String value = new Gson().toJson(convert(savedLe));

        template.sendDefault(savedLe.getLibraryEventId().toString(), value).get();

        //when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        //then
        verify(spyConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(spyService, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        Optional<LibraryEvent> optUpdateLe = repository.findById(savedLe.getLibraryEventId());
        assert optUpdateLe.isPresent(); //checks if lib event id present and valid
        LibraryEvent upLe = optUpdateLe.get();
        assertEquals(upLe.getBook().getBookId(),book2.getBookId());
        assertEquals(upLe.getBook().getBookName(), book2.getBookName());

    }

    private LibraryEventModel convert(LibraryEvent le) {
        return LibraryEventModel.builder()
                .libraryEventId(le.getLibraryEventId() != null ? le.getLibraryEventId().toString() : null)
                .libraryEventType(le.getLibraryEventType())
                .book(le.getBook()).build();
    }


    @Test
    void consumerUpdateEvent_NotValidLibraryEvent() throws ExecutionException, InterruptedException {
        //given
        Book book = Book.builder()
                .bookId(111)
                .bookName("Learn Kafka With Me")
                .bookAuthor("Faizal")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(new ObjectId("5f187e5e1526ae459748f9de"))
                .libraryEventType(LibraryEventType.UPDATE)
                .book(book)
                .build();

        String value = new Gson().toJson(convert(libraryEvent));
        template.sendDefault(libraryEvent.getLibraryEventId().toString(), value).get();

        //when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        //then
        verify(spyConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(spyService, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        Optional<LibraryEvent> optUpdateLe = repository.findById(libraryEvent.getLibraryEventId());
        assertFalse(optUpdateLe.isPresent()); //checks if lib event id present and valid

    }

    @Test
    void consumerUpdateEvent_NullLibraryEventId() throws ExecutionException, InterruptedException {
        //given
        Book book = Book.builder()
                .bookId(111)
                .bookName("Learn Kafka With Me")
                .bookAuthor("Faizal")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .libraryEventType(LibraryEventType.UPDATE)
                .book(book)
                .build();

        String value = new Gson().toJson(convert(libraryEvent));
        template.sendDefault(null, value).get();

        //when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        //then
        verify(spyConsumer, times(3)).onMessage(isA(ConsumerRecord.class));
        verify(spyService, times(3)).processLibraryEvent(isA(ConsumerRecord.class));

    }


}