package ru.stockmann.BonusesService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.stockmann.BonusesService.models.BonusesInDocument;
import ru.stockmann.BonusesService.repositories.projections.DocumentCandidateRow;
import ru.stockmann.BonusesService.repositories.projections.DocumentEventsCountRow;
import ru.stockmann.BonusesService.repositories.projections.DocumentVersionRow;

import java.util.List;

public interface TransactionsBonusesDeltaRepository extends JpaRepository<BonusesInDocument, Integer> {

    @Query(value = """
        SELECT
            d.Id AS id,
            d.CurrentVersion AS currentVersion,
            bc.RowsCount AS rowsCount
        FROM dbo.Documents d
        CROSS APPLY (
            SELECT COUNT_BIG(*) AS RowsCount
            FROM dbo.BonusesInDocuments b
            WHERE b.Document = d.Id
        ) bc
        WHERE d.CurrentVersion > :cursor
          AND bc.RowsCount > 0
          AND NOT EXISTS (
              SELECT 1
              FROM dbo.SMS_informed si
              WHERE si.Document = d.Id
          )
        ORDER BY d.CurrentVersion ASC
        OFFSET 0 ROWS FETCH NEXT :fetchLimit ROWS ONLY
        """, nativeQuery = true)
    List<DocumentVersionRow> findDocumentVersionsWithBonuses(
            @Param("cursor") Long cursor,
            @Param("fetchLimit") Integer fetchLimit
    );

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO SMS_informed (Document, CurrentVersion, EventsCount, InformedAt)
        SELECT
            d.Id,
            d.CurrentVersion,
            :eventsCount,
            SYSDATETIME()
        FROM Documents d
        WHERE d.Id = :documentId
          AND NOT EXISTS (
              SELECT 1
              FROM SMS_informed si
              WHERE si.Document = d.Id
          )
        """, nativeQuery = true)
    void markDocumentAsInformed(
            @Param("documentId") Integer documentId,
            @Param("eventsCount") Long eventsCount
    );

    @Query(value = """
    SELECT TOP (:fetchLimit)
        d.Id AS id,
        d.CurrentVersion AS currentVersion
    FROM dbo.Documents d
    LEFT JOIN dbo.SMS_informed si
        ON si.Document = d.Id
    WHERE d.CurrentVersion > :cursor
      AND si.Document IS NULL
    ORDER BY
        d.CurrentVersion ASC,
        d.Id ASC
    """, nativeQuery = true)
    List<DocumentCandidateRow> findDocumentCandidates(
            @Param("cursor") Long cursor,
            @Param("fetchLimit") Integer fetchLimit
    );

    @Query(value = """
        SELECT
            b.Document AS id,
            COUNT_BIG(*) AS rowsCount
        FROM dbo.BonusesInDocuments b
        WHERE b.Document IN (:documentIds)
        GROUP BY b.Document
        """, nativeQuery = true)
    List<DocumentEventsCountRow> countEventsByDocumentIds(
            @Param("documentIds") List<Integer> documentIds
    );
}