package ru.stockmann.BonusesService.models.api.delta;

import java.util.List;

public class TransactionsBonusesDeltaResponse {

    private List<TransactionBonusEvent> events;
    private String nextCursor;
    private Boolean hasMore;

    public TransactionsBonusesDeltaResponse() {
    }

    public TransactionsBonusesDeltaResponse(List<TransactionBonusEvent> events, String nextCursor, Boolean hasMore) {
        this.events = events;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    public List<TransactionBonusEvent> getEvents() {
        return events;
    }

    public void setEvents(List<TransactionBonusEvent> events) {
        this.events = events;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
}