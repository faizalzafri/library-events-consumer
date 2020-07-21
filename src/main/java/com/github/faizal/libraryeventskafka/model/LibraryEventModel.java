package com.github.faizal.libraryeventskafka.model;

import com.github.faizal.libraryeventskafka.domain.Book;
import com.github.faizal.libraryeventskafka.domain.LibraryEventType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LibraryEventModel {

    private String libraryEventId;
    private LibraryEventType libraryEventType;
    private Book book;
}
