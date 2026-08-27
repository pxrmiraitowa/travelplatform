package com.travelplatform.product.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.product.dto.hotel.HotelQueryRequest;
import com.travelplatform.product.service.hotel.HotelService;
import com.travelplatform.product.vo.hotel.HotelDetailVO;
import com.travelplatform.product.vo.hotel.HotelListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @Operation(summary = "搜索酒店")
    @GetMapping
    public Result<PageResult<HotelListItemVO>> searchHotels(HotelQueryRequest request) {
        return Result.success(hotelService.searchHotels(request));
    }

    @Operation(summary = "查询酒店详情")
    @GetMapping("/{id}")
    public Result<HotelDetailVO> getHotelDetail(@PathVariable Long id) {
        return Result.success(hotelService.getHotelDetail(id));
    }
}
