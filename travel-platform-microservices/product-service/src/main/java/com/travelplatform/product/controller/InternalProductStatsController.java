package com.travelplatform.product.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.product.mapper.FlightMapper;
import com.travelplatform.product.mapper.HotelMapper;
import com.travelplatform.product.mapper.HotelRoomMapper;
import com.travelplatform.product.mapper.TourPackageMapper;
import com.travelplatform.product.mapper.TrainTicketMapper;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/products")
public class InternalProductStatsController {

    private final FlightMapper flightMapper;
    private final TrainTicketMapper trainTicketMapper;
    private final HotelMapper hotelMapper;
    private final HotelRoomMapper hotelRoomMapper;
    private final TourPackageMapper tourPackageMapper;

    public InternalProductStatsController(FlightMapper flightMapper,
                                          TrainTicketMapper trainTicketMapper,
                                          HotelMapper hotelMapper,
                                          HotelRoomMapper hotelRoomMapper,
                                          TourPackageMapper tourPackageMapper) {
        this.flightMapper = flightMapper;
        this.trainTicketMapper = trainTicketMapper;
        this.hotelMapper = hotelMapper;
        this.hotelRoomMapper = hotelRoomMapper;
        this.tourPackageMapper = tourPackageMapper;
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        long count = flightMapper.selectCount(null)
                + trainTicketMapper.selectCount(null)
                + hotelMapper.selectCount(null)
                + hotelRoomMapper.selectCount(null)
                + tourPackageMapper.selectCount(null);
        return Result.success(Map.of("productCount", count));
    }
}
