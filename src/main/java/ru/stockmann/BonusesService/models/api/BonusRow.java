package ru.stockmann.BonusesService.models.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class BonusRow {
    @JsonProperty("Date")
    private LocalDateTime date;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("Value")
    private Integer value;
    @JsonProperty("Text")
    private String text;
    @JsonProperty("OrderId")
    private String orderId;


    public BonusRow() {
    }

    public BonusRow(LocalDateTime date, String type, Integer value, String text, String orderId) {
        this.date = date;
        this.type = type;
        this.value = value;
        this.text = text;
        this.orderId = orderId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
