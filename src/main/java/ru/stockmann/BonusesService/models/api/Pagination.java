package ru.stockmann.BonusesService.models.api;

public class Pagination {
    private Integer all;
    private Integer current;
    private Integer byPage;

    public Pagination() {
    }

    public Pagination(Integer all, Integer current, Integer byPage) {
        this.all = all;
        this.current = current;
        this.byPage = byPage;
    }

    public Integer getAll() {
        return all;
    }

    public void setAll(Integer all) {
        this.all = all;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getByPage() {
        return byPage;
    }

    public void setByPage(Integer byPage) {
        this.byPage = byPage;
    }
}
