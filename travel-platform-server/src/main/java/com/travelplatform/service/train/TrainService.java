package com.travelplatform.service.train;

import com.travelplatform.dto.train.TrainQueryRequest;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.train.TrainDetailVO;
import com.travelplatform.vo.train.TrainListItemVO;

public interface TrainService {

    PageResult<TrainListItemVO> searchTrains(TrainQueryRequest request);

    TrainDetailVO getTrainDetail(Long id);
}
