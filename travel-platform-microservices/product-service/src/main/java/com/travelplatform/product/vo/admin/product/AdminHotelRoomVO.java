package com.travelplatform.product.vo.admin.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminHotelRoomVO {

    private Long id;
    private Long hotelId;
    private String hotelName;
    private String roomName;
    private String bedType;
    private String breakfast;
    private String roomArea;
    private Integer guestCount;
    private BigDecimal price;
    private Integer stock;
    private String cancelRule;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }
    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public String getBreakfast() { return breakfast; }
    public void setBreakfast(String breakfast) { this.breakfast = breakfast; }
    public String getRoomArea() { return roomArea; }
    public void setRoomArea(String roomArea) { this.roomArea = roomArea; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getCancelRule() { return cancelRule; }
    public void setCancelRule(String cancelRule) { this.cancelRule = cancelRule; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
