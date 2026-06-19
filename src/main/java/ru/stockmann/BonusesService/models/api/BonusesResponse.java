package ru.stockmann.BonusesService.models.api;

public class BonusesResponse {
    private boolean status;
    private Payload payload;

    public BonusesResponse() {
    }

    public BonusesResponse(boolean status, Payload payload) {
        this.status = status;
        this.payload = payload;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }
}