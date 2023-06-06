package com.gangoffive.birdtradingplatform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class VersionController {

    @GetMapping("/")
    public String getDateRelease() {
        String date = "06/06/2023 21h24";
        String version = "0.0.9 SNAPSHOT";
        return date + " - version " + version + " - COPYRIGHT @c GANGOFFIVE - BS2ND";
    }
}
