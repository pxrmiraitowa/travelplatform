package com.travelplatform.product.dto.admin.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminFlightSaveRequest {

    @NotBlank(message = "航班号不能为空")
    private String flightNo;
    @NotBlank(message = "航司不能为空")
    private String airlineName;
    @NotBlank(message = "出发城市不能为空")
    private String departureCity;
    @NotBlank(message = "到达城市不能为空")
    private String arrivalCity;
    @NotBlank(message = "出发机场不能为空")
    private String departureAirport;
    @NotBlank(message = "到达机场不能为空")
    private String arrivalAirport;
    @NotNull(message = "起飞时间不能为空")
    private LocalDateTime departureTime;
    @NotNull(message = "到达时间不能为空")
    private LocalDateTime arrivalTime;
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal price;
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;
    @NotBlank(message = "舱位不能为空")
    private String cabinClass;
    private String baggagePolicy;
    private String refundPolicy;
    @NotNull(message = "状态不能为空")
    private Integer status;

    public String getFlightNo() { return flightNo; }
    public void setFlightNo(String flightNo) { this.flightNo = flightNo; }
    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }
    public String getDepartureCity() { return departureCity; }
    public void setDepartureCity(String departureCity) { this.departureCity = departureCity; }
    public String getArrivalCity() { return arrivalCity; }
    public void setArrivalCity(String arrivalCity) { this.arrivalCity = arrivalCity; }
    public String getDepartureAirport() { return departureAirport; }
    public void setDepartureAirport(String departureAirport) { this.departureAirport = departureAirport; }
    public String getArrivalAirport() { return arrivalAirport; }
    public void setArrivalAirport(String arrivalAirport) { this.arrivalAirport = arrivalAirport; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String cabinClass) { this.cabinClass = cabinClass; }
    public String getBaggagePolicy() { return baggagePolicy; }
    public void setBaggagePolicy(String baggagePolicy) { this.baggagePolicy = baggagePolicy; }
    public String getRefundPolicy() { return refundPolicy; }
    public void setRefundPolicy(String refundPolicy) { this.refundPolicy = refundPolicy; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
