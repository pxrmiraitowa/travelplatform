package com.travelplatform.service.pricealert;

import com.travelplatform.dto.pricealert.PriceAlertCreateRequest;
import com.travelplatform.vo.pricealert.PriceAlertVO;

import java.util.List;

public interface PriceAlertService {

    List<PriceAlertVO> listCurrentUserAlerts();

    PriceAlertVO createAlert(PriceAlertCreateRequest request);

    void deleteAlert(Long id);
}
