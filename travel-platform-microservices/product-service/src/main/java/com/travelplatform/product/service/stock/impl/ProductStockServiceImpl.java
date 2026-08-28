package com.travelplatform.product.service.stock.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.product.dto.internal.StockChangeRequest;
import com.travelplatform.product.entity.Flight;
import com.travelplatform.product.entity.HotelRoom;
import com.travelplatform.product.entity.TourPackage;
import com.travelplatform.product.entity.TrainTicket;
import com.travelplatform.product.mapper.FlightMapper;
import com.travelplatform.product.mapper.HotelRoomMapper;
import com.travelplatform.product.mapper.TourPackageMapper;
import com.travelplatform.product.mapper.TrainTicketMapper;
import com.travelplatform.product.service.stock.ProductStockService;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductStockServiceImpl implements ProductStockService {
    private final FlightMapper flightMapper;
    private final TrainTicketMapper trainTicketMapper;
    private final HotelRoomMapper hotelRoomMapper;
    private final TourPackageMapper tourPackageMapper;

    public ProductStockServiceImpl(FlightMapper flightMapper, TrainTicketMapper trainTicketMapper,
                                   HotelRoomMapper hotelRoomMapper, TourPackageMapper tourPackageMapper) {
        this.flightMapper = flightMapper;
        this.trainTicketMapper = trainTicketMapper;
        this.hotelRoomMapper = hotelRoomMapper;
        this.tourPackageMapper = tourPackageMapper;
    }

    @Override
    @Transactional
    public void deduct(StockChangeRequest request) {
        if (change(request, -request.getQuantity()) != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品库存不足或商品已下架");
        }
    }

    @Override
    @Transactional
    public void restore(StockChangeRequest request) {
        if (change(request, request.getQuantity()) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "库存恢复失败，商品不存在");
        }
    }

    private int change(StockChangeRequest request, int delta) {
        String type = request.getProductType().trim().toUpperCase(Locale.ROOT);
        int quantity = request.getQuantity();
        boolean deduct = delta < 0;
        return switch (type) {
            case "FLIGHT" -> flightMapper.update(null, new LambdaUpdateWrapper<Flight>()
                    .eq(Flight::getId, request.getProductId()).eq(deduct, Flight::getStatus, 1)
                    .ge(deduct, Flight::getStock, quantity).setSql("stock = stock " + signed(delta)));
            case "HOTEL" -> {
                if (request.getVariantId() == null) throw badRequest("酒店订单缺少房型 ID");
                yield hotelRoomMapper.update(null, new LambdaUpdateWrapper<HotelRoom>()
                        .eq(HotelRoom::getId, request.getVariantId()).eq(HotelRoom::getHotelId, request.getProductId())
                        .eq(deduct, HotelRoom::getStatus, 1).ge(deduct, HotelRoom::getStock, quantity)
                        .setSql("stock = stock " + signed(delta)));
            }
            case "TOUR" -> tourPackageMapper.update(null, new LambdaUpdateWrapper<TourPackage>()
                    .eq(TourPackage::getId, request.getProductId()).eq(deduct, TourPackage::getStatus, 1)
                    .ge(deduct, TourPackage::getStock, quantity).setSql("stock = stock " + signed(delta)));
            case "TRAIN" -> changeTrain(request, delta, deduct, quantity);
            default -> throw badRequest("不支持的商品类型");
        };
    }

    private int changeTrain(StockChangeRequest request, int delta, boolean deduct, int quantity) {
        LambdaUpdateWrapper<TrainTicket> update = new LambdaUpdateWrapper<TrainTicket>()
                .eq(TrainTicket::getId, request.getProductId()).eq(deduct, TrainTicket::getStatus, 1);
        return switch (request.getVariantName() == null ? "" : request.getVariantName().trim()) {
            case "商务座" -> trainTicketMapper.update(null, update.ge(deduct, TrainTicket::getBusinessStock, quantity)
                    .setSql("business_stock = business_stock " + signed(delta)));
            case "一等座" -> trainTicketMapper.update(null, update.ge(deduct, TrainTicket::getFirstClassStock, quantity)
                    .setSql("first_class_stock = first_class_stock " + signed(delta)));
            case "二等座" -> trainTicketMapper.update(null, update.ge(deduct, TrainTicket::getSecondClassStock, quantity)
                    .setSql("second_class_stock = second_class_stock " + signed(delta)));
            default -> throw badRequest("火车票订单缺少有效席别");
        };
    }

    private String signed(int delta) {
        return delta < 0 ? "- " + Math.abs(delta) : "+ " + delta;
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }
}
