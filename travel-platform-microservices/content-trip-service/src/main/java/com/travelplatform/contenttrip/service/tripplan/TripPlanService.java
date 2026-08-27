package com.travelplatform.contenttrip.service.tripplan;

import com.travelplatform.contenttrip.dto.tripplan.TripPlanCreateRequest;
import com.travelplatform.contenttrip.dto.tripplan.TripPlanItemCreateRequest;
import com.travelplatform.contenttrip.dto.tripplan.TripPlanItemUpdateRequest;
import com.travelplatform.contenttrip.dto.tripplan.TripPlanUpdateRequest;
import com.travelplatform.contenttrip.vo.tripplan.TripPlanDetailVO;
import com.travelplatform.contenttrip.vo.tripplan.TripPlanItemVO;
import com.travelplatform.contenttrip.vo.tripplan.TripPlanListItemVO;
import java.util.List;

public interface TripPlanService {

    List<TripPlanListItemVO> listCurrentUserPlans();

    TripPlanDetailVO createPlan(TripPlanCreateRequest request);

    TripPlanDetailVO getCurrentUserPlanDetail(Long id);

    TripPlanDetailVO updatePlan(Long id, TripPlanUpdateRequest request);

    void deletePlan(Long id);

    TripPlanItemVO createPlanItem(Long planId, TripPlanItemCreateRequest request);

    TripPlanItemVO updatePlanItem(Long planId, Long itemId, TripPlanItemUpdateRequest request);

    void deletePlanItem(Long planId, Long itemId);
}
