package com.travelplatform.order.service.admin;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.order.dto.admin.AdminOrderStatusUpdateRequest;
import com.travelplatform.order.vo.admin.AdminOrderDetailVO;
import com.travelplatform.order.vo.admin.AdminOrderListItemVO;

public interface AdminOrderManageService {

    PageResult<AdminOrderListItemVO> listOrders(String keyword, String bizType, Integer status, Integer pageNum, Integer pageSize);

    AdminOrderDetailVO getOrderDetail(Long id);

    void updateOrderStatus(Long id, AdminOrderStatusUpdateRequest request);

    void cancelOrder(Long id);
}
