//package com.calixtoneto.rinha.backend.config;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.index.Index;
//import org.springframework.stereotype.Component;
//
//@Component
//public class MongoDbInitializer {
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @PostConstruct
//    public void initIndexes() {
//        mongoTemplate.indexOps("payments")
//                .ensureIndex(new Index().on("requestedAt", 1));
//        mongoTemplate.indexOps("payments")
//                .ensureIndex(new Index().on("correlationId", 1));
//    }
//}