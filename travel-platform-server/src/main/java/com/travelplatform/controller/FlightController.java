package com.travelplatform.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.dto.flight.FlightQueryRequest;
import com.travelplatform.service.flight.FlightService;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.flight.FlightDetailVO;
import com.travelplatform.vo.flight.FlightListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @Operation(summary = "搜索航班")
    @GetMapping
    public Result<PageResult<FlightListItemVO>> searchFlights(FlightQueryRequest request) {
        return Result.success(flightService.searchFlights(request));
    }

    @Operation(summary = "查询航班详情")
    @GetMapping("/{id}")
    public Result<FlightDetailVO> getFlightDetail(@PathVariable Long id) {
        return Result.success(flightService.getFlightDetail(id));
    }
}
