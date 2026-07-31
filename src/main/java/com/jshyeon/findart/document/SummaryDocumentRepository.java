package com.jshyeon.findart.document;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SummaryDocumentRepository extends MongoRepository<SummaryDocument, String> {
}
