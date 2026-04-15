package com.travelplatform.dto.usercontact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserContactCreateRequest {

    @NotBlank(message = "出行人姓名不能为空")
    @Size(max = 30, message = "出行人姓名长度不能超过30位")
    private String name;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式不正确")
    private String phone;

    @NotBlank(message = "身份证号不能为空")
    @Size(min = 15, max = 18, message = "身份证号长度不正确")
    private String idCard;

    private Integer contactType;

    private Integer isDefault;

    @Size(max = 100, message = "备注长度不能超过100位")
    private String remark;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public Integer getContactType() {
        return contactType;
    }

    public void setContactType(Integer contactType) {
        this.contactType = contactType;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
