package com.travelplatform.product.service.attraction.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.product.entity.Attraction;
import com.travelplatform.product.mapper.AttractionMapper;
import com.travelplatform.product.service.attraction.AttractionCatalogService;
import com.travelplatform.product.vo.attraction.AttractionSnapshotVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AttractionCatalogServiceImpl implements AttractionCatalogService {
    private final AttractionMapper attractionMapper;

    public AttractionCatalogServiceImpl(AttractionMapper attractionMapper) {
        this.attractionMapper = attractionMapper;
    }

    @Override
    public List<AttractionSnapshotVO> list(String city, List<Long> ids) {
        LambdaQueryWrapper<Attraction> query = new LambdaQueryWrapper<Attraction>()
                .eq(Attraction::getStatus, 1)
                .in(ids != null && !ids.isEmpty(), Attraction::getId, ids)
                .like(StringUtils.hasText(city), Attraction::getCity, normalizeCity(city))
                .orderByDesc(Attraction::getPriority)
                .orderByAsc(Attraction::getId);
        return attractionMapper.selectList(query).stream().map(AttractionSnapshotVO::from).toList();
    }

    private String normalizeCity(String city) {
        return city == null ? null : city.trim().replace("市", "").replace("特别行政区", "").replace("自治区", "");
    }
}
