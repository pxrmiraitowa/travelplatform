package com.travelplatform.product.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.product.service.price.PriceCompareService;
import com.travelplatform.product.vo.price.PriceCompareVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/price-compare")
public class PriceCompareController {

    private final PriceCompareService priceCompareService;

    public PriceCompareController(PriceCompareService priceCompareService) {
        this.priceCompareService = priceCompareService;
    }

    @Operation(summary = "Hotel price compare")
    @GetMapping("/hotels/{id}")
    public Result<PriceCompareVO> getHotelCompare(@PathVariable Long id) {
        return Result.success(priceCompareService.getHotelCompare(id));
    }

    @Operation(summary = "Flight price compare")
    @GetMapping("/flights/{id}")
    public Result<PriceCompareVO> getFlightCompare(@PathVariable Long id) {
        return Result.success(priceCompareService.getFlightCompare(id));
    }

    @Operation(summary = "Tour price compare")
    @GetMapping("/tours/{id}")
    public Result<PriceCompareVO> getTourCompare(@PathVariable Long id) {
        return Result.success(priceCompareService.getTourCompare(id));
    }
}
