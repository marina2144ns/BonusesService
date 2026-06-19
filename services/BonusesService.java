package ru.stockmann.BonusesService.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.stockmann.BonusesService.models.BonusesInDocument;
import ru.stockmann.BonusesService.models.api.*;
import ru.stockmann.BonusesService.repositories.BonusesRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BonusesService {

    @Autowired
    private BonusesRepository bonusesRepository;

    //private static final Logger logger = LoggerFactory.getLogger(BonusesService.class);

    public BonusesResponse searchBonuses(BonusesRequest request) {

        int page = request.getPage();
        int byPage = request.getByPage();
        PageRequest pageRequest = PageRequest.of(page, byPage);

        if (request.getToDate()==null || request.getToDate().isAfter(LocalDateTime.now()))
        {
            request.setToDate(LocalDateTime.now());
        }

        Page<BonusesInDocument> bonusesPage = bonusesRepository.searchBonuses(request, pageRequest);


        List<BonusRow> bonusRows = bonusesPage.getContent()
                .stream()
                .map(this::mapToBonusRow)
                .collect(Collectors.toList());

        Pagination pagination = new Pagination(
                (int) bonusesPage.getTotalElements(),
                bonusesPage.getNumber(),
                bonusesPage.getSize()
        );

        Payload payload = new Payload(request.getUuid().toString(), pagination, bonusRows);
        return new BonusesResponse(true, payload);
    }

    private BonusRow mapToBonusRow(BonusesInDocument bonusesInDocument) {
        return new BonusRow(
                bonusesInDocument.getStartDate(),
                bonusesInDocument.getTypeOfIncrement(),
                bonusesInDocument.getValue().intValue(),
                bonusesInDocument.getTextOperation(),
                bonusesInDocument.getOrderId()
        );
    }
}
