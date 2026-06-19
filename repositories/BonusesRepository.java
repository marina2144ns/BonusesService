package ru.stockmann.BonusesService.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.stockmann.BonusesService.models.BonusesInDocument;
import ru.stockmann.BonusesService.models.api.BonusesRequest;

public interface BonusesRepository extends JpaRepository<BonusesInDocument, Integer> {

    @Query("SELECT b FROM BonusesInDocument b WHERE " +
            "(b.cardNumber = :#{#request.cardNumber}) AND " +
            "((:#{#request.fromDate} IS NULL OR b.startDate >= :#{#request.fromDate}) AND " +
            "(:#{#request.toDate} IS NULL OR b.startDate <= :#{#request.toDate})) "+
            "ORDER BY b.startDate desc")
    Page<BonusesInDocument> searchBonuses(BonusesRequest request, Pageable pageable);
}

//         AND    "(b.textOperation<>'Подарочные бонусы ДР СТОКМАНН') "+