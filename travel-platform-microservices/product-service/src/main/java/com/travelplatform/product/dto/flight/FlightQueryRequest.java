package com.travelplatform.product.dto.flight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

public class FlightQueryRequest {

    private String departureCity;
    private String arrivalCity;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate departureDate;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime departureStartTime;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime departureEndTime;
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    public String getDepartureCity() {
        return departureCity;
    }

    public void setDepartureCity(String departureCity) {
        this.departureCity = departureCity;
    }

    public String getArrivalCity() {
        return arrivalCity;
    }

    public void setArrivalCity(String arrivalCity) {
        this.arrivalCity = arrivalCity;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public LocalTime getDepartureStartTime() {
        return departureStartTime;
    }

    public void setDepartureStartTime(LocalTime departureStartTime) {
        this.departureStartTime = departureStartTime;
    }

    public LocalTime getDepartureEndTime() {
        return departureEndTime;
    }

    public void setDepartureEndTime(LocalTime departureEndTime) {
        this.departureEndTime = departureEndTime;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
