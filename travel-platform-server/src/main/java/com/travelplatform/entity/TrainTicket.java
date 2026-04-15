package com.travelplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("train_ticket")
public class TrainTicket extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String trainNo;
    private String trainType;
    private String departureCity;
    private String arrivalCity;
    private String departureStation;
    private String arrivalStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer durationMinutes;
    private BigDecimal businessPrice;
    private BigDecimal firstClassPrice;
    private BigDecimal secondClassPrice;
    private Integer businessStock;
    private Integer firstClassStock;
    private Integer secondClassStock;
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public void setTrainNo(String trainNo) {
        this.trainNo = trainNo;
    }

    public String getTrainType() {
        return trainType;
    }

    public void setTrainType(String trainType) {
        this.trainType = trainType;
    }

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

    public String getDepartureStation() {
        return departureStation;
    }

    public void setDepartureStation(String departureStation) {
        this.departureStation = departureStation;
    }

    public String getArrivalStation() {
        return arrivalStation;
    }

    public void setArrivalStation(String arrivalStation) {
        this.arrivalStation = arrivalStation;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getBusinessPrice() {
        return businessPrice;
    }

    public void setBusinessPrice(BigDecimal businessPrice) {
        this.businessPrice = businessPrice;
    }

    public BigDecimal getFirstClassPrice() {
        return firstClassPrice;
    }

    public void setFirstClassPrice(BigDecimal firstClassPrice) {
        this.firstClassPrice = firstClassPrice;
    }

    public BigDecimal getSecondClassPrice() {
        return secondClassPrice;
    }

    public void setSecondClassPrice(BigDecimal secondClassPrice) {
        this.secondClassPrice = secondClassPrice;
    }

    public Integer getBusinessStock() {
        return businessStock;
    }

    public void setBusinessStock(Integer businessStock) {
        this.businessStock = businessStock;
    }

    public Integer getFirstClassStock() {
        return firstClassStock;
    }

    public void setFirstClassStock(Integer firstClassStock) {
        this.firstClassStock = firstClassStock;
    }

    public Integer getSecondClassStock() {
        return secondClassStock;
    }

    public void setSecondClassStock(Integer secondClassStock) {
        this.secondClassStock = secondClassStock;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
