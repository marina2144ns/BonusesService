package ru.stockmann.BonusesService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.stockmann.BonusesService.models.BonusesInDocument;
import ru.stockmann.BonusesService.repositories.projections.DocumentVersionRow;

import java.util.List;

public interface TransactionsBonusesDeltaRepository extends JpaRepository<BonusesInDocument, Integer> {

    @Query(value = """
        SELECT
            d.Id AS id,
            d.CurrentVersion AS currentVersion,
            COUNT_BIG(b.Id) AS rowsCount
        FROM Documents d
        JOIN BonusesInDocuments b
            ON b.Document = d.Id
        WHERE d.CurrentVersion > :cursor
        GROUP BY
            d.Id,
            d.CurrentVersion
        ORDER BY d.CurrentVersion ASC
        OFFSET 0 ROWS FETCH NEXT :fetchLimit ROWS ONLY
        """, nativeQuery = true)
    List<DocumentVersionRow> findDocumentVersionsWithBonuses(
            @Param("cursor") Long cursor,
            @Param("fetchLimit") Integer fetchLimit
    );



}