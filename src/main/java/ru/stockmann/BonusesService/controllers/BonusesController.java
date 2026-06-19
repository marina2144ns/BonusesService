package ru.stockmann.BonusesService.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.stockmann.BonusesService.models.api.BonusesRequest;
import ru.stockmann.BonusesService.models.api.BonusesResponse;
import ru.stockmann.BonusesService.services.BonusesService;

import java.util.UUID;

@RestController
@RequestMapping("/v1.0/history")
public class BonusesController {

    @Autowired
    private BonusesService bonusesService;

    private static final Logger logger = LoggerFactory.getLogger(BonusesController.class);


/*    @PostMapping("/")
    public BonusesResponse searchBonuses(@RequestBody BonusesRequest request) {
        return bonusesService.searchBonuses(request);
    }*/
    @PostMapping("/")
    public ResponseEntity<Object> searchBonuses(@RequestBody BonusesRequest request) {
        try {
            // Проверка входных параметров
            validateInputParameters(request);

            // Ваш код обработки запроса
            BonusesResponse response = bonusesService.searchBonuses(request);

            // Вернуть успешный ответ с полем status
            return ResponseEntity.ok(new SuccessResponse(true, request.getUuid(), response));

        } catch (RuntimeException ex) {
            // Обработка вашего собственного исключения
            return ResponseEntity.ok(new ErrorResponse(false, request.getUuid(), 500, ex.getMessage()));
        } catch (Exception ex) {
            // Обработка других исключений
            return ResponseEntity.ok(new ErrorResponse(false, null, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
        }
    }

    // Класс для представления успешного ответа в JSON формате
    private static class SuccessResponse {
        private final boolean status;
        private final UUID uuid;
        private final Object data;

        public SuccessResponse(boolean status, UUID uuid, Object data) {
            this.status = status;
            this.uuid = uuid;
            this.data = data;
        }

        public boolean isStatus() {
            return status;
        }

        public UUID getUuid() {
            return uuid;
        }

        public Object getData() {
            return data;
        }
    }

    // Проверка входных параметров
    private void validateInputParameters(BonusesRequest request) {
        if (!isValidCardNumber(request.getCardNumber())) {
            throw new RuntimeException("Неверный формат номера карты");
        }
        if(request.getPage()<0){
            throw new RuntimeException("Номер страницы не может быть отрицательным");
        }
        if(request.getByPage()<0){
            throw new RuntimeException("Число строк на страницу не может быть отрицательным");
        }
    }
    Boolean isValidCardNumber(String cardNumber){
        if (cardNumber.length()!=16)
            return false;
        else
            return true;
    }

    // Класс для представления ошибки в JSON формате
    private static class ErrorResponse {
        private final boolean status;
        private final ErrorDetails error;

        public ErrorResponse(boolean status, UUID uuid, int code, String message) {
            this.status = status;
            this.error = new ErrorDetails(uuid, code, message);
        }

        public boolean isStatus() {
            return status;
        }

        public ErrorDetails getError() {
            return error;
        }
    }

    // Класс для представления деталей ошибки в JSON формате
    private static class ErrorDetails {
        private final UUID uuid;
        private final int code;
        private final String message;

        public ErrorDetails(UUID uuid, int code, String message) {
            this.uuid = uuid;
            this.code = code;
            this.message = message;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

}
