package com.github.faizal.libraryeventskafka.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Book {

    private Integer bookId;
    private String bookName;
    private String bookAuthor;
}
