package com.travelplatform.service.admin.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.admin.product.AdminFlightSaveRequest;
import com.travelplatform.dto.admin.product.AdminHotelRoomSaveRequest;
import com.travelplatform.dto.admin.product.AdminHotelSaveRequest;
import com.travelplatform.dto.admin.product.AdminTourSaveRequest;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.entity.TourPackage;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.vo.admin.product.AdminFlightVO;
import com.travelplatform.vo.admin.product.AdminHotelRoomVO;
import com.travelplatform.vo.admin.product.AdminHotelVO;
import com.travelplatform.vo.admin.product.AdminTourVO;
import com.travelplatform.vo.common.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductManageServiceImplTest {

    @Mock FlightMapper flightMapper;
    @Mock TrainTicketMapper trainTicketMapper;
    @Mock HotelMapper hotelMapper;
    @Mock HotelRoomMapper hotelRoomMapper;
    @Mock TourPackageMapper tourPackageMapper;
    @InjectMocks AdminProductManageServiceImpl service;

    @Test
    void listFlightsShouldFilterByKeyword() {
        when(flightMapper.selectList(any())).thenReturn(List.of(
                flight(1L, "MU1001", "Shanghai", "Beijing"),
                flight(2L, "CA2002", "Guangzhou", "Shenzhen")
        ));

        PageResult<AdminFlightVO> result = service.listFlights("mu1001", null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getFlightNo()).isEqualTo("MU1001");
    }

    @Test
    void createFlightShouldRejectInvalidArrivalTime() {
        AdminFlightSaveRequest request = flightRequest();
        request.setArrivalTime(request.getDepartureTime().minusHours(1));

        assertThatThrownBy(() -> service.createFlight(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createHotelShouldNormalizeDetailImages() {
        AdminHotelSaveRequest request = hotelRequest();
        request.setDetailImages(" /a.jpg , /b.jpg \n /a.jpg ");

        ArgumentCaptor<Hotel> captor = ArgumentCaptor.forClass(Hotel.class);
        when(hotelMapper.insert(captor.capture())).thenAnswer(invocation -> {
            captor.getValue().setId(11L);
            return 1;
        });
        when(hotelMapper.selectById(11L)).thenReturn(hotel(11L, "Lake Hotel"));

        AdminHotelVO result = service.createHotel(request);

        assertThat(captor.getValue().getDetailImages()).isEqualTo("/a.jpg\n/b.jpg");
        assertThat(result.getHotelName()).isEqualTo("Lake Hotel");
    }

    @Test
    void deleteHotelShouldRejectWhenRoomsExist() {
        when(hotelMapper.selectById(11L)).thenReturn(hotel(11L, "Lake Hotel"));
        when(hotelRoomMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteHotel(11L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createHotelRoomShouldRequireExistingHotel() {
        when(hotelMapper.selectById(11L)).thenReturn(null);
        AdminHotelRoomSaveRequest request = roomRequest();

        assertThatThrownBy(() -> service.createHotelRoom(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void listHotelRoomsShouldMatchKeywordByHotelName() {
        HotelRoom room = room(21L, 11L, "Deluxe");
        when(hotelRoomMapper.selectList(any())).thenReturn(List.of(room));
        when(hotelMapper.selectById(11L)).thenReturn(hotel(11L, "Lake Hotel"));

        PageResult<AdminHotelRoomVO> result = service.listHotelRooms(null, "lake", null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getHotelName()).isEqualTo("Lake Hotel");
    }

    @Test
    void updateTourShouldNormalizeDetailImages() {
        when(tourPackageMapper.selectById(31L)).thenReturn(tour(31L, "West Lake Tour"));
        AdminTourSaveRequest request = tourRequest();
        request.setDetailImages(" /1.jpg \n /2.jpg \n /1.jpg ");
        when(tourPackageMapper.selectById(31L)).thenReturn(tour(31L, "West Lake Tour"));

        ArgumentCaptor<TourPackage> captor = ArgumentCaptor.forClass(TourPackage.class);
        when(tourPackageMapper.selectById(31L)).thenReturn(tour(31L, "West Lake Tour"));

        AdminTourVO result = service.updateTour(31L, request);

        verify(tourPackageMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDetailImages()).isEqualTo("/1.jpg\n/2.jpg");
        assertThat(result.getPackageName()).isEqualTo("West Lake Tour");
    }

    private Flight flight(Long id, String no, String departure, String arrival) {
        Flight flight = new Flight();
        flight.setId(id);
        flight.setFlightNo(no);
        flight.setAirlineName("Airline");
        flight.setDepartureCity(departure);
        flight.setArrivalCity(arrival);
        flight.setDepartureAirport("A");
        flight.setArrivalAirport("B");
        flight.setDepartureTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        flight.setArrivalTime(LocalDateTime.of(2026, 1, 1, 12, 0));
        flight.setPrice(new BigDecimal("500"));
        flight.setStock(5);
        flight.setCabinClass("Economy");
        flight.setStatus(1);
        return flight;
    }

    private Hotel hotel(Long id, String name) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setHotelName(name);
        hotel.setCity("Hangzhou");
        hotel.setAddress("West Lake");
        hotel.setStarLevel(5);
        hotel.setStatus(1);
        return hotel;
    }

    private HotelRoom room(Long id, Long hotelId, String roomName) {
        HotelRoom room = new HotelRoom();
        room.setId(id);
        room.setHotelId(hotelId);
        room.setRoomName(roomName);
        room.setBedType("King");
        room.setBreakfast("Included");
        room.setRoomArea("40m2");
        room.setGuestCount(2);
        room.setPrice(new BigDecimal("600"));
        room.setStock(3);
        room.setStatus(1);
        return room;
    }

    private TourPackage tour(Long id, String name) {
        TourPackage tour = new TourPackage();
        tour.setId(id);
        tour.setPackageName(name);
        tour.setDestination("Hangzhou");
        tour.setDepartureCity("Shanghai");
        tour.setDays(2);
        tour.setPrice(new BigDecimal("999"));
        tour.setStock(10);
        tour.setTravelDates("2026-07-01");
        tour.setStatus(1);
        return tour;
    }

    private AdminFlightSaveRequest flightRequest() {
        AdminFlightSaveRequest request = new AdminFlightSaveRequest();
        request.setFlightNo("MU1001");
        request.setAirlineName("China Eastern");
        request.setDepartureCity("Shanghai");
        request.setArrivalCity("Beijing");
        request.setDepartureAirport("PVG");
        request.setArrivalAirport("PEK");
        request.setDepartureTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        request.setArrivalTime(LocalDateTime.of(2026, 7, 1, 12, 0));
        request.setPrice(new BigDecimal("500"));
        request.setStock(5);
        request.setCabinClass("Economy");
        request.setBaggagePolicy("20kg");
        request.setRefundPolicy("Flexible");
        request.setStatus(1);
        return request;
    }

    private AdminHotelSaveRequest hotelRequest() {
        AdminHotelSaveRequest request = new AdminHotelSaveRequest();
        request.setHotelName("Lake Hotel");
        request.setCity("Hangzhou");
        request.setDistrict("Xihu");
        request.setAddress("West Lake");
        request.setDescription("Nice");
        request.setStarLevel(5);
        request.setCoverImage("/cover.jpg");
        request.setCheckInTime("14:00");
        request.setCheckOutTime("12:00");
        request.setStatus(1);
        return request;
    }

    private AdminHotelRoomSaveRequest roomRequest() {
        AdminHotelRoomSaveRequest request = new AdminHotelRoomSaveRequest();
        request.setHotelId(11L);
        request.setRoomName("Deluxe");
        request.setBedType("King");
        request.setBreakfast("Included");
        request.setRoomArea("40m2");
        request.setGuestCount(2);
        request.setPrice(new BigDecimal("600"));
        request.setStock(3);
        request.setCancelRule("Free");
        request.setStatus(1);
        return request;
    }

    private AdminTourSaveRequest tourRequest() {
        AdminTourSaveRequest request = new AdminTourSaveRequest();
        request.setPackageName("West Lake Tour");
        request.setDestination("Hangzhou");
        request.setDepartureCity("Shanghai");
        request.setDays(2);
        request.setPrice(new BigDecimal("999"));
        request.setStock(10);
        request.setTravelDates("2026-07-01");
        request.setDescription("Great");
        request.setCoverImage("/cover.jpg");
        request.setStatus(1);
        return request;
    }
}
