package ru.stockmann.BonusesService.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1.0/live")
public class LiveController {
    @GetMapping("/")
    public String live(){
        return "OK";
    }
}
