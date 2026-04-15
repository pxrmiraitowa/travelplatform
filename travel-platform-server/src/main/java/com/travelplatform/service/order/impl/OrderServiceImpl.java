package com.travelplatform.service.order.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.constant.OrderBizTypeConstant;
import com.travelplatform.common.constant.OrderStatusConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.order.FlightOrderCreateRequest;
import com.travelplatform.dto.order.HotelOrderCreateRequest;
import com.travelplatform.dto.order.TourOrderCreateRequest;
import com.travelplatform.dto.order.TrainOrderCreateRequest;
import com.travelplatform.entity.Coupon;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.entity.OrderFlight;
import com.travelplatform.entity.OrderHotel;
import com.travelplatform.entity.OrderTour;
import com.travelplatform.entity.OrderTrain;
import com.travelplatform.entity.Orders;
import com.travelplatform.entity.Review;
import com.travelplatform.entity.TourPackage;
import com.travelplatform.entity.TrainTicket;
import com.travelplatform.entity.UserContact;
import com.travelplatform.mapper.CouponMapper;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.OrderFlightMapper;
import com.travelplatform.mapper.OrderHotelMapper;
import com.travelplatform.mapper.OrderTourMapper;
import com.travelplatform.mapper.OrderTrainMapper;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.ReviewMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.mapper.UserContactMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.order.OrderService;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.order.OrderDetailVO;
import com.travelplatform.vo.order.OrderFlightVO;
import com.travelplatform.vo.order.OrderHotelVO;
import com.travelplatform.vo.order.OrderListItemVO;
import com.travelplatform.vo.order.OrderTourVO;
import com.travelplatform.vo.order.OrderTrainVO;
import com.travelplatform.vo.review.ReviewVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrdersMapper ordersMapper;
    private final OrderFlightMapper orderFlightMapper;
    private final OrderTrainMapper orderTrainMapper;
    private final OrderHotelMapper orderHotelMapper;
    private final OrderTourMapper orderTourMapper;
    private final FlightMapper flightMapper;
    private final TrainTicketMapper trainTicketMapper;
    private final HotelMapper hotelMapper;
    private final HotelRoomMapper hotelRoomMapper;
    private final TourPackageMapper tourPackageMapper;
    private final UserContactMapper userContactMapper;
    private final CouponMapper couponMapper;
    private final ReviewMapper reviewMapper;

    public OrderServiceImpl(OrdersMapper ordersMapper,
                            OrderFlightMapper orderFlightMapper,
                            OrderTrainMapper orderTrainMapper,
                            OrderHotelMapper orderHotelMapper,
                            OrderTourMapper orderTourMapper,
                            FlightMapper flightMapper,
                            TrainTicketMapper trainTicketMapper,
                            HotelMapper hotelMapper,
                            HotelRoomMapper hotelRoomMapper,
                            TourPackageMapper tourPackageMapper,
                            UserContactMapper userContactMapper,
                            CouponMapper couponMapper,
                            ReviewMapper reviewMapper) {
        this.ordersMapper = ordersMapper;
        this.orderFlightMapper = orderFlightMapper;
        this.orderTrainMapper = orderTrainMapper;
        this.orderHotelMapper = orderHotelMapper;
        this.orderTourMapper = orderTourMapper;
        this.flightMapper = flightMapper;
        this.trainTicketMapper = trainTicketMapper;
        this.hotelMapper = hotelMapper;
        this.hotelRoomMapper = hotelRoomMapper;
        this.tourPackageMapper = tourPackageMapper;
        this.userContactMapper = userContactMapper;
        this.couponMapper = couponMapper;
        this.reviewMapper = reviewMapper;
    }

    @Override
    @Transactional
    public OrderDetailVO createFlightOrder(FlightOrderCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Flight flight = getAvailableFlight(request.getFlightId());
        if (!flight.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "航班已起飞，不能下单");
        }
        if (flight.getStock() == null || flight.getStock() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该航班余票不足");
        }

        UserContact contact = getOwnedContact(request.getContactId(), userId);
        PriceSettlement settlement = resolveSettlement(OrderBizTypeConstant.FLIGHT, flight.getPrice(), request.getCouponId());
        Orders order = createBaseOrder(userId, OrderBizTypeConstant.FLIGHT, flight.getId(), settlement,
                contact.getName(), contact.getPhone(), flight.getDepartureTime().toLocalDate(), request.getRemark(), "FL");

        OrderFlight orderFlight = new OrderFlight();
        orderFlight.setOrderId(order.getId());
        orderFlight.setFlightId(flight.getId());
        orderFlight.setFlightNo(flight.getFlightNo());
        orderFlight.setAirlineName(flight.getAirlineName());
        orderFlight.setDepartureCity(flight.getDepartureCity());
        orderFlight.setArrivalCity(flight.getArrivalCity());
        orderFlight.setDepartureAirport(flight.getDepartureAirport());
        orderFlight.setArrivalAirport(flight.getArrivalAirport());
        orderFlight.setDepartureTime(flight.getDepartureTime());
        orderFlight.setArrivalTime(flight.getArrivalTime());
        orderFlight.setPassengerName(contact.getName());
        orderFlight.setPassengerPhone(contact.getPhone());
        orderFlight.setPassengerIdCard(contact.getIdCard());
        orderFlight.setTicketPrice(flight.getPrice());
        orderFlight.setStatus(OrderStatusConstant.PENDING_PAYMENT);
        orderFlightMapper.insert(orderFlight);

        flight.setStock(flight.getStock() - 1);
        flightMapper.updateById(flight);
        return buildFlightOrderDetail(order, orderFlight, null);
    }

    @Override
    @Transactional
    public OrderDetailVO createTrainOrder(TrainOrderCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        TrainTicket trainTicket = getAvailableTrainTicket(request.getTrainTicketId());
        if (!trainTicket.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "车次已发车，不能下单");
        }
        UserContact contact = getOwnedContact(request.getContactId(), userId);

        BigDecimal seatPrice = resolveSeatPrice(trainTicket, request.getSeatType());
        if (seatPrice == null || seatPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前座位类型不可购买");
        }
        int stock = resolveSeatStock(trainTicket, request.getSeatType());
        if (stock <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前座位余票不足");
        }

        Orders order = createBaseOrder(userId, OrderBizTypeConstant.TRAIN, trainTicket.getId(),
                new PriceSettlement(seatPrice, BigDecimal.ZERO, seatPrice, null, null),
                contact.getName(), contact.getPhone(), trainTicket.getDepartureTime().toLocalDate(), request.getRemark(), "TR");

        OrderTrain orderTrain = new OrderTrain();
        orderTrain.setOrderId(order.getId());
        orderTrain.setTrainTicketId(trainTicket.getId());
        orderTrain.setTrainNo(trainTicket.getTrainNo());
        orderTrain.setTrainType(trainTicket.getTrainType());
        orderTrain.setDepartureCity(trainTicket.getDepartureCity());
        orderTrain.setArrivalCity(trainTicket.getArrivalCity());
        orderTrain.setDepartureStation(trainTicket.getDepartureStation());
        orderTrain.setArrivalStation(trainTicket.getArrivalStation());
        orderTrain.setDepartureTime(trainTicket.getDepartureTime());
        orderTrain.setArrivalTime(trainTicket.getArrivalTime());
        orderTrain.setSeatType(request.getSeatType());
        orderTrain.setSeatPrice(seatPrice);
        orderTrain.setPassengerName(contact.getName());
        orderTrain.setPassengerPhone(contact.getPhone());
        orderTrain.setPassengerIdCard(contact.getIdCard());
        orderTrain.setStatus(OrderStatusConstant.PENDING_PAYMENT);
        orderTrainMapper.insert(orderTrain);

        decreaseSeatStock(trainTicket, request.getSeatType());
        trainTicketMapper.updateById(trainTicket);
        return buildTrainOrderDetail(order, orderTrain, null);
    }

    @Override
    @Transactional
    public OrderDetailVO createHotelOrder(HotelOrderCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Hotel hotel = getAvailableHotel(request.getHotelId());
        HotelRoom hotelRoom = getAvailableHotelRoom(request.getHotelRoomId());
        if (!hotel.getId().equals(hotelRoom.getHotelId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "房型与酒店不匹配");
        }
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null
                || !request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "入住和离店日期不合法");
        }
        if (!request.getCheckInDate().isAfter(LocalDate.now().minusDays(1))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "入住日期不能早于今天");
        }
        if (hotelRoom.getStock() == null || hotelRoom.getStock() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前房型库存不足");
        }

        UserContact contact = getOwnedContact(request.getContactId(), userId);
        int nightCount = (int) ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = hotelRoom.getPrice().multiply(BigDecimal.valueOf(nightCount));
        PriceSettlement settlement = resolveSettlement(OrderBizTypeConstant.HOTEL, totalAmount, request.getCouponId());
        Orders order = createBaseOrder(userId, OrderBizTypeConstant.HOTEL, hotel.getId(), settlement,
                contact.getName(), contact.getPhone(), request.getCheckInDate(), request.getRemark(), "HO");

        OrderHotel orderHotel = new OrderHotel();
        orderHotel.setOrderId(order.getId());
        orderHotel.setHotelId(hotel.getId());
        orderHotel.setHotelRoomId(hotelRoom.getId());
        orderHotel.setHotelName(hotel.getHotelName());
        orderHotel.setCity(hotel.getCity());
        orderHotel.setAddress(hotel.getAddress());
        orderHotel.setRoomName(hotelRoom.getRoomName());
        orderHotel.setBedType(hotelRoom.getBedType());
        orderHotel.setBreakfast(hotelRoom.getBreakfast());
        orderHotel.setCheckInDate(request.getCheckInDate());
        orderHotel.setCheckOutDate(request.getCheckOutDate());
        orderHotel.setGuestName(contact.getName());
        orderHotel.setGuestPhone(contact.getPhone());
        orderHotel.setGuestIdCard(contact.getIdCard());
        orderHotel.setRoomPrice(hotelRoom.getPrice());
        orderHotel.setNightCount(nightCount);
        orderHotel.setStatus(OrderStatusConstant.PENDING_PAYMENT);
        orderHotelMapper.insert(orderHotel);

        hotelRoom.setStock(hotelRoom.getStock() - 1);
        hotelRoomMapper.updateById(hotelRoom);
        return buildHotelOrderDetail(order, orderHotel, null);
    }

    @Override
    @Transactional
    public OrderDetailVO createTourOrder(TourOrderCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        TourPackage tourPackage = getAvailableTourPackage(request.getTourPackageId());
        if (request.getTravelDate() == null || !request.getTravelDate().isAfter(LocalDate.now().minusDays(1))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "出行日期不能早于今天");
        }
        if (!containsTravelDate(tourPackage.getTravelDates(), request.getTravelDate())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该产品不支持所选出行日期");
        }
        if (tourPackage.getStock() == null || tourPackage.getStock() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前产品库存不足");
        }

        UserContact contact = getOwnedContact(request.getContactId(), userId);
        PriceSettlement settlement = resolveSettlement(OrderBizTypeConstant.TOUR, tourPackage.getPrice(), request.getCouponId());
        Orders order = createBaseOrder(userId, OrderBizTypeConstant.TOUR, tourPackage.getId(), settlement,
                contact.getName(), contact.getPhone(), request.getTravelDate(), request.getRemark(), "TO");

        OrderTour orderTour = new OrderTour();
        orderTour.setOrderId(order.getId());
        orderTour.setTourPackageId(tourPackage.getId());
        orderTour.setPackageName(tourPackage.getPackageName());
        orderTour.setDestination(tourPackage.getDestination());
        orderTour.setDepartureCity(tourPackage.getDepartureCity());
        orderTour.setTravelDate(request.getTravelDate());
        orderTour.setDays(tourPackage.getDays());
        orderTour.setGuestName(contact.getName());
        orderTour.setGuestPhone(contact.getPhone());
        orderTour.setGuestIdCard(contact.getIdCard());
        orderTour.setPackagePrice(tourPackage.getPrice());
        orderTour.setStatus(OrderStatusConstant.PENDING_PAYMENT);
        orderTourMapper.insert(orderTour);

        tourPackage.setStock(tourPackage.getStock() - 1);
        tourPackageMapper.updateById(tourPackage);
        return buildTourOrderDetail(order, orderTour, null);
    }

    @Override
    public PageResult<OrderListItemVO> listCurrentUserOrders(String bizType, Integer status, Integer pageNum, Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .eq(StringUtils.hasText(bizType), Orders::getBizType, bizType)
                .eq(status != null, Orders::getOrderStatus, status)
                .orderByDesc(Orders::getId);

        Page<Orders> page = ordersMapper.selectPage(new Page<>(safePageNum, safePageSize), queryWrapper);
        List<Orders> orders = page.getRecords();
        List<Long> orderIds = orders.stream().map(Orders::getId).toList();
        Map<Long, OrderFlight> orderFlightMap = orderIds.isEmpty() ? Map.of() :
                orderFlightMapper.selectList(new LambdaQueryWrapper<OrderFlight>().in(OrderFlight::getOrderId, orderIds))
                        .stream().collect(Collectors.toMap(OrderFlight::getOrderId, Function.identity()));
        Map<Long, OrderTrain> orderTrainMap = orderIds.isEmpty() ? Map.of() :
                orderTrainMapper.selectList(new LambdaQueryWrapper<OrderTrain>().in(OrderTrain::getOrderId, orderIds))
                        .stream().collect(Collectors.toMap(OrderTrain::getOrderId, Function.identity()));
        Map<Long, OrderHotel> orderHotelMap = orderIds.isEmpty() ? Map.of() :
                orderHotelMapper.selectList(new LambdaQueryWrapper<OrderHotel>().in(OrderHotel::getOrderId, orderIds))
                        .stream().collect(Collectors.toMap(OrderHotel::getOrderId, Function.identity()));
        Map<Long, OrderTour> orderTourMap = orderIds.isEmpty() ? Map.of() :
                orderTourMapper.selectList(new LambdaQueryWrapper<OrderTour>().in(OrderTour::getOrderId, orderIds))
                        .stream().collect(Collectors.toMap(OrderTour::getOrderId, Function.identity()));
        Map<Long, Review> reviewMap = orderIds.isEmpty() ? Map.of() :
                reviewMapper.selectList(new LambdaQueryWrapper<Review>().in(Review::getOrderId, orderIds))
                        .stream().collect(Collectors.toMap(Review::getOrderId, Function.identity()));

        PageResult<OrderListItemVO> result = new PageResult<>();
        result.setRecords(orders.stream()
                .map(order -> toOrderListItemVO(order, orderFlightMap.get(order.getId()), orderTrainMap.get(order.getId()),
                        orderHotelMap.get(order.getId()), orderTourMap.get(order.getId()), reviewMap.get(order.getId())))
                .toList());
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }

    @Override
    public OrderDetailVO getCurrentUserOrderDetail(Long id) {
        Orders order = getOwnedOrder(id);
        if (OrderBizTypeConstant.FLIGHT.equals(order.getBizType())) {
            return buildFlightOrderDetail(order, getOrderFlight(order.getId()), getReview(order.getId()));
        }
        if (OrderBizTypeConstant.TRAIN.equals(order.getBizType())) {
            return buildTrainOrderDetail(order, getOrderTrain(order.getId()), getReview(order.getId()));
        }
        if (OrderBizTypeConstant.HOTEL.equals(order.getBizType())) {
            return buildHotelOrderDetail(order, getOrderHotel(order.getId()), getReview(order.getId()));
        }
        if (OrderBizTypeConstant.TOUR.equals(order.getBizType())) {
            return buildTourOrderDetail(order, getOrderTour(order.getId()), getReview(order.getId()));
        }
        throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单类型不存在");
    }

    @Override
    @Transactional
    public void cancelCurrentUserOrder(Long id) {
        Orders order = getOwnedOrder(id);
        if (!Objects.equals(order.getOrderStatus(), OrderStatusConstant.PENDING_PAYMENT)
                && !Objects.equals(order.getOrderStatus(), OrderStatusConstant.PAID_PENDING_TRAVEL)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前订单状态不允许取消");
        }

        if (OrderBizTypeConstant.FLIGHT.equals(order.getBizType())) {
            OrderFlight orderFlight = getOrderFlight(order.getId());
            if (!orderFlight.getDepartureTime().isAfter(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "航班已起飞，不能取消订单");
            }
            order.setOrderStatus(OrderStatusConstant.CANCELLED);
            ordersMapper.updateById(order);
            orderFlight.setStatus(OrderStatusConstant.CANCELLED);
            orderFlightMapper.updateById(orderFlight);
            Flight flight = flightMapper.selectById(orderFlight.getFlightId());
            if (flight != null) {
                flight.setStock((flight.getStock() == null ? 0 : flight.getStock()) + 1);
                flightMapper.updateById(flight);
            }
            return;
        }

        if (OrderBizTypeConstant.TRAIN.equals(order.getBizType())) {
            OrderTrain orderTrain = getOrderTrain(order.getId());
            if (!orderTrain.getDepartureTime().isAfter(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "车次已发车，不能取消订单");
            }
            order.setOrderStatus(OrderStatusConstant.CANCELLED);
            ordersMapper.updateById(order);
            orderTrain.setStatus(OrderStatusConstant.CANCELLED);
            orderTrainMapper.updateById(orderTrain);
            TrainTicket trainTicket = trainTicketMapper.selectById(orderTrain.getTrainTicketId());
            if (trainTicket != null) {
                increaseSeatStock(trainTicket, orderTrain.getSeatType());
                trainTicketMapper.updateById(trainTicket);
            }
            return;
        }

        if (OrderBizTypeConstant.HOTEL.equals(order.getBizType())) {
            OrderHotel orderHotel = getOrderHotel(order.getId());
            if (!orderHotel.getCheckInDate().isAfter(LocalDate.now().minusDays(1))) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已到入住日期，不能取消订单");
            }
            order.setOrderStatus(OrderStatusConstant.CANCELLED);
            ordersMapper.updateById(order);
            orderHotel.setStatus(OrderStatusConstant.CANCELLED);
            orderHotelMapper.updateById(orderHotel);
            HotelRoom hotelRoom = hotelRoomMapper.selectById(orderHotel.getHotelRoomId());
            if (hotelRoom != null) {
                hotelRoom.setStock((hotelRoom.getStock() == null ? 0 : hotelRoom.getStock()) + 1);
                hotelRoomMapper.updateById(hotelRoom);
            }
            return;
        }

        if (OrderBizTypeConstant.TOUR.equals(order.getBizType())) {
            OrderTour orderTour = getOrderTour(order.getId());
            if (!orderTour.getTravelDate().isAfter(LocalDate.now().minusDays(1))) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已到出行日期，不能取消订单");
            }
            order.setOrderStatus(OrderStatusConstant.CANCELLED);
            ordersMapper.updateById(order);
            orderTour.setStatus(OrderStatusConstant.CANCELLED);
            orderTourMapper.updateById(orderTour);
            TourPackage tourPackage = tourPackageMapper.selectById(orderTour.getTourPackageId());
            if (tourPackage != null) {
                tourPackage.setStock((tourPackage.getStock() == null ? 0 : tourPackage.getStock()) + 1);
                tourPackageMapper.updateById(tourPackage);
            }
            return;
        }

        throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单类型不存在");
    }

    private Orders createBaseOrder(Long userId, String bizType, Long bizId, PriceSettlement settlement,
                                   String contactName, String contactPhone, LocalDate travelDate,
                                   String remark, String prefix) {
        Orders order = new Orders();
        order.setOrderNo(buildOrderNo(prefix, userId));
        order.setUserId(userId);
        order.setBizType(bizType);
        order.setBizId(bizId);
        order.setOrderStatus(OrderStatusConstant.PENDING_PAYMENT);
        order.setOriginalAmount(settlement.originalAmount());
        order.setDiscountAmount(settlement.discountAmount());
        order.setTotalAmount(settlement.payableAmount());
        order.setCouponId(settlement.couponId());
        order.setCouponName(settlement.couponName());
        order.setContactName(contactName);
        order.setContactPhone(contactPhone);
        order.setTravelDate(travelDate);
        order.setRemark(remark);
        ordersMapper.insert(order);
        return order;
    }

    private PriceSettlement resolveSettlement(String bizType, BigDecimal originalAmount, Long couponId) {
        if (couponId == null) {
            return new PriceSettlement(originalAmount, BigDecimal.ZERO, originalAmount, null, null);
        }
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !Integer.valueOf(1).equals(coupon.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "优惠券不存在或不可用");
        }
        if (!Objects.equals(resolveCouponProductType(bizType), coupon.getProductType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "优惠券不适用于当前订单");
        }
        LocalDateTime now = LocalDateTime.now();
        if ((coupon.getStartTime() != null && coupon.getStartTime().isAfter(now))
                || (coupon.getEndTime() != null && coupon.getEndTime().isBefore(now))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "优惠券不在有效期内");
        }
        BigDecimal thresholdAmount = coupon.getThresholdAmount() == null ? BigDecimal.ZERO : coupon.getThresholdAmount();
        if (originalAmount.compareTo(thresholdAmount) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单金额未达到优惠券使用门槛");
        }
        BigDecimal discountAmount = coupon.getDiscountAmount() == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        BigDecimal payableAmount = originalAmount.subtract(discountAmount);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO;
        }
        return new PriceSettlement(originalAmount, discountAmount, payableAmount, coupon.getId(), coupon.getCouponName());
    }

    private String resolveCouponProductType(String bizType) {
        if (OrderBizTypeConstant.FLIGHT.equals(bizType)) {
            return "FLIGHT";
        }
        if (OrderBizTypeConstant.HOTEL.equals(bizType)) {
            return "HOTEL";
        }
        if (OrderBizTypeConstant.TOUR.equals(bizType)) {
            return "TOUR";
        }
        return bizType;
    }

    private String buildOrderNo(String prefix, Long userId) {
        return prefix + ORDER_NO_FORMATTER.format(LocalDateTime.now()) + String.format("%04d", userId % 10000);
    }

    private Flight getAvailableFlight(Long id) {
        Flight flight = flightMapper.selectById(id);
        if (flight == null || !Integer.valueOf(1).equals(flight.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "航班不存在");
        }
        return flight;
    }

    private TrainTicket getAvailableTrainTicket(Long id) {
        TrainTicket trainTicket = trainTicketMapper.selectById(id);
        if (trainTicket == null || !Integer.valueOf(1).equals(trainTicket.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "车次不存在");
        }
        return trainTicket;
    }

    private Hotel getAvailableHotel(Long id) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null || !Integer.valueOf(1).equals(hotel.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "酒店不存在");
        }
        return hotel;
    }

    private HotelRoom getAvailableHotelRoom(Long id) {
        HotelRoom room = hotelRoomMapper.selectById(id);
        if (room == null || !Integer.valueOf(1).equals(room.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "房型不存在");
        }
        return room;
    }

    private TourPackage getAvailableTourPackage(Long id) {
        TourPackage tourPackage = tourPackageMapper.selectById(id);
        if (tourPackage == null || !Integer.valueOf(1).equals(tourPackage.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "旅游产品不存在");
        }
        return tourPackage;
    }

    private UserContact getOwnedContact(Long contactId, Long userId) {
        UserContact contact = userContactMapper.selectById(contactId);
        if (contact == null || !userId.equals(contact.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "入住人不存在");
        }
        return contact;
    }

    private Orders getOwnedOrder(Long orderId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return order;
    }

    private OrderFlight getOrderFlight(Long orderId) {
        OrderFlight orderFlight = orderFlightMapper.selectOne(new LambdaQueryWrapper<OrderFlight>()
                .eq(OrderFlight::getOrderId, orderId)
                .last("limit 1"));
        if (orderFlight == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单明细不存在");
        }
        return orderFlight;
    }

    private OrderTrain getOrderTrain(Long orderId) {
        OrderTrain orderTrain = orderTrainMapper.selectOne(new LambdaQueryWrapper<OrderTrain>()
                .eq(OrderTrain::getOrderId, orderId)
                .last("limit 1"));
        if (orderTrain == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单明细不存在");
        }
        return orderTrain;
    }

    private OrderHotel getOrderHotel(Long orderId) {
        OrderHotel orderHotel = orderHotelMapper.selectOne(new LambdaQueryWrapper<OrderHotel>()
                .eq(OrderHotel::getOrderId, orderId)
                .last("limit 1"));
        if (orderHotel == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单明细不存在");
        }
        return orderHotel;
    }

    private OrderTour getOrderTour(Long orderId) {
        OrderTour orderTour = orderTourMapper.selectOne(new LambdaQueryWrapper<OrderTour>()
                .eq(OrderTour::getOrderId, orderId)
                .last("limit 1"));
        if (orderTour == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单明细不存在");
        }
        return orderTour;
    }

    private boolean containsTravelDate(String rawDates, LocalDate travelDate) {
        if (!StringUtils.hasText(rawDates)) {
            return false;
        }
        String target = travelDate.toString();
        return List.of(rawDates.split(",")).stream().map(String::trim).anyMatch(target::equals);
    }

    private BigDecimal resolveSeatPrice(TrainTicket ticket, String seatType) {
        return switch (seatType) {
            case "商务座" -> ticket.getBusinessPrice();
            case "一等座" -> ticket.getFirstClassPrice();
            case "二等座" -> ticket.getSecondClassPrice();
            default -> null;
        };
    }

    private int resolveSeatStock(TrainTicket ticket, String seatType) {
        return switch (seatType) {
            case "商务座" -> safe(ticket.getBusinessStock());
            case "一等座" -> safe(ticket.getFirstClassStock());
            case "二等座" -> safe(ticket.getSecondClassStock());
            default -> 0;
        };
    }

    private void decreaseSeatStock(TrainTicket ticket, String seatType) {
        switch (seatType) {
            case "商务座" -> ticket.setBusinessStock(safe(ticket.getBusinessStock()) - 1);
            case "一等座" -> ticket.setFirstClassStock(safe(ticket.getFirstClassStock()) - 1);
            case "二等座" -> ticket.setSecondClassStock(safe(ticket.getSecondClassStock()) - 1);
            default -> throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "座位类型不支持");
        }
    }

    private void increaseSeatStock(TrainTicket ticket, String seatType) {
        switch (seatType) {
            case "商务座" -> ticket.setBusinessStock(safe(ticket.getBusinessStock()) + 1);
            case "一等座" -> ticket.setFirstClassStock(safe(ticket.getFirstClassStock()) + 1);
            case "二等座" -> ticket.setSecondClassStock(safe(ticket.getSecondClassStock()) + 1);
            default -> throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "座位类型不支持");
        }
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private OrderListItemVO toOrderListItemVO(Orders order,
                                              OrderFlight orderFlight,
                                              OrderTrain orderTrain,
                                              OrderHotel orderHotel,
                                              OrderTour orderTour,
                                              Review review) {
        OrderListItemVO vo = new OrderListItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBizType(order.getBizType());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setTravelDate(order.getTravelDate());
        vo.setCreateTime(order.getCreateTime());

        if (orderFlight != null) {
            vo.setSummaryTitle(orderFlight.getDepartureCity() + " -> " + orderFlight.getArrivalCity());
            vo.setSummarySubtitle(orderFlight.getFlightNo() + " | " + orderFlight.getAirlineName());
        } else if (orderTrain != null) {
            vo.setSummaryTitle(orderTrain.getDepartureCity() + " -> " + orderTrain.getArrivalCity());
            vo.setSummarySubtitle(orderTrain.getTrainNo() + " | " + orderTrain.getSeatType());
        } else if (orderHotel != null) {
            vo.setSummaryTitle(orderHotel.getHotelName());
            vo.setSummarySubtitle(orderHotel.getRoomName() + " | " + orderHotel.getCheckInDate() + "入住");
        } else if (orderTour != null) {
            vo.setSummaryTitle(orderTour.getPackageName());
            vo.setSummarySubtitle(orderTour.getDestination() + " | " + orderTour.getTravelDate() + "出发");
        } else {
            vo.setSummaryTitle("订单信息");
            vo.setSummarySubtitle(order.getBizType());
        }
        vo.setReviewed(review != null);
        vo.setReviewId(review == null ? null : review.getId());
        return vo;
    }

    private OrderDetailVO buildFlightOrderDetail(Orders order, OrderFlight orderFlight, Review review) {
        OrderDetailVO detailVO = buildBaseOrderDetail(order, review);
        detailVO.setFlightInfo(toOrderFlightVO(orderFlight));
        detailVO.setTrainInfo(null);
        detailVO.setHotelInfo(null);
        detailVO.setTourInfo(null);
        return detailVO;
    }

    private OrderDetailVO buildTrainOrderDetail(Orders order, OrderTrain orderTrain, Review review) {
        OrderDetailVO detailVO = buildBaseOrderDetail(order, review);
        detailVO.setFlightInfo(null);
        detailVO.setTrainInfo(toOrderTrainVO(orderTrain));
        detailVO.setHotelInfo(null);
        detailVO.setTourInfo(null);
        return detailVO;
    }

    private OrderDetailVO buildHotelOrderDetail(Orders order, OrderHotel orderHotel, Review review) {
        OrderDetailVO detailVO = buildBaseOrderDetail(order, review);
        detailVO.setFlightInfo(null);
        detailVO.setTrainInfo(null);
        detailVO.setHotelInfo(toOrderHotelVO(orderHotel));
        detailVO.setTourInfo(null);
        return detailVO;
    }

    private OrderDetailVO buildTourOrderDetail(Orders order, OrderTour orderTour, Review review) {
        OrderDetailVO detailVO = buildBaseOrderDetail(order, review);
        detailVO.setFlightInfo(null);
        detailVO.setTrainInfo(null);
        detailVO.setHotelInfo(null);
        detailVO.setTourInfo(toOrderTourVO(orderTour));
        return detailVO;
    }

    private OrderDetailVO buildBaseOrderDetail(Orders order, Review review) {
        OrderDetailVO detailVO = new OrderDetailVO();
        detailVO.setId(order.getId());
        detailVO.setOrderNo(order.getOrderNo());
        detailVO.setBizType(order.getBizType());
        detailVO.setBizId(order.getBizId());
        detailVO.setOrderStatus(order.getOrderStatus());
        detailVO.setOriginalAmount(order.getOriginalAmount());
        detailVO.setDiscountAmount(order.getDiscountAmount());
        detailVO.setTotalAmount(order.getTotalAmount());
        detailVO.setCouponName(order.getCouponName());
        detailVO.setContactName(order.getContactName());
        detailVO.setContactPhone(order.getContactPhone());
        detailVO.setTravelDate(order.getTravelDate());
        detailVO.setRemark(order.getRemark());
        detailVO.setCreateTime(order.getCreateTime());
        detailVO.setReviewed(review != null);
        detailVO.setReviewInfo(toReviewVO(review));
        return detailVO;
    }

    private OrderFlightVO toOrderFlightVO(OrderFlight orderFlight) {
        OrderFlightVO vo = new OrderFlightVO();
        vo.setFlightId(orderFlight.getFlightId());
        vo.setFlightNo(orderFlight.getFlightNo());
        vo.setAirlineName(orderFlight.getAirlineName());
        vo.setDepartureCity(orderFlight.getDepartureCity());
        vo.setArrivalCity(orderFlight.getArrivalCity());
        vo.setDepartureAirport(orderFlight.getDepartureAirport());
        vo.setArrivalAirport(orderFlight.getArrivalAirport());
        vo.setDepartureTime(orderFlight.getDepartureTime());
        vo.setArrivalTime(orderFlight.getArrivalTime());
        vo.setPassengerName(orderFlight.getPassengerName());
        vo.setPassengerPhone(orderFlight.getPassengerPhone());
        vo.setPassengerIdCard(orderFlight.getPassengerIdCard());
        vo.setTicketPrice(orderFlight.getTicketPrice());
        vo.setStatus(orderFlight.getStatus());
        return vo;
    }

    private OrderTrainVO toOrderTrainVO(OrderTrain orderTrain) {
        OrderTrainVO vo = new OrderTrainVO();
        vo.setTrainTicketId(orderTrain.getTrainTicketId());
        vo.setTrainNo(orderTrain.getTrainNo());
        vo.setTrainType(orderTrain.getTrainType());
        vo.setDepartureCity(orderTrain.getDepartureCity());
        vo.setArrivalCity(orderTrain.getArrivalCity());
        vo.setDepartureStation(orderTrain.getDepartureStation());
        vo.setArrivalStation(orderTrain.getArrivalStation());
        vo.setDepartureTime(orderTrain.getDepartureTime());
        vo.setArrivalTime(orderTrain.getArrivalTime());
        vo.setSeatType(orderTrain.getSeatType());
        vo.setSeatPrice(orderTrain.getSeatPrice());
        vo.setPassengerName(orderTrain.getPassengerName());
        vo.setPassengerPhone(orderTrain.getPassengerPhone());
        vo.setPassengerIdCard(orderTrain.getPassengerIdCard());
        vo.setStatus(orderTrain.getStatus());
        return vo;
    }

    private OrderHotelVO toOrderHotelVO(OrderHotel orderHotel) {
        OrderHotelVO vo = new OrderHotelVO();
        vo.setHotelId(orderHotel.getHotelId());
        vo.setHotelRoomId(orderHotel.getHotelRoomId());
        vo.setHotelName(orderHotel.getHotelName());
        vo.setCity(orderHotel.getCity());
        vo.setAddress(orderHotel.getAddress());
        vo.setRoomName(orderHotel.getRoomName());
        vo.setBedType(orderHotel.getBedType());
        vo.setBreakfast(orderHotel.getBreakfast());
        vo.setCheckInDate(orderHotel.getCheckInDate());
        vo.setCheckOutDate(orderHotel.getCheckOutDate());
        vo.setGuestName(orderHotel.getGuestName());
        vo.setGuestPhone(orderHotel.getGuestPhone());
        vo.setGuestIdCard(orderHotel.getGuestIdCard());
        vo.setRoomPrice(orderHotel.getRoomPrice());
        vo.setNightCount(orderHotel.getNightCount());
        vo.setStatus(orderHotel.getStatus());
        return vo;
    }

    private OrderTourVO toOrderTourVO(OrderTour orderTour) {
        OrderTourVO vo = new OrderTourVO();
        vo.setTourPackageId(orderTour.getTourPackageId());
        vo.setPackageName(orderTour.getPackageName());
        vo.setDestination(orderTour.getDestination());
        vo.setDepartureCity(orderTour.getDepartureCity());
        vo.setTravelDate(orderTour.getTravelDate());
        vo.setDays(orderTour.getDays());
        vo.setGuestName(orderTour.getGuestName());
        vo.setGuestPhone(orderTour.getGuestPhone());
        vo.setGuestIdCard(orderTour.getGuestIdCard());
        vo.setPackagePrice(orderTour.getPackagePrice());
        vo.setStatus(orderTour.getStatus());
        return vo;
    }

    private Review getReview(Long orderId) {
        return reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
                .last("limit 1"));
    }

    private ReviewVO toReviewVO(Review review) {
        if (review == null) {
            return null;
        }
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setBizType(review.getBizType());
        vo.setBizId(review.getBizId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    private record PriceSettlement(BigDecimal originalAmount,
                                   BigDecimal discountAmount,
                                   BigDecimal payableAmount,
                                   Long couponId,
                                   String couponName) {
    }
}
