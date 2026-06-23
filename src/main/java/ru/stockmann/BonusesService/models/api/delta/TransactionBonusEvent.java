package ru.stockmann.BonusesService.models.api.delta;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class TransactionBonusEvent {

    private String phone;
    private String cardNumber;
    private String typeOfIncrement;
    private Integer value;
    private String documentGuid;
    private String documentType;
    private String documentNumber;
    private String documentDate;
    private String documentStoreID;
    private String documentStoreName;
    private String textOperation;

    @JsonProperty("orderID")
    private String orderID;

    private BonusDate bonusDate;

    public TransactionBonusEvent() {
    }

    public TransactionBonusEvent(
            String phone,
            String cardNumber,
            String typeOfIncrement,
            Integer value,
            String documentGuid,
            String documentType,
            String documentNumber,
            String documentDate,
            String documentStoreID,
            String documentStoreName,
            String textOperation,
            String orderID,
            BonusDate bonusDate
    ) {
        this.phone = phone;
        this.cardNumber = cardNumber;
        this.typeOfIncrement = typeOfIncrement;
        this.value = value;
        this.documentGuid = documentGuid;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.documentDate = documentDate;
        this.documentStoreID = documentStoreID;
        this.documentStoreName = documentStoreName;
        this.textOperation = textOperation;
        this.orderID = orderID;
        this.bonusDate = bonusDate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getTypeOfIncrement() {
        return typeOfIncrement;
    }

    public void setTypeOfIncrement(String typeOfIncrement) {
        this.typeOfIncrement = typeOfIncrement;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getDocumentGuid() {
        return documentGuid;
    }

    public void setDocumentGuid(String documentGuid) {
        this.documentGuid = documentGuid;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(String documentDate) {
        this.documentDate = documentDate;
    }

    public String getDocumentStoreID() {
        return documentStoreID;
    }

    public void setDocumentStoreID(String documentStoreID) {
        this.documentStoreID = documentStoreID;
    }

    public String getDocumentStoreName() {
        return documentStoreName;
    }

    public void setDocumentStoreName(String documentStoreName) {
        this.documentStoreName = documentStoreName;
    }

    public String getTextOperation() {
        return textOperation;
    }

    public void setTextOperation(String textOperation) {
        this.textOperation = textOperation;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public BonusDate getBonusDate() {
        return bonusDate;
    }

    public void setBonusDate(BonusDate bonusDate) {
        this.bonusDate = bonusDate;
    }
}