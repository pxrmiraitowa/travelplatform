package com.travelplatform.product.controller.admin;

import com.travelplatform.common.result.Result;
import com.travelplatform.product.dto.admin.product.AdminFlightSaveRequest;
import com.travelplatform.product.dto.admin.product.AdminHotelRoomSaveRequest;
import com.travelplatform.product.dto.admin.product.AdminHotelSaveRequest;
import com.travelplatform.product.dto.admin.product.AdminTourSaveRequest;
import com.travelplatform.product.dto.admin.product.AdminTrainSaveRequest;
import com.travelplatform.product.service.admin.AdminProductManageService;
import com.travelplatform.product.vo.admin.product.AdminFlightVO;
import com.travelplatform.product.vo.admin.product.AdminHotelRoomVO;
import com.travelplatform.product.vo.admin.product.AdminHotelVO;
import com.travelplatform.product.vo.admin.product.AdminTourVO;
import com.travelplatform.product.vo.admin.product.AdminTrainVO;
import com.travelplatform.common.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminProductManageController {

    private final AdminProductManageService adminProductManageService;

    public AdminProductManageController(AdminProductManageService adminProductManageService) {
        this.adminProductManageService = adminProductManageService;
    }

    @Operation(summary = "后台航班列表")
    @GetMapping("/flights")
    public Result<PageResult<AdminFlightVO>> listFlights(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Integer status,
                                                         @RequestParam(required = false) Integer pageNum,
                                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminProductManageService.listFlights(keyword, status, pageNum, pageSize));
    }

    @PostMapping("/flights")
    public Result<AdminFlightVO> createFlight(@Valid @RequestBody AdminFlightSaveRequest request) {
        return Result.success(adminProductManageService.createFlight(request));
    }

    @PutMapping("/flights/{id}")
    public Result<AdminFlightVO> updateFlight(@PathVariable Long id, @Valid @RequestBody AdminFlightSaveRequest request) {
        return Result.success(adminProductManageService.updateFlight(id, request));
    }

    @DeleteMapping("/flights/{id}")
    public Result<Void> deleteFlight(@PathVariable Long id) {
        adminProductManageService.deleteFlight(id);
        return Result.success();
    }

    @Operation(summary = "后台车次列表")
    @GetMapping("/trains")
    public Result<PageResult<AdminTrainVO>> listTrains(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) Integer pageNum,
                                                       @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminProductManageService.listTrains(keyword, status, pageNum, pageSize));
    }

    @PostMapping("/trains")
    public Result<AdminTrainVO> createTrain(@Valid @RequestBody AdminTrainSaveRequest request) {
        return Result.success(adminProductManageService.createTrain(request));
    }

    @PutMapping("/trains/{id}")
    public Result<AdminTrainVO> updateTrain(@PathVariable Long id, @Valid @RequestBody AdminTrainSaveRequest request) {
        return Result.success(adminProductManageService.updateTrain(id, request));
    }

    @DeleteMapping("/trains/{id}")
    public Result<Void> deleteTrain(@PathVariable Long id) {
        adminProductManageService.deleteTrain(id);
        return Result.success();
    }

    @Operation(summary = "后台酒店列表")
    @GetMapping("/hotels")
    public Result<PageResult<AdminHotelVO>> listHotels(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) Integer pageNum,
                                                       @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminProductManageService.listHotels(keyword, status, pageNum, pageSize));
    }

    @PostMapping("/hotels")
    public Result<AdminHotelVO> createHotel(@Valid @RequestBody AdminHotelSaveRequest request) {
        return Result.success(adminProductManageService.createHotel(request));
    }

    @PutMapping("/hotels/{id}")
    public Result<AdminHotelVO> updateHotel(@PathVariable Long id, @Valid @RequestBody AdminHotelSaveRequest request) {
        return Result.success(adminProductManageService.updateHotel(id, request));
    }

    @DeleteMapping("/hotels/{id}")
    public Result<Void> deleteHotel(@PathVariable Long id) {
        adminProductManageService.deleteHotel(id);
        return Result.success();
    }

    @Operation(summary = "后台房型列表")
    @GetMapping("/hotel-rooms")
    public Result<PageResult<AdminHotelRoomVO>> listHotelRooms(@RequestParam(required = false) Long hotelId,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Integer status,
                                                               @RequestParam(required = false) Integer pageNum,
                                                               @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminProductManageService.listHotelRooms(hotelId, keyword, status, pageNum, pageSize));
    }

    @PostMapping("/hotel-rooms")
    public Result<AdminHotelRoomVO> createHotelRoom(@Valid @RequestBody AdminHotelRoomSaveRequest request) {
        return Result.success(adminProductManageService.createHotelRoom(request));
    }

    @PutMapping("/hotel-rooms/{id}")
    public Result<AdminHotelRoomVO> updateHotelRoom(@PathVariable Long id, @Valid @RequestBody AdminHotelRoomSaveRequest request) {
        return Result.success(adminProductManageService.updateHotelRoom(id, request));
    }

    @DeleteMapping("/hotel-rooms/{id}")
    public Result<Void> deleteHotelRoom(@PathVariable Long id) {
        adminProductManageService.deleteHotelRoom(id);
        return Result.success();
    }

    @Operation(summary = "后台旅游产品列表")
    @GetMapping("/tours")
    public Result<PageResult<AdminTourVO>> listTours(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) Integer pageNum,
                                                     @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminProductManageService.listTours(keyword, status, pageNum, pageSize));
    }

    @PostMapping("/tours")
    public Result<AdminTourVO> createTour(@Valid @RequestBody AdminTourSaveRequest request) {
        return Result.success(adminProductManageService.createTour(request));
    }

    @PutMapping("/tours/{id}")
    public Result<AdminTourVO> updateTour(@PathVariable Long id, @Valid @RequestBody AdminTourSaveRequest request) {
        return Result.success(adminProductManageService.updateTour(id, request));
    }

    @DeleteMapping("/tours/{id}")
    public Result<Void> deleteTour(@PathVariable Long id) {
        adminProductManageService.deleteTour(id);
        return Result.success();
    }
}
