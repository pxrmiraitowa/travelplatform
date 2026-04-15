package com.travelplatform.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TrainOrderCreateRequest {

    @NotNull(message = "车次不能为空")
    private Long trainTicketId;

    @NotNull(message = "乘车人不能为空")
    private Long contactId;

    @NotBlank(message = "座位类型不能为空")
    private String seatType;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;

    public Long getTrainTicketId() {
        return trainTicketId;
    }

    public void setTrainTicketId(Long trainTicketId) {
        this.trainTicketId = trainTicketId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
