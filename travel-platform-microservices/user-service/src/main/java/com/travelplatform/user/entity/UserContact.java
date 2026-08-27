package com.travelplatform.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("user_contact")
public class UserContact extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String idCard;
    private Integer contactType;
    private Integer isDefault;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public Integer getContactType() { return contactType; }
    public void setContactType(Integer contactType) { this.contactType = contactType; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
