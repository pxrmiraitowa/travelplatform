package com.travelplatform.dto.admin.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AdminHotelRoomSaveRequest {

    @NotNull(message = "酒店不能为空")
    private Long hotelId;
    @NotBlank(message = "房型名称不能为空")
    private String roomName;
    @NotBlank(message = "床型不能为空")
    private String bedType;
    private String breakfast;
    private String roomArea;
    @NotNull(message = "可住人数不能为空")
    @Min(value = 1, message = "可住人数不能小于1")
    private Integer guestCount;
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal price;
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;
    private String cancelRule;
    @NotNull(message = "状态不能为空")
    private Integer status;

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }
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
}
