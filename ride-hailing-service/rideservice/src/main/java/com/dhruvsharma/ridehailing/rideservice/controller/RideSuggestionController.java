package com.dhruvsharma.ridehailing.rideservice.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RideSuggestionController {

    @GetMapping("/rides/suggestions")
    public List<String> getRideSuggestions(@RequestParam String source, @RequestParam String destination) {
        return new ArrayList<>(Arrays.asList("Mini", "Sedan", "SUV", "Luxury"));
    }
}
