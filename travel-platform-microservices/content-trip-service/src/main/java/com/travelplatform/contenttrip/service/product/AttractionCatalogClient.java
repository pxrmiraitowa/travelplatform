package com.travelplatform.contenttrip.service.product;

import java.util.Collection;
import java.util.List;

public interface AttractionCatalogClient {
    List<AttractionSnapshot> listByCity(String city);
    List<AttractionSnapshot> listByIds(Collection<Long> ids);
}
