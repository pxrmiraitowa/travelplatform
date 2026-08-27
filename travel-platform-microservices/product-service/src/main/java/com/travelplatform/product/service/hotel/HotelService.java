package com.travelplatform.product.service.hotel;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.product.dto.hotel.HotelQueryRequest;
import com.travelplatform.product.vo.hotel.HotelDetailVO;
import com.travelplatform.product.vo.hotel.HotelListItemVO;

public interface HotelService {

    PageResult<HotelListItemVO> searchHotels(HotelQueryRequest request);

    HotelDetailVO getHotelDetail(Long id);
}
