package com.travelplatform.product.vo.flight;

public class FlightDetailVO extends FlightListItemVO {

    private String baggagePolicy;
    private String refundPolicy;
    private Integer status;

    public String getBaggagePolicy() {
        return baggagePolicy;
    }

    public void setBaggagePolicy(String baggagePolicy) {
        this.baggagePolicy = baggagePolicy;
    }

    public String getRefundPolicy() {
        return refundPolicy;
    }

    public void setRefundPolicy(String refundPolicy) {
        this.refundPolicy = refundPolicy;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
