package ru.stockmann.BonusesService.repositories.projections;

public interface DocumentVersionRow {

    Integer getId();

    Long getCurrentVersion();
}