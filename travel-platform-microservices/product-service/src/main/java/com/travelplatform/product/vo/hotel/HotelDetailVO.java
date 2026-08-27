package com.travelplatform.product.vo.hotel;

import java.util.List;

public class HotelDetailVO extends HotelListItemVO {

    private List<HotelRoomVO> roomList;

    public List<HotelRoomVO> getRoomList() {
        return roomList;
    }

    public void setRoomList(List<HotelRoomVO> roomList) {
        this.roomList = roomList;
    }
}
