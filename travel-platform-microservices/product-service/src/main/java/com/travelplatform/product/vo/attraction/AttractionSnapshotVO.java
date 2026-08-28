package com.travelplatform.product.vo.attraction;

import com.travelplatform.product.entity.Attraction;

public record AttractionSnapshotVO(Long id, String city, String district, String attractionName,
                                   String attractionType, String tags, String description,
                                   String suggestedDuration, Integer priority, Integer status) {
    public static AttractionSnapshotVO from(Attraction attraction) {
        return new AttractionSnapshotVO(attraction.getId(), attraction.getCity(), attraction.getDistrict(),
                attraction.getAttractionName(), attraction.getAttractionType(), attraction.getTags(),
                attraction.getDescription(), attraction.getSuggestedDuration(), attraction.getPriority(),
                attraction.getStatus());
    }
}
