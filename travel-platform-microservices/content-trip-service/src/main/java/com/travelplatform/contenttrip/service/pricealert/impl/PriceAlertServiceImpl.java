package com.travelplatform.contenttrip.service.pricealert.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.contenttrip.dto.pricealert.PriceAlertCreateRequest;
import com.travelplatform.contenttrip.entity.PriceAlert;
import com.travelplatform.contenttrip.mapper.PriceAlertMapper;
import com.travelplatform.contenttrip.security.CurrentUserProvider;
import com.travelplatform.contenttrip.service.pricealert.PriceAlertService;
import com.travelplatform.contenttrip.service.product.ProductSnapshot;
import com.travelplatform.contenttrip.service.product.ProductSnapshotService;
import com.travelplatform.contenttrip.vo.pricealert.PriceAlertVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PriceAlertServiceImpl implements PriceAlertService {

    private final PriceAlertMapper priceAlertMapper;
    private final ProductSnapshotService productSnapshotService;
    private final CurrentUserProvider currentUserProvider;

    public PriceAlertServiceImpl(PriceAlertMapper priceAlertMapper,
                                 ProductSnapshotService productSnapshotService,
                                 CurrentUserProvider currentUserProvider) {
        this.priceAlertMapper = priceAlertMapper;
        this.productSnapshotService = productSnapshotService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<PriceAlertVO> listCurrentUserAlerts() {
        Long userId = currentUserProvider.getCurrentUserId();
        return priceAlertMapper.selectList(new LambdaQueryWrapper<PriceAlert>()
                        .eq(PriceAlert::getUserId, userId)
                        .eq(PriceAlert::getStatus, 1)
                        .orderByDesc(PriceAlert::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public PriceAlertVO createAlert(PriceAlertCreateRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        String productType = productSnapshotService.normalizeProductType(request.getProductType());
        ProductSnapshot snapshot = productSnapshotService.getProductSnapshot(productType, request.getProductId());

        PriceAlert existing = priceAlertMapper.selectOne(new LambdaQueryWrapper<PriceAlert>()
                .eq(PriceAlert::getUserId, userId)
                .eq(PriceAlert::getProductType, productType)
                .eq(PriceAlert::getProductId, request.getProductId())
                .eq(PriceAlert::getStatus, 1)
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Price alert already exists for this product");
        }

        PriceAlert alert = new PriceAlert();
        alert.setUserId(userId);
        alert.setProductType(snapshot.getProductType());
        alert.setProductId(snapshot.getProductId());
        alert.setTargetPrice(request.getTargetPrice());
        alert.setStatus(1);
        alert.setRemark(request.getRemark());
        priceAlertMapper.insert(alert);
        return toVO(alert, snapshot);
    }

    @Override
    public void deleteAlert(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        PriceAlert alert = priceAlertMapper.selectById(id);
        if (alert == null || !userId.equals(alert.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Price alert not found");
        }
        priceAlertMapper.deleteById(id);
    }

    private PriceAlertVO toVO(PriceAlert alert) {
        ProductSnapshot snapshot = productSnapshotService.getProductSnapshot(alert.getProductType(), alert.getProductId());
        return toVO(alert, snapshot);
    }

    private PriceAlertVO toVO(PriceAlert alert, ProductSnapshot snapshot) {
        PriceAlertVO vo = new PriceAlertVO();
        vo.setId(alert.getId());
        vo.setProductType(alert.getProductType());
        vo.setProductId(alert.getProductId());
        vo.setProductName(snapshot.getProductName());
        vo.setCurrentPrice(snapshot.getCurrentPrice());
        vo.setTargetPrice(alert.getTargetPrice());
        vo.setTriggered(snapshot.getCurrentPrice().compareTo(alert.getTargetPrice()) <= 0);
        vo.setStatusText(vo.isTriggered() ? "已达到提醒条件" : "等待降价");
        vo.setRemark(alert.getRemark());
        vo.setCreateTime(alert.getCreateTime());
        return vo;
    }
}
