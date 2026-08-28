package com.travelplatform.product.service.attraction;

import com.travelplatform.product.vo.attraction.AttractionSnapshotVO;
import java.util.List;

public interface AttractionCatalogService {
    List<AttractionSnapshotVO> list(String city, List<Long> ids);
}
