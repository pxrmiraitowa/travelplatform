package com.travelplatform.service.tripplan;

import com.travelplatform.dto.tripplan.AiTripPlanPreviewRequest;
import com.travelplatform.dto.tripplan.AiTripPlanSaveRequest;
import com.travelplatform.vo.tripplan.AiTripPlanPreviewVO;
import com.travelplatform.vo.tripplan.TripPlanDetailVO;

public interface AiTripPlanService {

    AiTripPlanPreviewVO buildPreview(AiTripPlanPreviewRequest request);

    TripPlanDetailVO savePlan(AiTripPlanSaveRequest request);
}
