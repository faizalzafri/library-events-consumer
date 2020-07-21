package com.github.faizal.libraryeventskafka.service;

import com.github.faizal.libraryeventskafka.domain.LibraryEvent;
import com.github.faizal.libraryeventskafka.model.LibraryEventModel;
import com.github.faizal.libraryeventskafka.repository.LibraryEventRepository;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@Slf4j
public class LibraryEventService {

    @Autowired
    private LibraryEventRepository eventRepository;

    static Gson GSON = new Gson();

    public void processLibraryEvent(ConsumerRecord<String, String> consumerRecord) {

        String value = consumerRecord.value();
        LibraryEvent libEvent = convert(GSON.fromJson(value, LibraryEventModel.class));

        switch (libEvent.getLibraryEventType()) {
            case NEW:
                save(libEvent);
                break;
            case UPDATE:
                validate(libEvent);
                save(libEvent);
                break;
        }
    }

    private LibraryEvent convert(LibraryEventModel fromJson) {
        ObjectId libraryEventId = new ObjectId(fromJson.getLibraryEventId());
        return new LibraryEvent(libraryEventId, fromJson.getLibraryEventType(), fromJson.getBook());
    }

    private void validate(LibraryEvent libEvent) {

        if (Objects.isNull(libEvent.getLibraryEventId()))
            throw new IllegalArgumentException("Missing id, cannot update");

        LibraryEvent event = eventRepository.findById(libEvent.getLibraryEventId())
                .orElseThrow(() -> {
                    return new IllegalArgumentException("Invalid library event id");
                });

        log.info("Validation successful for event id {}", libEvent.getLibraryEventId());
    }

    private void save(LibraryEvent libEvent) {

        LibraryEvent le = eventRepository.save(libEvent);
        log.info("Event Saved: {}", le);
    }
}
