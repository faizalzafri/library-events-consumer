package com.github.faizal.libraryeventskafka.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@AllArgsConstructor
@Data
@Builder
public class LibraryEvent {

    @MongoId
    private ObjectId libraryEventId;
    private LibraryEventType libraryEventType;
    private Book book;
}
