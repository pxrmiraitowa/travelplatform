package com.travelplatform.service.order;

import com.travelplatform.dto.order.FlightOrderCreateRequest;
import com.travelplatform.dto.order.HotelOrderCreateRequest;
import com.travelplatform.dto.order.TourOrderCreateRequest;
import com.travelplatform.dto.order.TrainOrderCreateRequest;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.order.OrderDetailVO;
import com.travelplatform.vo.order.OrderListItemVO;

public interface OrderService {

    OrderDetailVO createFlightOrder(FlightOrderCreateRequest request);

    OrderDetailVO createTrainOrder(TrainOrderCreateRequest request);

    OrderDetailVO createHotelOrder(HotelOrderCreateRequest request);

    OrderDetailVO createTourOrder(TourOrderCreateRequest request);

    PageResult<OrderListItemVO> listCurrentUserOrders(String bizType, Integer status, Integer pageNum, Integer pageSize);

    OrderDetailVO getCurrentUserOrderDetail(Long id);

    void cancelCurrentUserOrder(Long id);
}
