package com.travelplatform.product.util;

import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;

public final class ProductMediaUtils {

    private ProductMediaUtils() {
    }

    public static List<String> parseImageList(String rawValue, String fallbackImage) {
        List<String> imageList = splitToList(rawValue);
        if (!imageList.isEmpty()) {
            return imageList;
        }
        return splitToList(fallbackImage);
    }

    public static String normalizeImageList(String rawValue) {
        return String.join("\n", splitToList(rawValue));
    }

    private static List<String> splitToList(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return List.of();
        }
        return Arrays.stream(rawValue.split("[\\r\\n,]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
