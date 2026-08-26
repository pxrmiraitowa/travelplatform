package com.travelplatform.product.service.tour;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.product.vo.tour.TourDetailVO;
import com.travelplatform.product.vo.tour.TourListItemVO;

public interface TourService {

    PageResult<TourListItemVO> listTours(String destination, Integer pageNum, Integer pageSize);

    TourDetailVO getTourDetail(Long id);
}
