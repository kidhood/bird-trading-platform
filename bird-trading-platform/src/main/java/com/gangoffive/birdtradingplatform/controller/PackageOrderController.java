package com.gangoffive.birdtradingplatform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangoffive.birdtradingplatform.dto.PackageOrderRequestDto;
import com.gangoffive.birdtradingplatform.service.PackageOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1")
@Slf4j
public class PackageOrderController {

    private final PackageOrderService packageOrderService;
    @PostMapping("/package-order")
    public ResponseEntity<?> getPackageOrder(
            @RequestBody PackageOrderRequestDto packageOrderRequestDto,
            @RequestParam(value = "paymentId", required = false) String paymentId,
            @RequestParam(value = "PayerID", required = false) String payerId
    ) {
        return packageOrderService.packageOrder(packageOrderRequestDto, paymentId, payerId);
    }

    @RequestMapping("/view-all-package-order")
    public ResponseEntity<?> viewAllPackageOrder() {
        return ResponseEntity.ok("");
    }
}
