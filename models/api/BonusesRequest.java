package ru.stockmann.BonusesService.models.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public class BonusesRequest {
    private String phone;
    private int clientSiteId;
    @JsonProperty("CardNumber")
    private String cardNumber;
    private int byPage;
    private int page;
    @JsonProperty("FromDate")
    private LocalDateTime fromDate;
    @JsonProperty("ToDate")
    private LocalDateTime toDate;
    private UUID uuid;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getClientSiteId() {
        return clientSiteId;
    }

    public void setClientSiteId(int clientSiteId) {
        this.clientSiteId = clientSiteId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public int getByPage() {
        return byPage;
    }

    public void setByPage(int byPage) {
        this.byPage = byPage;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "BonusesRequest{" +
                "phone='" + phone + '\'' +
                ", clientSiteId=" + clientSiteId +
                ", cardNumber='" + cardNumber + '\'' +
                ", byPage=" + byPage +
                ", page=" + page +
                ", fromDate=" + fromDate +
                ", toDate=" + toDate +
                ", uuid=" + uuid +
                '}';
    }
}
