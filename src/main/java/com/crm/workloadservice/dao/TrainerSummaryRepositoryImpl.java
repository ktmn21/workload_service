package com.crm.workloadservice.dao;

import com.crm.workloadservice.model.TrainerSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TrainerSummaryRepositoryImpl implements TrainerSummaryRepositoryCustom {

    private static final Logger log = LoggerFactory.getLogger(TrainerSummaryRepositoryImpl.class);
    private static final String TX_KEY = "transactionId";

    private final MongoTemplate mongoTemplate;

    public TrainerSummaryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<TrainerSummary> searchByUsername(String username) {
        log.info("[txId={}] operation=SEARCH collection=trainer_summary username={}", MDC.get(TX_KEY), username);
        Query query = new Query(Criteria.where("username").is(username));
        TrainerSummary found = mongoTemplate.findOne(query, TrainerSummary.class);
        log.debug("[txId={}] operation=SEARCH result={}", MDC.get(TX_KEY), found != null ? "FOUND" : "NOT_FOUND");
        return Optional.ofNullable(found);
    }

    @Override
    public TrainerSummary updateByUsername(String username, TrainerSummary updated) {
        log.info("[txId={}] operation=UPDATE collection=trainer_summary username={}", MDC.get(TX_KEY), username);
        Query query = new Query(Criteria.where("username").is(username));
        Update update = new Update()
                .set("firstName", updated.getFirstName())
                .set("lastName", updated.getLastName())
                .set("status", updated.getStatus())
                .set("years", updated.getYears());

        mongoTemplate.upsert(query, update, TrainerSummary.class);
        log.debug("[txId={}] operation=UPDATE completed username={}", MDC.get(TX_KEY), username);
        return mongoTemplate.findOne(query, TrainerSummary.class);
    }
}
