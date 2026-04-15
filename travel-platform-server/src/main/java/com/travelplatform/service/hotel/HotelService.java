package com.travelplatform.service.hotel;

import com.travelplatform.dto.hotel.HotelQueryRequest;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.hotel.HotelDetailVO;
import com.travelplatform.vo.hotel.HotelListItemVO;

public interface HotelService {

    PageResult<HotelListItemVO> searchHotels(HotelQueryRequest request);

    HotelDetailVO getHotelDetail(Long id);
}
