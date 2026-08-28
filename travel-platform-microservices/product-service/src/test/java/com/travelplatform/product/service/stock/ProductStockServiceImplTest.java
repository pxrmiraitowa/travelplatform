package com.travelplatform.product.service.stock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.product.dto.internal.StockChangeRequest;
import com.travelplatform.product.entity.Flight;
import com.travelplatform.product.mapper.FlightMapper;
import com.travelplatform.product.mapper.HotelRoomMapper;
import com.travelplatform.product.mapper.TourPackageMapper;
import com.travelplatform.product.mapper.TrainTicketMapper;
import com.travelplatform.product.service.stock.impl.ProductStockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductStockServiceImplTest {
    private FlightMapper flightMapper;
    private TrainTicketMapper trainTicketMapper;
    private HotelRoomMapper hotelRoomMapper;
    private TourPackageMapper tourPackageMapper;
    private ProductStockService service;

    @BeforeEach
    void setUp() {
        flightMapper = mock(FlightMapper.class);
        trainTicketMapper = mock(TrainTicketMapper.class);
        hotelRoomMapper = mock(HotelRoomMapper.class);
        tourPackageMapper = mock(TourPackageMapper.class);
        service = new ProductStockServiceImpl(flightMapper, trainTicketMapper, hotelRoomMapper, tourPackageMapper);
    }

    @Test
    void rejectsDeductionWhenAtomicUpdateChangesNoRow() {
        when(flightMapper.update(isNull(), org.mockito.ArgumentMatchers.<Wrapper<Flight>>any())).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.deduct(request("FLIGHT", 1L, null, null, 2)));
    }

    @Test
    void restoresFlightStockWithOneAtomicUpdate() {
        when(flightMapper.update(isNull(), org.mockito.ArgumentMatchers.<Wrapper<Flight>>any())).thenReturn(1);

        assertDoesNotThrow(() -> service.restore(request("FLIGHT", 1L, null, null, 1)));
        verify(flightMapper).update(isNull(), org.mockito.ArgumentMatchers.<Wrapper<Flight>>any());
    }

    @Test
    void rejectsTrainDeductionWithoutValidSeatType() {
        assertThrows(BusinessException.class,
                () -> service.deduct(request("TRAIN", 2L, null, "无效席别", 1)));
        verifyNoInteractions(trainTicketMapper);
    }

    private StockChangeRequest request(String type, Long productId, Long variantId,
                                       String variantName, int quantity) {
        StockChangeRequest request = new StockChangeRequest();
        request.setProductType(type);
        request.setProductId(productId);
        request.setVariantId(variantId);
        request.setVariantName(variantName);
        request.setQuantity(quantity);
        return request;
    }
}
