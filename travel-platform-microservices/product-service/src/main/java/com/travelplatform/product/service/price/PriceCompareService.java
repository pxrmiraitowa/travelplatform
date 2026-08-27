package com.travelplatform.product.service.price;

import com.travelplatform.product.vo.price.PriceCompareVO;

public interface PriceCompareService {

    PriceCompareVO getHotelCompare(Long hotelId);

    PriceCompareVO getFlightCompare(Long flightId);

    PriceCompareVO getTourCompare(Long tourId);
}
