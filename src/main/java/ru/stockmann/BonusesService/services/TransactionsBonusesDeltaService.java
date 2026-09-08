package ru.stockmann.BonusesService.services;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.stockmann.BonusesService.models.api.delta.BonusDate;
import ru.stockmann.BonusesService.models.api.delta.TransactionBonusEvent;
import ru.stockmann.BonusesService.repositories.TransactionsBonusesDeltaRepository;
import ru.stockmann.BonusesService.repositories.projections.DocumentCandidateRow;
import ru.stockmann.BonusesService.repositories.projections.DocumentEventsCountRow;
import ru.stockmann.BonusesService.repositories.projections.DocumentVersionRow;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TransactionsBonusesDeltaService {

    private static final Logger logger =
            LoggerFactory.getLogger(TransactionsBonusesDeltaService.class);

    /*
     * В SQL Server есть ограничение на количество параметров одного запроса.
     * Поэтому DocumentId передаём пачками.
     */
    private static final int DOCUMENTS_BATCH_SIZE = 1000;

    /*
     * Подсказка JDBC-драйверу читать ResultSet небольшими порциями.
     */
    private static final int JDBC_FETCH_SIZE = 1000;

    @Autowired
    private TransactionsBonusesDeltaRepository deltaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Реализация DocumentVersionRow для результата,
     * собранного из кандидата и отдельного подсчёта events.
     */
    private static class DocumentVersionInfo
            implements DocumentVersionRow {

        private final Integer id;
        private final Long currentVersion;
        private final Long rowsCount;

        private DocumentVersionInfo(
                Integer id,
                Long currentVersion,
                Long rowsCount
        ) {
            this.id = id;
            this.currentVersion = currentVersion;
            this.rowsCount = rowsCount;
        }

        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public Long getCurrentVersion() {
            return currentVersion;
        }

        @Override
        public Long getRowsCount() {
            return rowsCount;
        }
    }

    /**
     * Результат выбора документов.
     *
     * selectedDocuments — документы, events которых попадут в ответ.
     * nextCursor — версия последнего безопасно просмотренного документа.
     * hasMore — остались ли после него непросмотренные документы.
     */
    private static class DocumentSelectionResult {

        private final List<DocumentVersionRow> selectedDocuments;
        private final long nextCursor;
        private final boolean hasMore;

        private DocumentSelectionResult(
                List<DocumentVersionRow> selectedDocuments,
                long nextCursor,
                boolean hasMore
        ) {
            this.selectedDocuments = selectedDocuments;
            this.nextCursor = nextCursor;
            this.hasMore = hasMore;
        }

        private List<DocumentVersionRow> getSelectedDocuments() {
            return selectedDocuments;
        }

        private long getNextCursor() {
            return nextCursor;
        }

        private boolean isHasMore() {
            return hasMore;
        }
    }

    /**
     * Потоково формирует JSON-ответ delta.
     *
     * Events не накапливаются в List:
     * каждая строка ResultSet сразу записывается в HTTP response.
     */
    public void writeDelta(
            String cursor,
            Integer limit,
            OutputStream outputStream
    ) throws IOException {

        long t0 = System.currentTimeMillis();

        long cursorValue = Long.parseLong(cursor);
        int fetchLimit = limit + 1;

        /*
         * Быстро выбираем документы, которых ещё нет в SMS_informed.
         *
         * BonusesInDocuments в этом запросе не участвует.
         */
        List<DocumentCandidateRow> candidates =
                deltaRepository.findDocumentCandidates(
                        cursorValue,
                        fetchLimit
                );

        long t1 = System.currentTimeMillis();

        /*
         * Дополнительный кандидат нужен только для определения hasMore.
         * Проверяем events не более чем у limit документов.
         */
        int candidatesToInspectCount =
                Math.min(limit, candidates.size());

        List<DocumentCandidateRow> candidatesToInspect =
                candidates.subList(
                        0,
                        candidatesToInspectCount
                );

        /*
         * Считаем events только у небольшой выбранной пачки документов.
         */
        Map<Integer, Long> eventCountsByDocument =
                loadEventCounts(candidatesToInspect);

        /*
         * Сохраняем прежнюю логику ограничения по количеству events,
         * но разрешаем cursor пройти документы без events.
         */
        DocumentSelectionResult selectionResult =
                selectDocuments(
                        candidates,
                        candidatesToInspect,
                        eventCountsByDocument,
                        cursorValue,
                        limit
                );

        List<DocumentVersionRow> selectedDocuments =
                selectionResult.getSelectedDocuments();

        long nextCursor =
                selectionResult.getNextCursor();

        boolean hasMore =
                selectionResult.isHasMore();

        long t2 = System.currentTimeMillis();

        AtomicLong writtenEventsCount = new AtomicLong();

        try (JsonGenerator jsonGenerator =
                     objectMapper.getFactory().createGenerator(outputStream)) {

            /*
             * Сохраняем прежнюю обёртку успешного ответа:
             *
             * {
             *   "status": true,
             *   "data": {
             *     "events": [...],
             *     "nextCursor": "...",
             *     "hasMore": true
             *   }
             * }
             */
            jsonGenerator.writeStartObject();
            jsonGenerator.writeBooleanField("status", true);

            jsonGenerator.writeObjectFieldStart("data");
            jsonGenerator.writeArrayFieldStart("events");

            if (!selectedDocuments.isEmpty()) {
                streamEvents(
                        selectedDocuments,
                        jsonGenerator,
                        writtenEventsCount
                );
            }

            jsonGenerator.writeEndArray();

            jsonGenerator.writeStringField(
                    "nextCursor",
                    formatCursor(nextCursor)
            );

            jsonGenerator.writeBooleanField(
                    "hasMore",
                    hasMore
            );

            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();

            /*
             * Принудительно передаём сформированный JSON дальше
             * в выходной поток HTTP response.
             */
            jsonGenerator.flush();
        }

        long t3 = System.currentTimeMillis();

        /*
         * До этой точки дошли только в том случае, если чтение БД
         * и формирование JSON завершились без исключения.
         *
         * В SMS_informed записываем только документы,
         * events которых действительно были отправлены.
         */
        markDocumentsAsInformed(selectedDocuments);

        long t4 = System.currentTimeMillis();

        logger.info(
                "Delta timings ms: candidatesQuery={}, countAndSelect={}, streamEvents={}, markInformed={}, total={}, cursor={}, limit={}, candidates={}, selectedDocuments={}, events={}, nextCursor={}, hasMore={}",
                t1 - t0,
                t2 - t1,
                t3 - t2,
                t4 - t3,
                t4 - t0,
                cursorValue,
                limit,
                candidates.size(),
                selectedDocuments.size(),
                writtenEventsCount.get(),
                nextCursor,
                hasMore
        );
    }

    /**
     * Считает количество events только для документов,
     * которые действительно будут проверяться.
     */
    private Map<Integer, Long> loadEventCounts(
            List<DocumentCandidateRow> candidates
    ) {

        Map<Integer, Long> result = new HashMap<>();

        if (candidates.isEmpty()) {
            return result;
        }

        List<Integer> documentIds =
                candidates.stream()
                        .map(DocumentCandidateRow::getId)
                        .toList();

        for (
                int fromIndex = 0;
                fromIndex < documentIds.size();
                fromIndex += DOCUMENTS_BATCH_SIZE
        ) {

            int toIndex = Math.min(
                    fromIndex + DOCUMENTS_BATCH_SIZE,
                    documentIds.size()
            );

            List<Integer> documentBatch =
                    documentIds.subList(
                            fromIndex,
                            toIndex
                    );

            List<DocumentEventsCountRow> countRows =
                    deltaRepository.countEventsByDocumentIds(
                            documentBatch
                    );

            for (DocumentEventsCountRow countRow : countRows) {

                long rowsCount =
                        countRow.getRowsCount() == null
                                ? 0L
                                : countRow.getRowsCount();

                result.put(
                        countRow.getId(),
                        rowsCount
                );
            }
        }

        return result;
    }

    /**
     * Выбирает документы с учётом прежнего лимита events.
     *
     * Документы без events:
     * - не попадают в ответ;
     * - не записываются в SMS_informed;
     * - считаются просмотренными;
     * - cursor проходит дальше них.
     *
     * Первый документ с events отдаётся всегда целиком,
     * даже если число его events превышает limit.
     *
     * Если следующий непустой документ не помещается в limit,
     * выбор останавливается перед ним и cursor его не проходит.
     */
    private DocumentSelectionResult selectDocuments(
            List<DocumentCandidateRow> allCandidates,
            List<DocumentCandidateRow> candidatesToInspect,
            Map<Integer, Long> eventCountsByDocument,
            long originalCursor,
            int limit
    ) {

        List<DocumentVersionRow> selectedDocuments =
                new ArrayList<>();

        long selectedRowsCount = 0;
        long nextCursor = originalCursor;
        int processedCandidatesCount = 0;

        for (DocumentCandidateRow candidate : candidatesToInspect) {

            long documentRowsCount =
                    eventCountsByDocument.getOrDefault(
                            candidate.getId(),
                            0L
                    );

            /*
             * Пустой документ не отдаём, но cursor может пройти его.
             */
            if (documentRowsCount == 0) {
                nextCursor = candidate.getCurrentVersion();
                processedCandidatesCount++;
                continue;
            }

            /*
             * Первый непустой документ отдаём всегда целиком.
             */
            if (selectedDocuments.isEmpty()) {

                selectedDocuments.add(
                        new DocumentVersionInfo(
                                candidate.getId(),
                                candidate.getCurrentVersion(),
                                documentRowsCount
                        )
                );

                selectedRowsCount += documentRowsCount;
                nextCursor = candidate.getCurrentVersion();
                processedCandidatesCount++;

                continue;
            }

            /*
             * Следующий документ добавляем только целиком.
             */
            if (selectedRowsCount + documentRowsCount <= limit) {

                selectedDocuments.add(
                        new DocumentVersionInfo(
                                candidate.getId(),
                                candidate.getCurrentVersion(),
                                documentRowsCount
                        )
                );

                selectedRowsCount += documentRowsCount;
                nextCursor = candidate.getCurrentVersion();
                processedCandidatesCount++;

            } else {

                /*
                 * Этот документ не отдали, поэтому cursor
                 * не должен пройти дальше него.
                 */
                break;
            }
        }

        /*
         * hasMore=true в двух случаях:
         *
         * 1. Остановились внутри проверяемой пачки,
         *    потому что очередной документ не поместился.
         *
         * 2. Обработали всю проверяемую пачку, но получили
         *    дополнительного кандидата limit + 1.
         */
        boolean hasMore =
                processedCandidatesCount < candidatesToInspect.size()
                        || allCandidates.size() > candidatesToInspect.size();

        return new DocumentSelectionResult(
                selectedDocuments,
                nextCursor,
                hasMore
        );
    }

    /**
     * Выбирает events только по реально выбранным DocumentId.
     *
     * Документы разбиваются на пачки, чтобы не превысить ограничение
     * SQL Server по количеству параметров.
     */
    private void streamEvents(
            List<DocumentVersionRow> selectedDocuments,
            JsonGenerator jsonGenerator,
            AtomicLong writtenEventsCount
    ) throws IOException {

        for (
                int fromIndex = 0;
                fromIndex < selectedDocuments.size();
                fromIndex += DOCUMENTS_BATCH_SIZE
        ) {

            int toIndex = Math.min(
                    fromIndex + DOCUMENTS_BATCH_SIZE,
                    selectedDocuments.size()
            );

            List<DocumentVersionRow> documentBatch =
                    selectedDocuments.subList(
                            fromIndex,
                            toIndex
                    );

            streamEventsBatch(
                    documentBatch,
                    jsonGenerator,
                    writtenEventsCount
            );
        }
    }

    private void streamEventsBatch(
            List<DocumentVersionRow> documentBatch,
            JsonGenerator jsonGenerator,
            AtomicLong writtenEventsCount
    ) throws IOException {

        List<Integer> documentIds =
                documentBatch.stream()
                        .map(DocumentVersionRow::getId)
                        .toList();

        String placeholders =
                documentIds.stream()
                        .map(documentId -> "?")
                        .collect(Collectors.joining(","));

        String sql = """
                SELECT
                    phoneData.Phone,
                    b.CardNumber,
                    b.TypeOfIncrement,
                    b.Value,
                    CASE
                        WHEN b.StoreId IS NULL THEN NULL
                        ELSE CONVERT(
                            varchar(36),
                            CONVERT(uniqueidentifier, b.StoreId)
                        )
                    END AS documentStoreID,
                    b.StoreName,
                    b.TextOperation,
                    b.OrderID,
                    CONVERT(
                        varchar(23),
                        b.BonusStartDate,
                        126
                    ) AS startDate,
                    CONVERT(
                        varchar(23),
                        b.BonusEndDate,
                        126
                    ) AS endDate,
                    d.OneCId,
                    dt.DocumentName,
                    d.Ext_Number,
                    CONVERT(
                        varchar(23),
                        d.Ext_Date_Time,
                        126
                    ) AS documentDate
                FROM dbo.BonusesInDocuments b
                JOIN dbo.Documents d
                    ON d.Id = b.Document
                JOIN dbo.DocumentTypes dt
                    ON dt.Id = d.DocumentType
                OUTER APPLY (
                    SELECT TOP (1)
                        pp.Phone
                    FROM dbo.InformationCards ic
                    JOIN dbo.PersonPhones pp
                        ON pp.PersonId = ic.PersonId
                    WHERE ic.CardCode = b.CardNumber
                    ORDER BY
                        ic.Id ASC,
                        pp.Id ASC
                ) phoneData
                WHERE b.Document IN (%s)
                ORDER BY
                    d.CurrentVersion ASC,
                    b.Id ASC
                """.formatted(placeholders);

        try {
            jdbcTemplate.query(
                    connection -> {

                        PreparedStatement preparedStatement =
                                connection.prepareStatement(
                                        sql,
                                        java.sql.ResultSet.TYPE_FORWARD_ONLY,
                                        java.sql.ResultSet.CONCUR_READ_ONLY
                                );

                        preparedStatement.setFetchSize(
                                JDBC_FETCH_SIZE
                        );

                        for (
                                int i = 0;
                                i < documentIds.size();
                                i++
                        ) {
                            preparedStatement.setInt(
                                    i + 1,
                                    documentIds.get(i)
                            );
                        }

                        return preparedStatement;
                    },
                    resultSet -> {
                        try {
                            TransactionBonusEvent event =
                                    new TransactionBonusEvent(
                                            resultSet.getString(
                                                    "Phone"
                                            ),
                                            resultSet.getString(
                                                    "CardNumber"
                                            ),
                                            mapTypeOfIncrement(
                                                    resultSet.getString(
                                                            "TypeOfIncrement"
                                                    )
                                            ),
                                            resultSet.getObject("Value") == null
                                                    ? null
                                                    : (int) Math.round(
                                                    resultSet.getDouble(
                                                            "Value"
                                                    ) * 100
                                            ),
                                            resultSet.getString(
                                                    "OneCId"
                                            ),
                                            resultSet.getString(
                                                    "DocumentName"
                                            ),
                                            resultSet.getString(
                                                    "Ext_Number"
                                            ),
                                            resultSet.getString(
                                                    "documentDate"
                                            ),
                                            resultSet.getString(
                                                    "documentStoreID"
                                            ),
                                            resultSet.getString(
                                                    "StoreName"
                                            ),
                                            resultSet.getString(
                                                    "TextOperation"
                                            ),
                                            resultSet.getString(
                                                    "OrderID"
                                            ),
                                            new BonusDate(
                                                    resultSet.getString(
                                                            "startDate"
                                                    ),
                                                    resultSet.getString(
                                                            "endDate"
                                                    )
                                            )
                                    );

                            /*
                             * В памяти находится только один текущий event.
                             */
                            objectMapper.writeValue(
                                    jsonGenerator,
                                    event
                            );

                            writtenEventsCount.incrementAndGet();

                        } catch (IOException exception) {
                            throw new UncheckedIOException(
                                    exception
                            );
                        }
                    }
            );

        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private void markDocumentsAsInformed(
            List<DocumentVersionRow> selectedDocuments
    ) {

        for (DocumentVersionRow document : selectedDocuments) {
            deltaRepository.markDocumentAsInformed(
                    document.getId(),
                    document.getRowsCount()
            );
        }
    }

    private String mapTypeOfIncrement(
            String typeOfIncrement
    ) {

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