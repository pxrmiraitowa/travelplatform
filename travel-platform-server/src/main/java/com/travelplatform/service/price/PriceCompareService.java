package com.travelplatform.service.price;

import com.travelplatform.vo.price.PriceCompareVO;

public interface PriceCompareService {

    PriceCompareVO getHotelCompare(Long hotelId);

    PriceCompareVO getFlightCompare(Long flightId);

    PriceCompareVO getTourCompare(Long tourId);
}
