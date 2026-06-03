package com.travelplatform.dto.admin.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AdminTourSaveRequest {

    @NotBlank(message = "产品名称不能为空")
    private String packageName;
    @NotBlank(message = "目的地不能为空")
    private String destination;
    private String departureCity;
    @NotNull(message = "天数不能为空")
    @Min(value = 1, message = "天数不能小于1")
    private Integer days;
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal price;
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;
    private String travelDates;
    private String description;
    private String coverImage;
    private String detailImages;
    @NotNull(message = "状态不能为空")
    private Integer status;

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getDepartureCity() { return departureCity; }
    public void setDepartureCity(String departureCity) { this.departureCity = departureCity; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getTravelDates() { return travelDates; }
    public void setTravelDates(String travelDates) { this.travelDates = travelDates; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getDetailImages() { return detailImages; }
    public void setDetailImages(String detailImages) { this.detailImages = detailImages; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
