package ru.stockmann.BonusesService.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.stockmann.BonusesService.models.api.delta.BonusDate;
import ru.stockmann.BonusesService.models.api.delta.TransactionBonusEvent;
import ru.stockmann.BonusesService.models.api.delta.TransactionsBonusesDeltaResponse;
import ru.stockmann.BonusesService.repositories.TransactionsBonusesDeltaRepository;
import ru.stockmann.BonusesService.repositories.projections.DocumentVersionRow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionsBonusesDeltaService {

    private static final Logger logger =
            LoggerFactory.getLogger(TransactionsBonusesDeltaService.class);

    @Autowired
    private TransactionsBonusesDeltaRepository deltaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public TransactionsBonusesDeltaResponse getDelta(String cursor, Integer limit) {

        Long cursorValue = Long.parseLong(cursor);



        List<DocumentVersionRow> documentRows =
                deltaRepository.findDocumentVersionsWithBonuses(cursorValue, limit + 1);

        boolean hasMore = documentRows.size() > limit;

        List<DocumentVersionRow> selectedDocuments = hasMore
                ? documentRows.subList(0, limit)
                : documentRows;

        if (selectedDocuments.isEmpty()) {
            return new TransactionsBonusesDeltaResponse(
                    List.of(),
                    formatCursor(cursorValue),
                    false
            );
        }

        List<Integer> documentIds = selectedDocuments.stream()
                .map(DocumentVersionRow::getId)
                .collect(Collectors.toList());

        logger.info("DOCUMENT IDS = {}", documentIds);
        logger.info("DOCUMENT COUNT = {}", documentIds.size());
        logger.info("CURSOR = {}, LIMIT = {}", cursorValue, limit);


/*
        List<TransactionBonusEvent> events = jdbcTemplate.query(
                """
                SELECT
                    b.CardNumber,
                    b.TypeOfIncrement,
                    b.Value,
                    CASE 
                        WHEN b.StoreId IS NULL THEN NULL
                        ELSE CONVERT(varchar(36), CONVERT(uniqueidentifier, b.StoreId))
                    END AS documentStoreID,
                    b.StoreName,
                    b.TextOperation,
                    b.OrderID,
                    CONVERT(varchar(23), b.BonusStartDate, 126) AS startDate,
                    CONVERT(varchar(23), b.BonusEndDate, 126) AS endDate,
                    d.OneCId,
                    dt.DocumentName,
                    d.Ext_Number,
                    CONVERT(varchar(23), d.Ext_Date_Time, 126) AS documentDate
                FROM BonusesInDocuments b
                JOIN Documents d ON d.Id = b.Document
                JOIN DocumentTypes dt ON dt.Id = d.DocumentType
                WHERE b.Document = ?
                ORDER BY d.CurrentVersion ASC, b.Id ASC
                """,
                (rs, rowNum) -> new TransactionBonusEvent(
                        null,
                        rs.getString("CardNumber"),
                        mapTypeOfIncrement(rs.getString("TypeOfIncrement")),
                        rs.getObject("Value") == null ? null : (int) Math.round(rs.getDouble("Value") * 100),
                        rs.getString("OneCId"),
                        rs.getString("DocumentName"),
                        rs.getString("Ext_Number"),
                        rs.getString("documentDate"),
                        rs.getString("documentStoreID"),
                        rs.getString("StoreName"),
                        rs.getString("TextOperation"),
                        rs.getString("OrderID"),
                        new BonusDate(
                                rs.getString("startDate"),
                                rs.getString("endDate")
                        )
                ),
                selectedDocuments.get(0).getId()
        );

 */

        List<TransactionBonusEvent> events = new ArrayList<>();

        for (DocumentVersionRow document : selectedDocuments) {
            List<TransactionBonusEvent> documentEvents = jdbcTemplate.query(
                    """
                    SELECT
                        b.CardNumber,
                        b.TypeOfIncrement,
                        b.Value,
                        CASE 
                            WHEN b.StoreId IS NULL THEN NULL
                            ELSE CONVERT(varchar(36), CONVERT(uniqueidentifier, b.StoreId))
                        END AS documentStoreID,
                        b.StoreName,
                        b.TextOperation,
                        b.OrderID,
                        CONVERT(varchar(23), b.BonusStartDate, 126) AS startDate,
                        CONVERT(varchar(23), b.BonusEndDate, 126) AS endDate,
                        d.OneCId,
                        dt.DocumentName,
                        d.Ext_Number,
                        CONVERT(varchar(23), d.Ext_Date_Time, 126) AS documentDate
                    FROM BonusesInDocuments b
                    JOIN Documents d ON d.Id = b.Document
                    JOIN DocumentTypes dt ON dt.Id = d.DocumentType
                    WHERE b.Document = ?
                    ORDER BY b.Id ASC
                    """,
                    (rs, rowNum) -> new TransactionBonusEvent(
                            null,
                            rs.getString("CardNumber"),
                            mapTypeOfIncrement(rs.getString("TypeOfIncrement")),
                            rs.getObject("Value") == null ? null : (int) Math.round(rs.getDouble("Value") * 100),
                            rs.getString("OneCId"),
                            rs.getString("DocumentName"),
                            rs.getString("Ext_Number"),
                            rs.getString("documentDate"),
                            rs.getString("documentStoreID"),
                            rs.getString("StoreName"),
                            rs.getString("TextOperation"),
                            rs.getString("OrderID"),
                            new BonusDate(
                                    rs.getString("startDate"),
                                    rs.getString("endDate")
                            )
                    ),
                    document.getId()
            );

            events.addAll(documentEvents);
        }


        Long nextCursor = selectedDocuments.get(selectedDocuments.size() - 1).getCurrentVersion();

        return new TransactionsBonusesDeltaResponse(
                events,
                formatCursor(nextCursor),
                hasMore
        );

    }
/*
    private TransactionBonusEvent mapToEvent(BonusDeltaRow row) {
        Integer value = row.getValue() == null
                ? null
                : (int) Math.round(row.getValue() * 100);

        return new TransactionBonusEvent(
                null,
                row.getCardNumber(),
                mapTypeOfIncrement(row.getTypeOfIncrement()),
                value,
                row.getDocumentGuid(),
                row.getDocumentType(),
                row.getDocumentNumber(),
                row.getDocumentDate(),
                row.getDocumentStoreID(),
                row.getDocumentStoreName(),
                row.getTextOperation(),
                row.getOrderID(),
                new BonusDate(
                        row.getStartDate(),
                        row.getEndDate()
                )
        );
    }

 */

    private String mapTypeOfIncrement(String typeOfIncrement) {
        if (typeOfIncrement == null) {
            return null;
        }

        if (typeOfIncrement.equals("+")) {
            return "Add";
        }

        if (typeOfIncrement.equals("-")) {
            return "Subtract";
        }

        return typeOfIncrement;
    }

    private String formatCursor(Long value) {
        return String.format("%014d", value);
    }
}