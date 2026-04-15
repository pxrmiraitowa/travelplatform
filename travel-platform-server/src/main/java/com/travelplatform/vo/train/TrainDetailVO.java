package com.travelplatform.vo.train;

import java.util.List;

public class TrainDetailVO extends TrainListItemVO {

    private List<SeatOptionVO> seatOptions;
    private Integer status;

    public List<SeatOptionVO> getSeatOptions() {
        return seatOptions;
    }

    public void setSeatOptions(List<SeatOptionVO> seatOptions) {
        this.seatOptions = seatOptions;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
