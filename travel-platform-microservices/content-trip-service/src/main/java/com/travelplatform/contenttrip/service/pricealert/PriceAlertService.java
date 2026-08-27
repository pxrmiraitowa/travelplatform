package com.travelplatform.contenttrip.service.pricealert;

import com.travelplatform.contenttrip.dto.pricealert.PriceAlertCreateRequest;
import com.travelplatform.contenttrip.vo.pricealert.PriceAlertVO;
import java.util.List;

public interface PriceAlertService {

    List<PriceAlertVO> listCurrentUserAlerts();

    PriceAlertVO createAlert(PriceAlertCreateRequest request);

    void deleteAlert(Long id);
}
