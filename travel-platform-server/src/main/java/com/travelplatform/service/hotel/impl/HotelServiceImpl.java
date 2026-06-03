package com.travelplatform.service.hotel.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.util.ProductMediaUtils;
import com.travelplatform.dto.hotel.HotelQueryRequest;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.service.hotel.HotelService;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.hotel.HotelDetailVO;
import com.travelplatform.vo.hotel.HotelListItemVO;
import com.travelplatform.vo.hotel.HotelRoomVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HotelServiceImpl implements HotelService {

    private final HotelMapper hotelMapper;
    private final HotelRoomMapper hotelRoomMapper;

    public HotelServiceImpl(HotelMapper hotelMapper, HotelRoomMapper hotelRoomMapper) {
        this.hotelMapper = hotelMapper;
        this.hotelRoomMapper = hotelRoomMapper;
    }

    @Override
    public PageResult<HotelListItemVO> searchHotels(HotelQueryRequest request) {
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : Math.min(request.getPageSize(), 50);

        List<Hotel> hotels = hotelMapper.selectList(new LambdaQueryWrapper<Hotel>()
                .eq(Hotel::getStatus, 1)
                .like(StringUtils.hasText(request.getCity()), Hotel::getCity, request.getCity())
                .orderByAsc(Hotel::getStarLevel)
                .orderByAsc(Hotel::getId));

        List<Long> hotelIds = hotels.stream().map(Hotel::getId).toList();
        Map<Long, List<HotelRoom>> roomMap = hotelIds.isEmpty()
                ? Map.of()
                : hotelRoomMapper.selectList(new LambdaQueryWrapper<HotelRoom>()
                .in(HotelRoom::getHotelId, hotelIds)
                .eq(HotelRoom::getStatus, 1))
                .stream()
                .collect(Collectors.groupingBy(HotelRoom::getHotelId));

        List<HotelListItemVO> filtered = hotels.stream()
                .map(hotel -> toListItemVO(hotel, roomMap.getOrDefault(hotel.getId(), List.of())))
                .filter(item -> item.getAvailableRoomCount() != null && item.getAvailableRoomCount() > 0)
                .toList();

        int total = filtered.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);

        PageResult<HotelListItemVO> result = new PageResult<>();
        result.setRecords(filtered.subList(fromIndex, toIndex));
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    @Override
    public HotelDetailVO getHotelDetail(Long id) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null || !Integer.valueOf(1).equals(hotel.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "酒店不存在");
        }
        List<HotelRoom> rooms = hotelRoomMapper.selectList(new LambdaQueryWrapper<HotelRoom>()
                .eq(HotelRoom::getHotelId, id)
                .eq(HotelRoom::getStatus, 1)
                .orderByAsc(HotelRoom::getPrice));

        HotelDetailVO detailVO = new HotelDetailVO();
        copyBaseInfo(hotel, detailVO, rooms);
        detailVO.setRoomList(rooms.stream().map(this::toRoomVO).toList());
        return detailVO;
    }

    private HotelListItemVO toListItemVO(Hotel hotel, List<HotelRoom> rooms) {
        HotelListItemVO vo = new HotelListItemVO();
        copyBaseInfo(hotel, vo, rooms);
        return vo;
    }

    private void copyBaseInfo(Hotel hotel, HotelListItemVO vo, List<HotelRoom> rooms) {
        vo.setId(hotel.getId());
        vo.setHotelName(hotel.getHotelName());
        vo.setCity(hotel.getCity());
        vo.setDistrict(hotel.getDistrict());
        vo.setAddress(hotel.getAddress());
        vo.setDescription(hotel.getDescription());
        vo.setStarLevel(hotel.getStarLevel());
        vo.setCoverImage(hotel.getCoverImage());
        vo.setDetailImages(ProductMediaUtils.parseImageList(hotel.getDetailImages(), hotel.getCoverImage()));
        vo.setCheckInTime(hotel.getCheckInTime());
        vo.setCheckOutTime(hotel.getCheckOutTime());
        vo.setMinPrice(resolveMinPrice(rooms));
        vo.setAvailableRoomCount(resolveAvailableRoomCount(rooms));
    }

    private HotelRoomVO toRoomVO(HotelRoom room) {
        HotelRoomVO vo = new HotelRoomVO();
        vo.setId(room.getId());
        vo.setRoomName(room.getRoomName());
        vo.setBedType(room.getBedType());
        vo.setBreakfast(room.getBreakfast());
        vo.setRoomArea(room.getRoomArea());
        vo.setGuestCount(room.getGuestCount());
        vo.setPrice(room.getPrice());
        vo.setStock(room.getStock());
        vo.setCancelRule(room.getCancelRule());
        return vo;
    }

    private BigDecimal resolveMinPrice(List<HotelRoom> rooms) {
        return rooms.stream()
                .filter(room -> room.getStock() != null && room.getStock() > 0)
                .map(HotelRoom::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private Integer resolveAvailableRoomCount(List<HotelRoom> rooms) {
        return rooms.stream()
                .filter(room -> room.getStock() != null && room.getStock() > 0)
                .mapToInt(room -> room.getStock() == null ? 0 : room.getStock())
                .sum();
    }
}
