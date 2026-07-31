package com.jshyeon.findart.document;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ContentDocumentRepository extends MongoRepository<ContentDocument, String> {
}
