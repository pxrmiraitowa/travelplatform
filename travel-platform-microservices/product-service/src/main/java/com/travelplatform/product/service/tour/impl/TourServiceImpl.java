package com.travelplatform.product.service.tour.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.product.entity.TourPackage;
import com.travelplatform.product.mapper.TourPackageMapper;
import com.travelplatform.product.service.tour.TourService;
import com.travelplatform.product.util.ProductMediaUtils;
import com.travelplatform.product.vo.tour.TourDetailVO;
import com.travelplatform.product.vo.tour.TourListItemVO;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TourServiceImpl implements TourService {

    private final TourPackageMapper tourPackageMapper;

    public TourServiceImpl(TourPackageMapper tourPackageMapper) {
        this.tourPackageMapper = tourPackageMapper;
    }

    @Override
    public PageResult<TourListItemVO> listTours(String destination, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        List<TourListItemVO> records = tourPackageMapper.selectList(new LambdaQueryWrapper<TourPackage>()
                        .eq(TourPackage::getStatus, 1)
                        .like(StringUtils.hasText(destination), TourPackage::getDestination, destination)
                        .orderByAsc(TourPackage::getPrice)
                        .orderByAsc(TourPackage::getId))
                .stream()
                .map(this::toListItemVO)
                .toList();

        int total = records.size();
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, total);
        int toIndex = Math.min(fromIndex + safePageSize, total);

        PageResult<TourListItemVO> result = new PageResult<>();
        result.setRecords(records.subList(fromIndex, toIndex));
        result.setTotal(total);
        result.setPageNum(safePageNum);
        result.setPageSize(safePageSize);
        return result;
    }

    @Override
    public TourDetailVO getTourDetail(Long id) {
        TourPackage tourPackage = tourPackageMapper.selectById(id);
        if (tourPackage == null || !Integer.valueOf(1).equals(tourPackage.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "旅游产品不存在");
        }
        TourDetailVO detailVO = new TourDetailVO();
        copyInfo(tourPackage, detailVO);
        return detailVO;
    }

    private TourListItemVO toListItemVO(TourPackage tourPackage) {
        TourListItemVO vo = new TourListItemVO();
        copyInfo(tourPackage, vo);
        return vo;
    }

    private void copyInfo(TourPackage tourPackage, TourListItemVO vo) {
        vo.setId(tourPackage.getId());
        vo.setPackageName(tourPackage.getPackageName());
        vo.setDestination(tourPackage.getDestination());
        vo.setDepartureCity(tourPackage.getDepartureCity());
        vo.setDays(tourPackage.getDays());
        vo.setPrice(tourPackage.getPrice());
        vo.setStock(tourPackage.getStock());
        vo.setDescription(tourPackage.getDescription());
        vo.setCoverImage(tourPackage.getCoverImage());
        vo.setDetailImages(ProductMediaUtils.parseImageList(tourPackage.getDetailImages(), tourPackage.getCoverImage()));
        vo.setTravelDateOptions(parseTravelDates(tourPackage.getTravelDates()));
    }

    private List<String> parseTravelDates(String travelDates) {
        if (!StringUtils.hasText(travelDates)) {
            return List.of();
        }
        return Arrays.stream(travelDates.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
