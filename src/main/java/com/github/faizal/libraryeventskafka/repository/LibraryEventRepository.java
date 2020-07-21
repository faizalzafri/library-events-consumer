package com.github.faizal.libraryeventskafka.repository;

import com.github.faizal.libraryeventskafka.domain.LibraryEvent;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LibraryEventRepository extends MongoRepository<LibraryEvent, ObjectId> {
}
