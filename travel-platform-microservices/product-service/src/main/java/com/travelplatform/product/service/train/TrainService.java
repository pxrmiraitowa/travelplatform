package com.travelplatform.product.service.train;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.product.dto.train.TrainQueryRequest;
import com.travelplatform.product.vo.train.TrainDetailVO;
import com.travelplatform.product.vo.train.TrainListItemVO;

public interface TrainService {

    PageResult<TrainListItemVO> searchTrains(TrainQueryRequest request);

    TrainDetailVO getTrainDetail(Long id);
}
