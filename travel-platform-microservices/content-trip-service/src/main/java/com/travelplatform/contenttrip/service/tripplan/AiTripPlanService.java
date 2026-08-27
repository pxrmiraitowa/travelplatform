package com.travelplatform.contenttrip.service.tripplan;

import com.travelplatform.contenttrip.dto.tripplan.AiTripPlanPreviewRequest;
import com.travelplatform.contenttrip.dto.tripplan.AiTripPlanSaveRequest;
import com.travelplatform.contenttrip.vo.tripplan.AiTripPlanPreviewVO;
import com.travelplatform.contenttrip.vo.tripplan.TripPlanDetailVO;

public interface AiTripPlanService {

    AiTripPlanPreviewVO buildPreview(AiTripPlanPreviewRequest request);

    TripPlanDetailVO savePlan(AiTripPlanSaveRequest request);
}
