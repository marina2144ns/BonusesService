package ru.stockmann.BonusesService.models.api;

import java.util.List;

public class Payload {
    private String uuid;
    private Pagination pagination;
    private List<BonusRow> rows;

    public Payload() {
    }

    public Payload(String uuid, Pagination pagination, List<BonusRow> rows) {
        this.uuid = uuid;
        this.pagination = pagination;
        this.rows = rows;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public List<BonusRow> getRows() {
        return rows;
    }

    public void setRows(List<BonusRow> rows) {
        this.rows = rows;
    }
}