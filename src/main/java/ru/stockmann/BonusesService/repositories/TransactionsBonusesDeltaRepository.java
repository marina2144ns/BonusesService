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
        d.CurrentVersion AS currentVersion
    FROM Documents d
    WHERE d.CurrentVersion > :cursor
      AND EXISTS (
          SELECT 1
          FROM BonusesInDocuments b
          WHERE b.Document = d.Id
      )
    ORDER BY d.CurrentVersion ASC
    OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
    """, nativeQuery = true)
    List<DocumentVersionRow> findDocumentVersionsWithBonuses(
            @Param("cursor") Long cursor,
            @Param("limit") Integer limit
    );


    /*

    @Query(value = """
            SELECT 
                b.CardNumber AS cardNumber,
                b.TypeOfIncrement AS typeOfIncrement,
                b.Value AS value,
                CASE
                    WHEN b.StoreId IS NULL THEN NULL
                    ELSE CONVERT(varchar(36), CONVERT(uniqueidentifier, b.StoreId))
                END AS documentStoreID,
                b.StoreName AS documentStoreName,
                b.TextOperation AS textOperation,
                b.OrderID AS orderID,
                CONVERT(varchar(23), b.BonusStartDate, 126) AS startDate,
                CONVERT(varchar(23), b.BonusEndDate, 126) AS endDate,

                d.OneCId AS documentGuid,
                dt.DocumentName AS documentType,
                d.Ext_Number AS documentNumber,
                CONVERT(varchar(23), d.Ext_Date_Time, 126) AS documentDate,
                d.CurrentVersion AS currentVersion,
                b.Id AS bonusRowId
            FROM BonusesInDocuments b
            JOIN Documents d ON d.Id = b.Document
            JOIN DocumentTypes dt ON dt.Id = d.DocumentType
            WHERE b.Document IN (:documentIds)
            ORDER BY d.CurrentVersion ASC, b.Id ASC
            """, nativeQuery = true)
    List<BonusDeltaRow> findBonusRowsByDocumentIds(
            @Param("documentIds") List<Integer> documentIds
    );
     */
}