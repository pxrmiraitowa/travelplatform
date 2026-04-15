package com.travelplatform.service.flight;

import com.travelplatform.dto.flight.FlightQueryRequest;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.flight.FlightDetailVO;
import com.travelplatform.vo.flight.FlightListItemVO;

public interface FlightService {

    PageResult<FlightListItemVO> searchFlights(FlightQueryRequest request);

    FlightDetailVO getFlightDetail(Long id);
}
