package com.travelplatform.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.constant.OrderBizTypeConstant;
import com.travelplatform.common.constant.OrderStatusConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.admin.order.AdminOrderStatusUpdateRequest;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.entity.OrderFlight;
import com.travelplatform.entity.OrderHotel;
import com.travelplatform.entity.OrderTour;
import com.travelplatform.entity.OrderTrain;
import com.travelplatform.entity.Orders;
import com.travelplatform.entity.TourPackage;
import com.travelplatform.entity.TrainTicket;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.OrderFlightMapper;
import com.travelplatform.mapper.OrderHotelMapper;
import com.travelplatform.mapper.OrderTourMapper;
import com.travelplatform.mapper.OrderTrainMapper;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.service.admin.AdminOrderManageService;
import com.travelplatform.vo.admin.order.AdminOrderDetailVO;
import com.travelplatform.vo.admin.order.AdminOrderListItemVO;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.order.OrderFlightVO;
import com.travelplatform.vo.order.OrderHotelVO;
import com.travelplatform.vo.order.OrderTourVO;
import com.travelplatform.vo.order.OrderTrainVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminOrderManageServiceImpl implements AdminOrderManageService {

    private final OrdersMapper ordersMapper;
    private final OrderFlightMapper orderFlightMapper;
    private final OrderTrainMapper orderTrainMapper;
    private final OrderHotelMapper orderHotelMapper;
    private final OrderTourMapper orderTourMapper;
    private final UserMapper userMapper;
    private final FlightMapper flightMapper;
    private final TrainTicketMapper trainTicketMapper;
    private final HotelRoomMapper hotelRoomMapper;
    private final TourPackageMapper tourPackageMapper;

    public AdminOrderManageServiceImpl(OrdersMapper ordersMapper,
                                       OrderFlightMapper orderFlightMapper,
                                       OrderTrainMapper orderTrainMapper,
                                       OrderHotelMapper orderHotelMapper,
                                       OrderTourMapper orderTourMapper,
                                       UserMapper userMapper,
                                       FlightMapper flightMapper,
                                       TrainTicketMapper trainTicketMapper,
                                       HotelRoomMapper hotelRoomMapper,
                                       TourPackageMapper tourPackageMapper) {
        this.ordersMapper = ordersMapper;
        this.orderFlightMapper = orderFlightMapper;
        this.orderTrainMapper = orderTrainMapper;
        this.orderHotelMapper = orderHotelMapper;
        this.orderTourMapper = orderTourMapper;
        this.userMapper = userMapper;
        this.flightMapper = flightMapper;
        this.trainTicketMapper = trainTicketMapper;
        this.hotelRoomMapper = hotelRoomMapper;
        this.tourPackageMapper = tourPackageMapper;
    }

    @Override
    public PageResult<AdminOrderListItemVO> listOrders(String keyword, String bizType, Integer status, Integer pageNum, Integer pageSize) {
        List<AdminOrderListItemVO> records = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                        .eq(StringUtils.hasText(bizType), Orders::getBizType, bizType)
                        .eq(status != null, Orders::getOrderStatus, status)
                        .orderByDesc(Orders::getId))
                .stream()
                .map(this::toListVO)
                .filter(item -> matchKeyword(item, keyword))
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    @Override
    public AdminOrderDetailVO getOrderDetail(Long id) {
        Orders order = getOrder(id);
        User user = getUser(order.getUserId());
        AdminOrderDetailVO vo = new AdminOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBizType(order.getBizType());
        vo.setBizId(order.getBizId());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setTravelDate(order.getTravelDate());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());

        if (OrderBizTypeConstant.FLIGHT.equals(order.getBizType())) {
            vo.setFlightInfo(toFlightInfo(getOrderFlight(order.getId())));
        } else if (OrderBizTypeConstant.TRAIN.equals(order.getBizType())) {
            vo.setTrainInfo(toTrainInfo(getOrderTrain(order.getId())));
        } else if (OrderBizTypeConstant.HOTEL.equals(order.getBizType())) {
            vo.setHotelInfo(toHotelInfo(getOrderHotel(order.getId())));
        } else if (OrderBizTypeConstant.TOUR.equals(order.getBizType())) {
            vo.setTourInfo(toTourInfo(getOrderTour(order.getId())));
        }
        return vo;
    }

    @Override
    public void updateOrderStatus(Long id, AdminOrderStatusUpdateRequest request) {
        Orders order = getOrder(id);
        Integer targetStatus = request.getOrderStatus();
        if (!List.of(OrderStatusConstant.PENDING_PAYMENT, OrderStatusConstant.PAID_PENDING_TRAVEL,
                OrderStatusConstant.COMPLETED, OrderStatusConstant.CANCELLED).contains(targetStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单状态不合法");
        }
        if (OrderStatusConstant.CANCELLED == order.getOrderStatus() && targetStatus != OrderStatusConstant.CANCELLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已取消订单不支持恢复");
        }
        if (targetStatus == OrderStatusConstant.CANCELLED) {
            cancelOrder(id);
            return;
        }
        order.setOrderStatus(targetStatus);
        ordersMapper.updateById(order);
        syncDetailStatus(order, targetStatus);
    }

    @Override
    public void cancelOrder(Long id) {
        Orders order = getOrder(id);
        if (OrderStatusConstant.CANCELLED == order.getOrderStatus()) {
            return;
        }
        if (!List.of(OrderStatusConstant.PENDING_PAYMENT, OrderStatusConstant.PAID_PENDING_TRAVEL).contains(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前订单状态不允许取消");
        }
        order.setOrderStatus(OrderStatusConstant.CANCELLED);
        ordersMapper.updateById(order);

        if (OrderBizTypeConstant.FLIGHT.equals(order.getBizType())) {
            OrderFlight detail = getOrderFlight(order.getId());
            if (!detail.getDepartureTime().isAfter(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "航班已起飞，不能取消订单");
            }
            detail.setStatus(OrderStatusConstant.CANCELLED);
            orderFlightMapper.updateById(detail);
            Flight flight = flightMapper.selectById(detail.getFlightId());
            if (flight != null) {
                flight.setStock(safe(flight.getStock()) + 1);
                flightMapper.updateById(flight);
            }
            return;
        }

        if (OrderBizTypeConstant.TRAIN.equals(order.getBizType())) {
            OrderTrain detail = getOrderTrain(order.getId());
            if (!detail.getDepartureTime().isAfter(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "车次已发车，不能取消订单");
            }
            detail.setStatus(OrderStatusConstant.CANCELLED);
            orderTrainMapper.updateById(detail);
            TrainTicket train = trainTicketMapper.selectById(detail.getTrainTicketId());
            if (train != null) {
                increaseTrainStock(train, detail.getSeatType());
                trainTicketMapper.updateById(train);
            }
            return;
        }

        if (OrderBizTypeConstant.HOTEL.equals(order.getBizType())) {
            OrderHotel detail = getOrderHotel(order.getId());
            if (!detail.getCheckInDate().isAfter(LocalDate.now().minusDays(1))) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已到入住日期，不能取消订单");
            }
            detail.setStatus(OrderStatusConstant.CANCELLED);
            orderHotelMapper.updateById(detail);
            HotelRoom room = hotelRoomMapper.selectById(detail.getHotelRoomId());
            if (room != null) {
                room.setStock(safe(room.getStock()) + 1);
                hotelRoomMapper.updateById(room);
            }
            return;
        }

        if (OrderBizTypeConstant.TOUR.equals(order.getBizType())) {
            OrderTour detail = getOrderTour(order.getId());
            if (!detail.getTravelDate().isAfter(LocalDate.now().minusDays(1))) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已到出行日期，不能取消订单");
            }
            detail.setStatus(OrderStatusConstant.CANCELLED);
            orderTourMapper.updateById(detail);
            TourPackage tour = tourPackageMapper.selectById(detail.getTourPackageId());
            if (tour != null) {
                tour.setStock(safe(tour.getStock()) + 1);
                tourPackageMapper.updateById(tour);
            }
        }
    }

    private boolean matchKeyword(AdminOrderListItemVO item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return contains(item.getOrderNo(), normalized)
                || contains(item.getUsername(), normalized)
                || contains(item.getNickname(), normalized)
                || contains(item.getSummaryTitle(), normalized)
                || contains(item.getSummarySubtitle(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private Orders getOrder(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return order;
    }

    private User getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "下单用户不存在");
        }
        return user;
    }

    private OrderFlight getOrderFlight(Long orderId) {
        OrderFlight detail = orderFlightMapper.selectOne(new LambdaQueryWrapper<OrderFlight>()
                .eq(OrderFlight::getOrderId, orderId).last("limit 1"));
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单明细不存在");
        }
        return detail;
    }

    private OrderTrain getOrderTrain(Long orderId) {
        OrderTrain detail = orderTrainMapper.selectOne(new LambdaQueryWrapper<OrderTrain>()
                .eq(OrderTrain::getOrderId, orderId).last("limit 1"));
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单明细不存在");
        }
        return detail;
    }

    private OrderHotel getOrderHotel(Long orderId) {
        OrderHotel detail = orderHotelMapper.selectOne(new LambdaQueryWrapper<OrderHotel>()
                .eq(OrderHotel::getOrderId, orderId).last("limit 1"));
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单明细不存在");
        }
        return detail;
    }

    private OrderTour getOrderTour(Long orderId) {
        OrderTour detail = orderTourMapper.selectOne(new LambdaQueryWrapper<OrderTour>()
                .eq(OrderTour::getOrderId, orderId).last("limit 1"));
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单明细不存在");
        }
        return detail;
    }

    private void syncDetailStatus(Orders order, Integer status) {
        if (OrderBizTypeConstant.FLIGHT.equals(order.getBizType())) {
            OrderFlight detail = getOrderFlight(order.getId());
            detail.setStatus(status);
            orderFlightMapper.updateById(detail);
        } else if (OrderBizTypeConstant.TRAIN.equals(order.getBizType())) {
            OrderTrain detail = getOrderTrain(order.getId());
            detail.setStatus(status);
            orderTrainMapper.updateById(detail);
        } else if (OrderBizTypeConstant.HOTEL.equals(order.getBizType())) {
            OrderHotel detail = getOrderHotel(order.getId());
            detail.setStatus(status);
            orderHotelMapper.updateById(detail);
        } else if (OrderBizTypeConstant.TOUR.equals(order.getBizType())) {
            OrderTour detail = getOrderTour(order.getId());
            detail.setStatus(status);
            orderTourMapper.updateById(detail);
        }
    }

    private void increaseTrainStock(TrainTicket train, String seatType) {
        switch (seatType) {
            case "商务座" -> train.setBusinessStock(safe(train.getBusinessStock()) + 1);
            case "一等座" -> train.setFirstClassStock(safe(train.getFirstClassStock()) + 1);
            case "二等座" -> train.setSecondClassStock(safe(train.getSecondClassStock()) + 1);
            default -> throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "座位类型不合法");
        }
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private AdminOrderListItemVO toListVO(Orders order) {
        User user = userMapper.selectById(order.getUserId());
        AdminOrderListItemVO vo = new AdminOrderListItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBizType(order.getBizType());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setTravelDate(order.getTravelDate());
        vo.setCreateTime(order.getCreateTime());
        vo.setUserId(order.getUserId());
        vo.setUsername(user == null ? "" : user.getUsername());
        vo.setNickname(user == null ? "" : user.getNickname());

        if (OrderBizTypeConstant.FLIGHT.equals(order.getBizType())) {
            OrderFlight detail = getOrderFlight(order.getId());
            vo.setSummaryTitle(detail.getDepartureCity() + " -> " + detail.getArrivalCity());
            vo.setSummarySubtitle(detail.getFlightNo() + " | " + detail.getAirlineName());
        } else if (OrderBizTypeConstant.TRAIN.equals(order.getBizType())) {
            OrderTrain detail = getOrderTrain(order.getId());
            vo.setSummaryTitle(detail.getDepartureCity() + " -> " + detail.getArrivalCity());
            vo.setSummarySubtitle(detail.getTrainNo() + " | " + detail.getSeatType());
        } else if (OrderBizTypeConstant.HOTEL.equals(order.getBizType())) {
            OrderHotel detail = getOrderHotel(order.getId());
            vo.setSummaryTitle(detail.getHotelName());
            vo.setSummarySubtitle(detail.getRoomName() + " | " + detail.getCheckInDate());
        } else if (OrderBizTypeConstant.TOUR.equals(order.getBizType())) {
            OrderTour detail = getOrderTour(order.getId());
            vo.setSummaryTitle(detail.getPackageName());
            vo.setSummarySubtitle(detail.getDestination() + " | " + detail.getTravelDate());
        }
        return vo;
    }

    private OrderFlightVO toFlightInfo(OrderFlight detail) {
        OrderFlightVO vo = new OrderFlightVO();
        vo.setFlightId(detail.getFlightId());
        vo.setFlightNo(detail.getFlightNo());
        vo.setAirlineName(detail.getAirlineName());
        vo.setDepartureCity(detail.getDepartureCity());
        vo.setArrivalCity(detail.getArrivalCity());
        vo.setDepartureAirport(detail.getDepartureAirport());
        vo.setArrivalAirport(detail.getArrivalAirport());
        vo.setDepartureTime(detail.getDepartureTime());
        vo.setArrivalTime(detail.getArrivalTime());
        vo.setPassengerName(detail.getPassengerName());
        vo.setPassengerPhone(detail.getPassengerPhone());
        vo.setPassengerIdCard(detail.getPassengerIdCard());
        vo.setTicketPrice(detail.getTicketPrice());
        vo.setStatus(detail.getStatus());
        return vo;
    }

    private OrderTrainVO toTrainInfo(OrderTrain detail) {
        OrderTrainVO vo = new OrderTrainVO();
        vo.setTrainTicketId(detail.getTrainTicketId());
        vo.setTrainNo(detail.getTrainNo());
        vo.setTrainType(detail.getTrainType());
        vo.setDepartureCity(detail.getDepartureCity());
        vo.setArrivalCity(detail.getArrivalCity());
        vo.setDepartureStation(detail.getDepartureStation());
        vo.setArrivalStation(detail.getArrivalStation());
        vo.setDepartureTime(detail.getDepartureTime());
        vo.setArrivalTime(detail.getArrivalTime());
        vo.setSeatType(detail.getSeatType());
        vo.setSeatPrice(detail.getSeatPrice());
        vo.setPassengerName(detail.getPassengerName());
        vo.setPassengerPhone(detail.getPassengerPhone());
        vo.setPassengerIdCard(detail.getPassengerIdCard());
        vo.setStatus(detail.getStatus());
        return vo;
    }

    private OrderHotelVO toHotelInfo(OrderHotel detail) {
        OrderHotelVO vo = new OrderHotelVO();
        vo.setHotelId(detail.getHotelId());
        vo.setHotelRoomId(detail.getHotelRoomId());
        vo.setHotelName(detail.getHotelName());
        vo.setCity(detail.getCity());
        vo.setAddress(detail.getAddress());
        vo.setRoomName(detail.getRoomName());
        vo.setBedType(detail.getBedType());
        vo.setBreakfast(detail.getBreakfast());
        vo.setCheckInDate(detail.getCheckInDate());
        vo.setCheckOutDate(detail.getCheckOutDate());
        vo.setGuestName(detail.getGuestName());
        vo.setGuestPhone(detail.getGuestPhone());
        vo.setGuestIdCard(detail.getGuestIdCard());
        vo.setRoomPrice(detail.getRoomPrice());
        vo.setNightCount(detail.getNightCount());
        vo.setStatus(detail.getStatus());
        return vo;
    }

    private OrderTourVO toTourInfo(OrderTour detail) {
        OrderTourVO vo = new OrderTourVO();
        vo.setTourPackageId(detail.getTourPackageId());
        vo.setPackageName(detail.getPackageName());
        vo.setDestination(detail.getDestination());
        vo.setDepartureCity(detail.getDepartureCity());
        vo.setTravelDate(detail.getTravelDate());
        vo.setDays(detail.getDays());
        vo.setGuestName(detail.getGuestName());
        vo.setGuestPhone(detail.getGuestPhone());
        vo.setGuestIdCard(detail.getGuestIdCard());
        vo.setPackagePrice(detail.getPackagePrice());
        vo.setStatus(detail.getStatus());
        return vo;
    }

    private <T> PageResult<T> paginate(List<T> source, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, source.size());
        int toIndex = Math.min(fromIndex + safePageSize, source.size());
        PageResult<T> result = new PageResult<>();
        result.setRecords(new ArrayList<>(source.subList(fromIndex, toIndex)));
        result.setTotal((long) source.size());
        result.setPageNum(safePageNum);
        result.setPageSize(safePageSize);
        return result;
    }
}
