package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.ProductDto;
import ru.yandex.practicum.order.dto.ReserveRequest;
import ru.yandex.practicum.order.dto.ReserveResponse;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOrchestrationService {

    private final OrderService orderService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    /**
     * Сценарий создания заказа. Внешние вызовы выполняются вне DB-транзакции;
     * сохранение заказа — отдельный @Transactional-метод OrderService.save.
     */
    public OrderDto createOrder(CreateOrderRequest request) {
        // 1. Агрегируем количество по productId: если productId встречается несколько раз,
        //    в inventory-service уйдёт ОДИН резерв с суммарным количеством.
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
        for (OrderItemRequest item : request.items()) {
            quantityByProductId.merge(item.productId(), item.quantity(), Integer::sum);
        }

        // 2. Запрашиваем каждый товар РОВНО ОДИН РАЗ (даже если он в нескольких позициях).
        Map<Long, ProductDto> productCache = new HashMap<>();
        for (Long productId : quantityByProductId.keySet()) {
            productCache.put(productId, fetchAndValidateProduct(productId));
        }

        // 3. Резервируем. Если что-то пошло не так — освобождаем уже созданные резервы.
        List<ReserveRequest> successfulReserves = new ArrayList<>();
        try {
            for (Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {
                ReserveRequest reserveRequest = new ReserveRequest(entry.getKey(), entry.getValue());
                reserveStock(reserveRequest);
                successfulReserves.add(reserveRequest);
            }
        } catch (RuntimeException e) {
            compensate(successfulReserves);
            throw e;
        }

        // 4. Сохраняем заказ со снимком товарных данных.
        Order order = buildOrder(request, quantityByProductId, productCache);
        return orderService.save(order);
    }

    private ProductDto fetchAndValidateProduct(Long productId) {
        ProductDto product;
        try {
            product = productClient.getProductById(productId);
        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException("Товар с id=" + productId + " не найден");
        } catch (FeignException e) {
            log.error("Ошибка вызова product-service для productId={}", productId, e);
            throw new OrderProcessingException(
                    "Не удалось получить данные товара. Повторите попытку позже");
        }

        if (!Boolean.TRUE.equals(product.active())) {
            throw new OrderProcessingException("Товар '" + product.name() + "' снят с продажи");
        }
        return product;
    }

    private void reserveStock(ReserveRequest request) {
        ReserveResponse response;
        try {
            response = inventoryClient.reserveStock(request);
        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException(
                    "Складская запись для товара " + request.productId() + " не найдена");
        } catch (FeignException.Conflict e) {
            throw new OrderProcessingException(
                    "Недостаточно товара " + request.productId() + " на складе");
        } catch (FeignException e) {
            log.error("Ошибка вызова inventory-service для productId={}",
                    request.productId(), e);
            throw new OrderProcessingException(
                    "Не удалось зарезервировать товар. Повторите попытку позже");
        }

        if (!response.success()) {
            throw new OrderProcessingException(
                    "Недостаточно товара " + request.productId() + " на складе");
        }
    }

    private void compensate(List<ReserveRequest> successfulReserves) {
        for (ReserveRequest request : successfulReserves) {
            try {
                inventoryClient.releaseStock(request);
                log.info("Снят резерв по товару {} в количестве {}",
                        request.productId(), request.quantity());
            } catch (Exception ex) {
                // Best-effort: не маскируем исходную ошибку, только логируем.
                log.error("Не удалось снять резерв по товару {} в количестве {}",
                        request.productId(), request.quantity(), ex);
            }
        }
    }

    private Order buildOrder(CreateOrderRequest request,
                             Map<Long, Integer> quantityByProductId,
                             Map<Long, ProductDto> productCache) {
        Order order = new Order();
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {
            ProductDto product = productCache.get(entry.getKey());

            OrderItem item = new OrderItem();
            item.setProductId(product.id());
            item.setProductName(product.name());
            item.setQuantity(entry.getValue());
            item.setPrice(product.price());
            order.addItem(item);

            total = total.add(product.price().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        order.setTotalPrice(total);

        return order;
    }
}