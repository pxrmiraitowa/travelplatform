package com.travelplatform.service.pricealert.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.pricealert.PriceAlertCreateRequest;
import com.travelplatform.entity.PriceAlert;
import com.travelplatform.mapper.PriceAlertMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.price.impl.PriceCompareServiceImpl;
import com.travelplatform.vo.pricealert.PriceAlertVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceImplTest {

    @Mock PriceAlertMapper priceAlertMapper;
    @Mock PriceCompareServiceImpl priceCompareService;
    @InjectMocks PriceAlertServiceImpl service;

    @Test
    void listCurrentUserAlertsShouldMapTriggeredStatus() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            PriceAlert alert = alert(1L, 1L, "hotel", 8L, new BigDecimal("300"), "watch");
            when(priceAlertMapper.selectList(any())).thenReturn(List.of(alert));
            when(priceCompareService.getProductSnapshot("hotel", 8L))
                    .thenReturn(new PriceCompareServiceImpl.ProductSnapshot("HOTEL", 8L, "West Lake Hotel", new BigDecimal("280")));

            List<PriceAlertVO> result = service.listCurrentUserAlerts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductName()).isEqualTo("West Lake Hotel");
            assertThat(result.get(0).isTriggered()).isTrue();
        }
    }

    @Test
    void createAlertShouldRejectDuplicateAlert() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            PriceAlertCreateRequest request = createRequest();
            when(priceCompareService.normalizeProductType("hotel")).thenReturn("HOTEL");
            when(priceCompareService.getProductSnapshot("HOTEL", 8L))
                    .thenReturn(new PriceCompareServiceImpl.ProductSnapshot("HOTEL", 8L, "West Lake Hotel", new BigDecimal("320")));
            when(priceAlertMapper.selectOne(any())).thenReturn(alert(9L, 1L, "HOTEL", 8L, new BigDecimal("300"), "dup"));

            assertThatThrownBy(() -> service.createAlert(request)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void createAlertShouldPersistAndReturnSnapshotInfo() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            PriceAlertCreateRequest request = createRequest();
            when(priceCompareService.normalizeProductType("hotel")).thenReturn("HOTEL");
            when(priceCompareService.getProductSnapshot("HOTEL", 8L))
                    .thenReturn(new PriceCompareServiceImpl.ProductSnapshot("HOTEL", 8L, "West Lake Hotel", new BigDecimal("320")));
            when(priceAlertMapper.selectOne(any())).thenReturn(null);

            ArgumentCaptor<PriceAlert> captor = ArgumentCaptor.forClass(PriceAlert.class);
            when(priceAlertMapper.insert(captor.capture())).thenAnswer(invocation -> {
                captor.getValue().setId(5L);
                captor.getValue().setCreateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
                return 1;
            });

            PriceAlertVO result = service.createAlert(request);

            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
            assertThat(captor.getValue().getProductType()).isEqualTo("HOTEL");
            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getProductName()).isEqualTo("West Lake Hotel");
            assertThat(result.isTriggered()).isFalse();
        }
    }

    @Test
    void deleteAlertShouldRejectUnownedAlert() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(priceAlertMapper.selectById(3L)).thenReturn(alert(3L, 2L, "HOTEL", 8L, new BigDecimal("300"), "other"));

            assertThatThrownBy(() -> service.deleteAlert(3L)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void deleteAlertShouldRemoveOwnedAlert() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(priceAlertMapper.selectById(3L)).thenReturn(alert(3L, 1L, "HOTEL", 8L, new BigDecimal("300"), "mine"));

            service.deleteAlert(3L);

            verify(priceAlertMapper).deleteById(3L);
        }
    }

    private PriceAlertCreateRequest createRequest() {
        PriceAlertCreateRequest request = new PriceAlertCreateRequest();
        request.setProductType("hotel");
        request.setProductId(8L);
        request.setTargetPrice(new BigDecimal("300"));
        request.setRemark("watch");
        return request;
    }

    private PriceAlert alert(Long id, Long userId, String productType, Long productId, BigDecimal targetPrice, String remark) {
        PriceAlert alert = new PriceAlert();
        alert.setId(id);
        alert.setUserId(userId);
        alert.setProductType(productType);
        alert.setProductId(productId);
        alert.setTargetPrice(targetPrice);
        alert.setStatus(1);
        alert.setRemark(remark);
        alert.setCreateTime(LocalDateTime.of(2026, 1, 1, 9, 0));
        return alert;
    }
}
