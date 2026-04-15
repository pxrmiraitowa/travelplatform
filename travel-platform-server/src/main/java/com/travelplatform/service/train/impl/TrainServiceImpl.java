package com.travelplatform.service.train.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.train.TrainQueryRequest;
import com.travelplatform.entity.TrainTicket;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.service.train.TrainService;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.train.SeatOptionVO;
import com.travelplatform.vo.train.TrainDetailVO;
import com.travelplatform.vo.train.TrainListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrainServiceImpl implements TrainService {

    private final TrainTicketMapper trainTicketMapper;

    public TrainServiceImpl(TrainTicketMapper trainTicketMapper) {
        this.trainTicketMapper = trainTicketMapper;
    }

    @Override
    public PageResult<TrainListItemVO> searchTrains(TrainQueryRequest request) {
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : Math.min(request.getPageSize(), 50);

        LambdaQueryWrapper<TrainTicket> queryWrapper = new LambdaQueryWrapper<TrainTicket>()
                .eq(TrainTicket::getStatus, 1)
                .like(StringUtils.hasText(request.getDepartureCity()), TrainTicket::getDepartureCity, request.getDepartureCity())
                .like(StringUtils.hasText(request.getArrivalCity()), TrainTicket::getArrivalCity, request.getArrivalCity())
                .eq(StringUtils.hasText(request.getTrainType()), TrainTicket::getTrainType, request.getTrainType())
                .orderByAsc(TrainTicket::getDepartureTime);

        if (request.getTravelDate() != null) {
            LocalDateTime start = request.getTravelDate().atStartOfDay();
            LocalDateTime end = request.getTravelDate().plusDays(1).atStartOfDay();
            queryWrapper.ge(TrainTicket::getDepartureTime, start).lt(TrainTicket::getDepartureTime, end);
        }

        List<TrainTicket> filtered = trainTicketMapper.selectList(queryWrapper).stream()
                .filter(ticket -> matchPrice(ticket, request.getMinPrice(), request.getMaxPrice()))
                .toList();

        int total = filtered.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);

        PageResult<TrainListItemVO> result = new PageResult<>();
        result.setRecords(filtered.subList(fromIndex, toIndex).stream().map(this::toListItemVO).toList());
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    @Override
    public TrainDetailVO getTrainDetail(Long id) {
        TrainTicket ticket = getTrainOrThrow(id);
        TrainDetailVO vo = new TrainDetailVO();
        copyBaseInfo(ticket, vo);
        vo.setSeatOptions(buildSeatOptions(ticket));
        vo.setStatus(ticket.getStatus());
        return vo;
    }

    private boolean matchPrice(TrainTicket ticket, BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal targetPrice = resolveMinAvailablePrice(ticket);
        if (targetPrice == null) {
            return false;
        }
        if (minPrice != null && targetPrice.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && targetPrice.compareTo(maxPrice) > 0) {
            return false;
        }
        return true;
    }

    private TrainTicket getTrainOrThrow(Long id) {
        TrainTicket ticket = trainTicketMapper.selectById(id);
        if (ticket == null || !Integer.valueOf(1).equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "车次不存在");
        }
        return ticket;
    }

    private TrainListItemVO toListItemVO(TrainTicket ticket) {
        TrainListItemVO vo = new TrainListItemVO();
        copyBaseInfo(ticket, vo);
        return vo;
    }

    private void copyBaseInfo(TrainTicket ticket, TrainListItemVO vo) {
        vo.setId(ticket.getId());
        vo.setTrainNo(ticket.getTrainNo());
        vo.setTrainType(ticket.getTrainType());
        vo.setDepartureCity(ticket.getDepartureCity());
        vo.setArrivalCity(ticket.getArrivalCity());
        vo.setDepartureStation(ticket.getDepartureStation());
        vo.setArrivalStation(ticket.getArrivalStation());
        vo.setDepartureTime(ticket.getDepartureTime());
        vo.setArrivalTime(ticket.getArrivalTime());
        vo.setDurationMinutes(ticket.getDurationMinutes());
        vo.setMinPrice(resolveMinAvailablePrice(ticket));
        vo.setTotalStock(safe(ticket.getBusinessStock()) + safe(ticket.getFirstClassStock()) + safe(ticket.getSecondClassStock()));
    }

    private List<SeatOptionVO> buildSeatOptions(TrainTicket ticket) {
        List<SeatOptionVO> options = new ArrayList<>();
        options.add(buildSeatOption("商务座", ticket.getBusinessPrice(), ticket.getBusinessStock()));
        options.add(buildSeatOption("一等座", ticket.getFirstClassPrice(), ticket.getFirstClassStock()));
        options.add(buildSeatOption("二等座", ticket.getSecondClassPrice(), ticket.getSecondClassStock()));
        return options;
    }

    private SeatOptionVO buildSeatOption(String seatType, BigDecimal price, Integer stock) {
        SeatOptionVO vo = new SeatOptionVO();
        vo.setSeatType(seatType);
        vo.setPrice(price);
        vo.setStock(stock);
        vo.setAvailable(price != null && price.compareTo(BigDecimal.ZERO) > 0 && safe(stock) > 0);
        return vo;
    }

    private BigDecimal resolveMinAvailablePrice(TrainTicket ticket) {
        List<BigDecimal> prices = new ArrayList<>();
        collectPrice(prices, ticket.getBusinessPrice(), ticket.getBusinessStock());
        collectPrice(prices, ticket.getFirstClassPrice(), ticket.getFirstClassStock());
        collectPrice(prices, ticket.getSecondClassPrice(), ticket.getSecondClassStock());
        return prices.stream().min(BigDecimal::compareTo).orElse(null);
    }

    private void collectPrice(List<BigDecimal> prices, BigDecimal price, Integer stock) {
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0 && safe(stock) > 0) {
            prices.add(price);
        }
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
