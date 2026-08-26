package com.travelplatform.product.service.flight.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.product.dto.flight.FlightQueryRequest;
import com.travelplatform.product.entity.Flight;
import com.travelplatform.product.mapper.FlightMapper;
import com.travelplatform.product.service.flight.FlightService;
import com.travelplatform.product.vo.flight.FlightDetailVO;
import com.travelplatform.product.vo.flight.FlightListItemVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FlightServiceImpl implements FlightService {

    private final FlightMapper flightMapper;

    public FlightServiceImpl(FlightMapper flightMapper) {
        this.flightMapper = flightMapper;
    }

    @Override
    public PageResult<FlightListItemVO> searchFlights(FlightQueryRequest request) {
        FlightQueryRequest query = request == null ? new FlightQueryRequest() : request;
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 50);

        LambdaQueryWrapper<Flight> queryWrapper = new LambdaQueryWrapper<Flight>()
                .eq(Flight::getStatus, 1)
                .like(StringUtils.hasText(query.getDepartureCity()), Flight::getDepartureCity, query.getDepartureCity())
                .like(StringUtils.hasText(query.getArrivalCity()), Flight::getArrivalCity, query.getArrivalCity())
                .ge(query.getMinPrice() != null, Flight::getPrice, query.getMinPrice())
                .le(query.getMaxPrice() != null, Flight::getPrice, query.getMaxPrice())
                .orderByAsc(Flight::getDepartureTime)
                .orderByAsc(Flight::getPrice);

        if (query.getDepartureDate() != null) {
            LocalDateTime start = query.getDepartureDate().atStartOfDay();
            LocalDateTime end = query.getDepartureDate().plusDays(1).atStartOfDay();
            queryWrapper.ge(Flight::getDepartureTime, start).lt(Flight::getDepartureTime, end);
        }

        List<Flight> filteredFlights = flightMapper.selectList(queryWrapper)
                .stream()
                .filter(flight -> matchesTimeRange(flight, query))
                .toList();

        int total = filteredFlights.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);

        PageResult<FlightListItemVO> result = new PageResult<>();
        result.setRecords(filteredFlights.subList(fromIndex, toIndex).stream().map(this::toListItemVO).toList());
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    @Override
    public FlightDetailVO getFlightDetail(Long id) {
        Flight flight = getFlightOrThrow(id);
        FlightDetailVO vo = new FlightDetailVO();
        copyBaseFlightInfo(flight, vo);
        vo.setBaggagePolicy(flight.getBaggagePolicy());
        vo.setRefundPolicy(flight.getRefundPolicy());
        vo.setStatus(flight.getStatus());
        return vo;
    }

    private boolean matchesTimeRange(Flight flight, FlightQueryRequest request) {
        if (request.getDepartureStartTime() != null
                && flight.getDepartureTime().toLocalTime().isBefore(request.getDepartureStartTime())) {
            return false;
        }
        if (request.getDepartureEndTime() != null
                && flight.getDepartureTime().toLocalTime().isAfter(request.getDepartureEndTime())) {
            return false;
        }
        return true;
    }

    private Flight getFlightOrThrow(Long id) {
        Flight flight = flightMapper.selectById(id);
        if (flight == null || !Integer.valueOf(1).equals(flight.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "航班不存在");
        }
        return flight;
    }

    private FlightListItemVO toListItemVO(Flight flight) {
        FlightListItemVO vo = new FlightListItemVO();
        copyBaseFlightInfo(flight, vo);
        return vo;
    }

    private void copyBaseFlightInfo(Flight flight, FlightListItemVO vo) {
        vo.setId(flight.getId());
        vo.setFlightNo(flight.getFlightNo());
        vo.setAirlineName(flight.getAirlineName());
        vo.setDepartureCity(flight.getDepartureCity());
        vo.setArrivalCity(flight.getArrivalCity());
        vo.setDepartureAirport(flight.getDepartureAirport());
        vo.setArrivalAirport(flight.getArrivalAirport());
        vo.setDepartureTime(flight.getDepartureTime());
        vo.setArrivalTime(flight.getArrivalTime());
        vo.setPrice(flight.getPrice());
        vo.setStock(flight.getStock());
        vo.setCabinClass(flight.getCabinClass());
    }
}
