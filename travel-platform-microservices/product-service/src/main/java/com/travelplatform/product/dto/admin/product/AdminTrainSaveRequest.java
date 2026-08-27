package com.travelplatform.product.dto.admin.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminTrainSaveRequest {

    @NotBlank(message = "车次不能为空")
    private String trainNo;
    @NotBlank(message = "车次类型不能为空")
    private String trainType;
    @NotBlank(message = "出发城市不能为空")
    private String departureCity;
    @NotBlank(message = "到达城市不能为空")
    private String arrivalCity;
    @NotBlank(message = "出发站不能为空")
    private String departureStation;
    @NotBlank(message = "到达站不能为空")
    private String arrivalStation;
    @NotNull(message = "出发时间不能为空")
    private LocalDateTime departureTime;
    @NotNull(message = "到达时间不能为空")
    private LocalDateTime arrivalTime;
    @NotNull(message = "时长不能为空")
    @Min(value = 1, message = "时长不能小于1")
    private Integer durationMinutes;
    @DecimalMin(value = "0.00", message = "商务座价格不能小于0")
    private BigDecimal businessPrice;
    @DecimalMin(value = "0.00", message = "一等座价格不能小于0")
    private BigDecimal firstClassPrice;
    @DecimalMin(value = "0.00", message = "二等座价格不能小于0")
    private BigDecimal secondClassPrice;
    @NotNull(message = "商务座库存不能为空")
    @Min(value = 0, message = "商务座库存不能小于0")
    private Integer businessStock;
    @NotNull(message = "一等座库存不能为空")
    @Min(value = 0, message = "一等座库存不能小于0")
    private Integer firstClassStock;
    @NotNull(message = "二等座库存不能为空")
    @Min(value = 0, message = "二等座库存不能小于0")
    private Integer secondClassStock;
    @NotNull(message = "状态不能为空")
    private Integer status;

    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }
    public String getTrainType() { return trainType; }
    public void setTrainType(String trainType) { this.trainType = trainType; }
    public String getDepartureCity() { return departureCity; }
    public void setDepartureCity(String departureCity) { this.departureCity = departureCity; }
    public String getArrivalCity() { return arrivalCity; }
    public void setArrivalCity(String arrivalCity) { this.arrivalCity = arrivalCity; }
    public String getDepartureStation() { return departureStation; }
    public void setDepartureStation(String departureStation) { this.departureStation = departureStation; }
    public String getArrivalStation() { return arrivalStation; }
    public void setArrivalStation(String arrivalStation) { this.arrivalStation = arrivalStation; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getBusinessPrice() { return businessPrice; }
    public void setBusinessPrice(BigDecimal businessPrice) { this.businessPrice = businessPrice; }
    public BigDecimal getFirstClassPrice() { return firstClassPrice; }
    public void setFirstClassPrice(BigDecimal firstClassPrice) { this.firstClassPrice = firstClassPrice; }
    public BigDecimal getSecondClassPrice() { return secondClassPrice; }
    public void setSecondClassPrice(BigDecimal secondClassPrice) { this.secondClassPrice = secondClassPrice; }
    public Integer getBusinessStock() { return businessStock; }
    public void setBusinessStock(Integer businessStock) { this.businessStock = businessStock; }
    public Integer getFirstClassStock() { return firstClassStock; }
    public void setFirstClassStock(Integer firstClassStock) { this.firstClassStock = firstClassStock; }
    public Integer getSecondClassStock() { return secondClassStock; }
    public void setSecondClassStock(Integer secondClassStock) { this.secondClassStock = secondClassStock; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
