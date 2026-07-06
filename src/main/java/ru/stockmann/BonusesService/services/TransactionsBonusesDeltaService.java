package ru.stockmann.BonusesService.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stockmann.BonusesService.models.api.delta.BonusDate;
import ru.stockmann.BonusesService.models.api.delta.TransactionBonusEvent;
import ru.stockmann.BonusesService.models.api.delta.TransactionsBonusesDeltaResponse;
import ru.stockmann.BonusesService.repositories.TransactionsBonusesDeltaRepository;
import ru.stockmann.BonusesService.repositories.projections.DocumentVersionRow;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionsBonusesDeltaService {

    @Autowired
    private TransactionsBonusesDeltaRepository deltaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger logger =
            LoggerFactory.getLogger(TransactionsBonusesDeltaService.class);

    @Transactional
    public TransactionsBonusesDeltaResponse getDelta(String cursor, Integer limit) {

        long t0 = System.currentTimeMillis();

        Long cursorValue = Long.parseLong(cursor);

        /*
         * Берём документов с запасом.
         * limit теперь означает лимит строк events, а не документов.
         * Если limit = 5000, нам не нужно смотреть больше 5001 документа,
         * потому что даже если в каждом документе по 1 строке, этого достаточно,
         * чтобы понять hasMore.
         */
        int fetchLimit = limit + 1;

        List<DocumentVersionRow> documentRows =
                deltaRepository.findDocumentVersionsWithBonuses(cursorValue, fetchLimit);

        long t1 = System.currentTimeMillis();

        List<DocumentVersionRow> selectedDocuments = new ArrayList<>();
        long selectedRowsCount = 0;

        for (DocumentVersionRow documentRow : documentRows) {
            long documentRowsCount = documentRow.getRowsCount() == null
                    ? 0
                    : documentRow.getRowsCount();

            /*
             * Первый документ отдаём всегда целиком,
             * даже если он один превышает limit.
             */
            if (selectedDocuments.isEmpty()) {
                selectedDocuments.add(documentRow);
                selectedRowsCount += documentRowsCount;
                continue;
            }

            /*
             * Следующие документы добавляем только если они вместе
             * укладываются в limit по количеству events.
             */
            if (selectedRowsCount + documentRowsCount <= limit) {
                selectedDocuments.add(documentRow);
                selectedRowsCount += documentRowsCount;
            } else {
                break;
            }
        }

        if (selectedDocuments.isEmpty()) {
            return new TransactionsBonusesDeltaResponse(
                    List.of(),
                    formatCursor(cursorValue),
                    false
            );
        }

        Long nextCursor = selectedDocuments
                .get(selectedDocuments.size() - 1)
                .getCurrentVersion();

        long t2 = System.currentTimeMillis();

        /*
         * hasMore = есть ли среди предварительно выбранных документов
         * документ после последнего отданного.
         */
        boolean hasMore = documentRows.stream()
                .anyMatch(d -> d.getCurrentVersion() > nextCursor);

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
                WHERE d.CurrentVersion > ?
                  AND d.CurrentVersion <= ?
                ORDER BY d.CurrentVersion ASC, b.Id ASC
                """,
                (rs, rowNum) -> new TransactionBonusEvent(
                        null,
                        rs.getString("CardNumber"),
                        mapTypeOfIncrement(rs.getString("TypeOfIncrement")),
                        rs.getObject("Value") == null
                                ? null
                                : (int) Math.round(rs.getDouble("Value") * 100),
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
                cursorValue,
                nextCursor
        );

        long t3 = System.currentTimeMillis();

        List<Integer> documentIds = selectedDocuments.stream()
                .map(DocumentVersionRow::getId)
                .toList();

        for (DocumentVersionRow document : selectedDocuments) {
            deltaRepository.markDocumentAsInformed(
                    document.getId(),
                    document.getRowsCount()
            );
        }

        long t4 = System.currentTimeMillis();

        logger.info(
                "Delta timings ms: documentsQuery={}, selectDocs={}, eventsQuery={}, markInformed={}, total={}, cursor={}, limit={}, selectedDocuments={}, events={}",
                t1 - t0,
                t2 - t1,
                t3 - t2,
                t4 - t3,
                t4 - t0,
                cursorValue,
                limit,
                selectedDocuments.size(),
                events.size()
        );

        return new TransactionsBonusesDeltaResponse(
                events,
                formatCursor(nextCursor),
                hasMore
        );
    }

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