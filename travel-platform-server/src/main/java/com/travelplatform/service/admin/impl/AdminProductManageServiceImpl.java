package com.travelplatform.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.admin.product.AdminFlightSaveRequest;
import com.travelplatform.dto.admin.product.AdminHotelRoomSaveRequest;
import com.travelplatform.dto.admin.product.AdminHotelSaveRequest;
import com.travelplatform.dto.admin.product.AdminTourSaveRequest;
import com.travelplatform.dto.admin.product.AdminTrainSaveRequest;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.entity.TourPackage;
import com.travelplatform.entity.TrainTicket;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.service.admin.AdminProductManageService;
import com.travelplatform.vo.admin.product.AdminFlightVO;
import com.travelplatform.vo.admin.product.AdminHotelRoomVO;
import com.travelplatform.vo.admin.product.AdminHotelVO;
import com.travelplatform.vo.admin.product.AdminTourVO;
import com.travelplatform.vo.admin.product.AdminTrainVO;
import com.travelplatform.vo.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminProductManageServiceImpl implements AdminProductManageService {

    private final FlightMapper flightMapper;
    private final TrainTicketMapper trainTicketMapper;
    private final HotelMapper hotelMapper;
    private final HotelRoomMapper hotelRoomMapper;
    private final TourPackageMapper tourPackageMapper;

    public AdminProductManageServiceImpl(FlightMapper flightMapper,
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

    @Override
    public PageResult<AdminFlightVO> listFlights(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        List<AdminFlightVO> records = flightMapper.selectList(new LambdaQueryWrapper<Flight>()
                        .eq(status != null, Flight::getStatus, status)
                        .orderByDesc(Flight::getId))
                .stream()
                .filter(flight -> matchFlight(flight, keyword))
                .map(this::toFlightVO)
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    @Override
    public AdminFlightVO createFlight(AdminFlightSaveRequest request) {
        Flight flight = new Flight();
        applyFlight(flight, request);
        flightMapper.insert(flight);
        return toFlightVO(flightMapper.selectById(flight.getId()));
    }

    @Override
    public AdminFlightVO updateFlight(Long id, AdminFlightSaveRequest request) {
        Flight flight = getFlight(id);
        applyFlight(flight, request);
        flightMapper.updateById(flight);
        return toFlightVO(flightMapper.selectById(id));
    }

    @Override
    public void deleteFlight(Long id) {
        ensureExists(flightMapper.selectById(id), "航班不存在");
        flightMapper.deleteById(id);
    }

    @Override
    public PageResult<AdminTrainVO> listTrains(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        List<AdminTrainVO> records = trainTicketMapper.selectList(new LambdaQueryWrapper<TrainTicket>()
                        .eq(status != null, TrainTicket::getStatus, status)
                        .orderByDesc(TrainTicket::getId))
                .stream()
                .filter(train -> matchTrain(train, keyword))
                .map(this::toTrainVO)
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    @Override
    public AdminTrainVO createTrain(AdminTrainSaveRequest request) {
        TrainTicket train = new TrainTicket();
        applyTrain(train, request);
        trainTicketMapper.insert(train);
        return toTrainVO(trainTicketMapper.selectById(train.getId()));
    }

    @Override
    public AdminTrainVO updateTrain(Long id, AdminTrainSaveRequest request) {
        TrainTicket train = getTrain(id);
        applyTrain(train, request);
        trainTicketMapper.updateById(train);
        return toTrainVO(trainTicketMapper.selectById(id));
    }

    @Override
    public void deleteTrain(Long id) {
        ensureExists(trainTicketMapper.selectById(id), "车次不存在");
        trainTicketMapper.deleteById(id);
    }

    @Override
    public PageResult<AdminHotelVO> listHotels(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        List<AdminHotelVO> records = hotelMapper.selectList(new LambdaQueryWrapper<Hotel>()
                        .eq(status != null, Hotel::getStatus, status)
                        .orderByDesc(Hotel::getId))
                .stream()
                .filter(hotel -> matchHotel(hotel, keyword))
                .map(this::toHotelVO)
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    @Override
    public AdminHotelVO createHotel(AdminHotelSaveRequest request) {
        Hotel hotel = new Hotel();
        applyHotel(hotel, request);
        hotelMapper.insert(hotel);
        return toHotelVO(hotelMapper.selectById(hotel.getId()));
    }

    @Override
    public AdminHotelVO updateHotel(Long id, AdminHotelSaveRequest request) {
        Hotel hotel = getHotel(id);
        applyHotel(hotel, request);
        hotelMapper.updateById(hotel);
        return toHotelVO(hotelMapper.selectById(id));
    }

    @Override
    public void deleteHotel(Long id) {
        Hotel hotel = getHotel(id);
        long roomCount = hotelRoomMapper.selectCount(new LambdaQueryWrapper<HotelRoom>().eq(HotelRoom::getHotelId, hotel.getId()));
        if (roomCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先删除该酒店下的房型");
        }
        hotelMapper.deleteById(id);
    }

    @Override
    public PageResult<AdminHotelRoomVO> listHotelRooms(Long hotelId, String keyword, Integer status, Integer pageNum, Integer pageSize) {
        List<AdminHotelRoomVO> records = hotelRoomMapper.selectList(new LambdaQueryWrapper<HotelRoom>()
                        .eq(hotelId != null, HotelRoom::getHotelId, hotelId)
                        .eq(status != null, HotelRoom::getStatus, status)
                        .orderByDesc(HotelRoom::getId))
                .stream()
                .filter(room -> matchRoom(room, keyword))
                .map(this::toRoomVO)
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    @Override
    public AdminHotelRoomVO createHotelRoom(AdminHotelRoomSaveRequest request) {
        ensureExists(hotelMapper.selectById(request.getHotelId()), "酒店不存在");
        HotelRoom room = new HotelRoom();
        applyRoom(room, request);
        hotelRoomMapper.insert(room);
        return toRoomVO(hotelRoomMapper.selectById(room.getId()));
    }

    @Override
    public AdminHotelRoomVO updateHotelRoom(Long id, AdminHotelRoomSaveRequest request) {
        ensureExists(hotelMapper.selectById(request.getHotelId()), "酒店不存在");
        HotelRoom room = getRoom(id);
        applyRoom(room, request);
        hotelRoomMapper.updateById(room);
        return toRoomVO(hotelRoomMapper.selectById(id));
    }

    @Override
    public void deleteHotelRoom(Long id) {
        ensureExists(hotelRoomMapper.selectById(id), "房型不存在");
        hotelRoomMapper.deleteById(id);
    }

    @Override
    public PageResult<AdminTourVO> listTours(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        List<AdminTourVO> records = tourPackageMapper.selectList(new LambdaQueryWrapper<TourPackage>()
                        .eq(status != null, TourPackage::getStatus, status)
                        .orderByDesc(TourPackage::getId))
                .stream()
                .filter(tour -> matchTour(tour, keyword))
                .map(this::toTourVO)
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    @Override
    public AdminTourVO createTour(AdminTourSaveRequest request) {
        TourPackage tour = new TourPackage();
        applyTour(tour, request);
        tourPackageMapper.insert(tour);
        return toTourVO(tourPackageMapper.selectById(tour.getId()));
    }

    @Override
    public AdminTourVO updateTour(Long id, AdminTourSaveRequest request) {
        TourPackage tour = getTour(id);
        applyTour(tour, request);
        tourPackageMapper.updateById(tour);
        return toTourVO(tourPackageMapper.selectById(id));
    }

    @Override
    public void deleteTour(Long id) {
        ensureExists(tourPackageMapper.selectById(id), "旅游产品不存在");
        tourPackageMapper.deleteById(id);
    }

    private void applyFlight(Flight flight, AdminFlightSaveRequest request) {
        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "到达时间必须晚于起飞时间");
        }
        flight.setFlightNo(request.getFlightNo());
        flight.setAirlineName(request.getAirlineName());
        flight.setDepartureCity(request.getDepartureCity());
        flight.setArrivalCity(request.getArrivalCity());
        flight.setDepartureAirport(request.getDepartureAirport());
        flight.setArrivalAirport(request.getArrivalAirport());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setPrice(request.getPrice());
        flight.setStock(request.getStock());
        flight.setCabinClass(request.getCabinClass());
        flight.setBaggagePolicy(request.getBaggagePolicy());
        flight.setRefundPolicy(request.getRefundPolicy());
        flight.setStatus(request.getStatus());
    }

    private void applyTrain(TrainTicket train, AdminTrainSaveRequest request) {
        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "到达时间必须晚于出发时间");
        }
        train.setTrainNo(request.getTrainNo());
        train.setTrainType(request.getTrainType());
        train.setDepartureCity(request.getDepartureCity());
        train.setArrivalCity(request.getArrivalCity());
        train.setDepartureStation(request.getDepartureStation());
        train.setArrivalStation(request.getArrivalStation());
        train.setDepartureTime(request.getDepartureTime());
        train.setArrivalTime(request.getArrivalTime());
        train.setDurationMinutes(request.getDurationMinutes());
        train.setBusinessPrice(request.getBusinessPrice());
        train.setFirstClassPrice(request.getFirstClassPrice());
        train.setSecondClassPrice(request.getSecondClassPrice());
        train.setBusinessStock(request.getBusinessStock());
        train.setFirstClassStock(request.getFirstClassStock());
        train.setSecondClassStock(request.getSecondClassStock());
        train.setStatus(request.getStatus());
    }

    private void applyHotel(Hotel hotel, AdminHotelSaveRequest request) {
        hotel.setHotelName(request.getHotelName());
        hotel.setCity(request.getCity());
        hotel.setDistrict(request.getDistrict());
        hotel.setAddress(request.getAddress());
        hotel.setDescription(request.getDescription());
        hotel.setStarLevel(request.getStarLevel());
        hotel.setCoverImage(request.getCoverImage());
        hotel.setCheckInTime(request.getCheckInTime());
        hotel.setCheckOutTime(request.getCheckOutTime());
        hotel.setStatus(request.getStatus());
    }

    private void applyRoom(HotelRoom room, AdminHotelRoomSaveRequest request) {
        room.setHotelId(request.getHotelId());
        room.setRoomName(request.getRoomName());
        room.setBedType(request.getBedType());
        room.setBreakfast(request.getBreakfast());
        room.setRoomArea(request.getRoomArea());
        room.setGuestCount(request.getGuestCount());
        room.setPrice(request.getPrice());
        room.setStock(request.getStock());
        room.setCancelRule(request.getCancelRule());
        room.setStatus(request.getStatus());
    }

    private void applyTour(TourPackage tour, AdminTourSaveRequest request) {
        tour.setPackageName(request.getPackageName());
        tour.setDestination(request.getDestination());
        tour.setDepartureCity(request.getDepartureCity());
        tour.setDays(request.getDays());
        tour.setPrice(request.getPrice());
        tour.setStock(request.getStock());
        tour.setTravelDates(request.getTravelDates());
        tour.setDescription(request.getDescription());
        tour.setCoverImage(request.getCoverImage());
        tour.setStatus(request.getStatus());
    }

    private boolean matchFlight(Flight flight, String keyword) {
        return !StringUtils.hasText(keyword)
                || contains(flight.getFlightNo(), keyword)
                || contains(flight.getDepartureCity(), keyword)
                || contains(flight.getArrivalCity(), keyword)
                || contains(flight.getAirlineName(), keyword);
    }

    private boolean matchTrain(TrainTicket train, String keyword) {
        return !StringUtils.hasText(keyword)
                || contains(train.getTrainNo(), keyword)
                || contains(train.getDepartureCity(), keyword)
                || contains(train.getArrivalCity(), keyword)
                || contains(train.getDepartureStation(), keyword)
                || contains(train.getArrivalStation(), keyword);
    }

    private boolean matchHotel(Hotel hotel, String keyword) {
        return !StringUtils.hasText(keyword)
                || contains(hotel.getHotelName(), keyword)
                || contains(hotel.getCity(), keyword)
                || contains(hotel.getAddress(), keyword);
    }

    private boolean matchRoom(HotelRoom room, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        Hotel hotel = hotelMapper.selectById(room.getHotelId());
        return contains(room.getRoomName(), keyword)
                || contains(room.getBedType(), keyword)
                || contains(hotel == null ? null : hotel.getHotelName(), keyword);
    }

    private boolean matchTour(TourPackage tour, String keyword) {
        return !StringUtils.hasText(keyword)
                || contains(tour.getPackageName(), keyword)
                || contains(tour.getDestination(), keyword)
                || contains(tour.getDepartureCity(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private Flight getFlight(Long id) {
        return ensureExists(flightMapper.selectById(id), "航班不存在");
    }

    private TrainTicket getTrain(Long id) {
        return ensureExists(trainTicketMapper.selectById(id), "车次不存在");
    }

    private Hotel getHotel(Long id) {
        return ensureExists(hotelMapper.selectById(id), "酒店不存在");
    }

    private HotelRoom getRoom(Long id) {
        return ensureExists(hotelRoomMapper.selectById(id), "房型不存在");
    }

    private TourPackage getTour(Long id) {
        return ensureExists(tourPackageMapper.selectById(id), "旅游产品不存在");
    }

    private <T> T ensureExists(T value, String message) {
        if (value == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), message);
        }
        return value;
    }

    private AdminFlightVO toFlightVO(Flight flight) {
        AdminFlightVO vo = new AdminFlightVO();
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
        vo.setBaggagePolicy(flight.getBaggagePolicy());
        vo.setRefundPolicy(flight.getRefundPolicy());
        vo.setStatus(flight.getStatus());
        vo.setCreateTime(flight.getCreateTime());
        return vo;
    }

    private AdminTrainVO toTrainVO(TrainTicket train) {
        AdminTrainVO vo = new AdminTrainVO();
        vo.setId(train.getId());
        vo.setTrainNo(train.getTrainNo());
        vo.setTrainType(train.getTrainType());
        vo.setDepartureCity(train.getDepartureCity());
        vo.setArrivalCity(train.getArrivalCity());
        vo.setDepartureStation(train.getDepartureStation());
        vo.setArrivalStation(train.getArrivalStation());
        vo.setDepartureTime(train.getDepartureTime());
        vo.setArrivalTime(train.getArrivalTime());
        vo.setDurationMinutes(train.getDurationMinutes());
        vo.setBusinessPrice(train.getBusinessPrice());
        vo.setFirstClassPrice(train.getFirstClassPrice());
        vo.setSecondClassPrice(train.getSecondClassPrice());
        vo.setBusinessStock(train.getBusinessStock());
        vo.setFirstClassStock(train.getFirstClassStock());
        vo.setSecondClassStock(train.getSecondClassStock());
        vo.setStatus(train.getStatus());
        vo.setCreateTime(train.getCreateTime());
        return vo;
    }

    private AdminHotelVO toHotelVO(Hotel hotel) {
        AdminHotelVO vo = new AdminHotelVO();
        vo.setId(hotel.getId());
        vo.setHotelName(hotel.getHotelName());
        vo.setCity(hotel.getCity());
        vo.setDistrict(hotel.getDistrict());
        vo.setAddress(hotel.getAddress());
        vo.setDescription(hotel.getDescription());
        vo.setStarLevel(hotel.getStarLevel());
        vo.setCoverImage(hotel.getCoverImage());
        vo.setCheckInTime(hotel.getCheckInTime());
        vo.setCheckOutTime(hotel.getCheckOutTime());
        vo.setStatus(hotel.getStatus());
        vo.setCreateTime(hotel.getCreateTime());
        return vo;
    }

    private AdminHotelRoomVO toRoomVO(HotelRoom room) {
        Hotel hotel = hotelMapper.selectById(room.getHotelId());
        AdminHotelRoomVO vo = new AdminHotelRoomVO();
        vo.setId(room.getId());
        vo.setHotelId(room.getHotelId());
        vo.setHotelName(hotel == null ? "" : hotel.getHotelName());
        vo.setRoomName(room.getRoomName());
        vo.setBedType(room.getBedType());
        vo.setBreakfast(room.getBreakfast());
        vo.setRoomArea(room.getRoomArea());
        vo.setGuestCount(room.getGuestCount());
        vo.setPrice(room.getPrice());
        vo.setStock(room.getStock());
        vo.setCancelRule(room.getCancelRule());
        vo.setStatus(room.getStatus());
        vo.setCreateTime(room.getCreateTime());
        return vo;
    }

    private AdminTourVO toTourVO(TourPackage tour) {
        AdminTourVO vo = new AdminTourVO();
        vo.setId(tour.getId());
        vo.setPackageName(tour.getPackageName());
        vo.setDestination(tour.getDestination());
        vo.setDepartureCity(tour.getDepartureCity());
        vo.setDays(tour.getDays());
        vo.setPrice(tour.getPrice());
        vo.setStock(tour.getStock());
        vo.setTravelDates(tour.getTravelDates());
        vo.setDescription(tour.getDescription());
        vo.setCoverImage(tour.getCoverImage());
        vo.setStatus(tour.getStatus());
        vo.setCreateTime(tour.getCreateTime());
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
