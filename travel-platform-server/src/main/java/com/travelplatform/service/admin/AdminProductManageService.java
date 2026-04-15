package com.travelplatform.service.admin;

import com.travelplatform.dto.admin.product.AdminFlightSaveRequest;
import com.travelplatform.dto.admin.product.AdminHotelRoomSaveRequest;
import com.travelplatform.dto.admin.product.AdminHotelSaveRequest;
import com.travelplatform.dto.admin.product.AdminTourSaveRequest;
import com.travelplatform.dto.admin.product.AdminTrainSaveRequest;
import com.travelplatform.vo.admin.product.AdminFlightVO;
import com.travelplatform.vo.admin.product.AdminHotelRoomVO;
import com.travelplatform.vo.admin.product.AdminHotelVO;
import com.travelplatform.vo.admin.product.AdminTourVO;
import com.travelplatform.vo.admin.product.AdminTrainVO;
import com.travelplatform.vo.common.PageResult;

public interface AdminProductManageService {

    PageResult<AdminFlightVO> listFlights(String keyword, Integer status, Integer pageNum, Integer pageSize);

    AdminFlightVO createFlight(AdminFlightSaveRequest request);

    AdminFlightVO updateFlight(Long id, AdminFlightSaveRequest request);

    void deleteFlight(Long id);

    PageResult<AdminTrainVO> listTrains(String keyword, Integer status, Integer pageNum, Integer pageSize);

    AdminTrainVO createTrain(AdminTrainSaveRequest request);

    AdminTrainVO updateTrain(Long id, AdminTrainSaveRequest request);

    void deleteTrain(Long id);

    PageResult<AdminHotelVO> listHotels(String keyword, Integer status, Integer pageNum, Integer pageSize);

    AdminHotelVO createHotel(AdminHotelSaveRequest request);

    AdminHotelVO updateHotel(Long id, AdminHotelSaveRequest request);

    void deleteHotel(Long id);

    PageResult<AdminHotelRoomVO> listHotelRooms(Long hotelId, String keyword, Integer status, Integer pageNum, Integer pageSize);

    AdminHotelRoomVO createHotelRoom(AdminHotelRoomSaveRequest request);

    AdminHotelRoomVO updateHotelRoom(Long id, AdminHotelRoomSaveRequest request);

    void deleteHotelRoom(Long id);

    PageResult<AdminTourVO> listTours(String keyword, Integer status, Integer pageNum, Integer pageSize);

    AdminTourVO createTour(AdminTourSaveRequest request);

    AdminTourVO updateTour(Long id, AdminTourSaveRequest request);

    void deleteTour(Long id);
}
