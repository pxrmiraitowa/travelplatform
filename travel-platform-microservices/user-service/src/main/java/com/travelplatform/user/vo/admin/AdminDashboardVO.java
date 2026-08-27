package com.travelplatform.user.vo.admin;

public class AdminDashboardVO {

    private Long userCount = 0L;
    private Long productCount = 0L;
    private Long orderCount = 0L;
    private Long recentOrderCount = 0L;

    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }
    public Long getProductCount() { return productCount; }
    public void setProductCount(Long productCount) { this.productCount = productCount; }
    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
    public Long getRecentOrderCount() { return recentOrderCount; }
    public void setRecentOrderCount(Long recentOrderCount) { this.recentOrderCount = recentOrderCount; }
}
