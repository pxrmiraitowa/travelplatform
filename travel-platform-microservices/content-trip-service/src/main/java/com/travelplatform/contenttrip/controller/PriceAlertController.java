package com.travelplatform.contenttrip.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.contenttrip.dto.pricealert.PriceAlertCreateRequest;
import com.travelplatform.contenttrip.service.pricealert.PriceAlertService;
import com.travelplatform.contenttrip.vo.pricealert.PriceAlertVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/price-alerts")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    public PriceAlertController(PriceAlertService priceAlertService) {
        this.priceAlertService = priceAlertService;
    }

    @Operation(summary = "List current user price alerts")
    @GetMapping
    public Result<List<PriceAlertVO>> listAlerts() {
        return Result.success(priceAlertService.listCurrentUserAlerts());
    }

    @Operation(summary = "Create price alert")
    @PostMapping
    public Result<PriceAlertVO> createAlert(@Valid @RequestBody PriceAlertCreateRequest request) {
        return Result.success(priceAlertService.createAlert(request));
    }

    @Operation(summary = "Delete price alert")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAlert(@PathVariable Long id) {
        priceAlertService.deleteAlert(id);
        return Result.success();
    }
}
