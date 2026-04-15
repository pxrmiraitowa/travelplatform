package com.travelplatform.service.admin;

import com.travelplatform.dto.admin.order.AdminOrderStatusUpdateRequest;
import com.travelplatform.vo.admin.order.AdminOrderDetailVO;
import com.travelplatform.vo.admin.order.AdminOrderListItemVO;
import com.travelplatform.vo.common.PageResult;

public interface AdminOrderManageService {

    PageResult<AdminOrderListItemVO> listOrders(String keyword, String bizType, Integer status, Integer pageNum, Integer pageSize);

    AdminOrderDetailVO getOrderDetail(Long id);

    void updateOrderStatus(Long id, AdminOrderStatusUpdateRequest request);

    void cancelOrder(Long id);
}
