package com.travelplatform.product.service.flight;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.product.dto.flight.FlightQueryRequest;
import com.travelplatform.product.vo.flight.FlightDetailVO;
import com.travelplatform.product.vo.flight.FlightListItemVO;

public interface FlightService {

    PageResult<FlightListItemVO> searchFlights(FlightQueryRequest request);

    FlightDetailVO getFlightDetail(Long id);
}
