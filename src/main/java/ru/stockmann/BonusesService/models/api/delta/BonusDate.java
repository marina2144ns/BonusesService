package ru.stockmann.BonusesService.models.api.delta;

import java.time.LocalDateTime;

public class BonusDate {

    private String startDate;
    private String endDate;

    public BonusDate() {
    }

    public BonusDate(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

}