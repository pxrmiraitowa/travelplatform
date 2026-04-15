package com.travelplatform.service.tour;

import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.tour.TourDetailVO;
import com.travelplatform.vo.tour.TourListItemVO;

public interface TourService {

    PageResult<TourListItemVO> listTours(String destination, Integer pageNum, Integer pageSize);

    TourDetailVO getTourDetail(Long id);
}
