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
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
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

    public OrderDto createOrder(CreateOrderRequest request) {
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
        for (OrderItemRequest item : request.items()) {
            quantityByProductId.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Map<Long, ProductDto> productCache = new HashMap<>();
        boolean productDegraded = false;
        for (Long productId : quantityByProductId.keySet()) {
            try {
                productCache.put(productId, fetchAndValidateProduct(productId));
            } catch (ProductServiceUnavailableException e) {
                log.warn("Деградация: не удалось получить данные товара id={}", productId, e);
                productDegraded = true;
            }
        }

        List<ReserveRequest> successfulReserves = new ArrayList<>();
        boolean inventoryDegraded = false;
        try {
            for (Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {
                ReserveRequest rr = new ReserveRequest(entry.getKey(), entry.getValue());
                reserveStock(rr);
                successfulReserves.add(rr);
            }
        } catch (InventoryServiceUnavailableException e) {
            log.warn("Деградация: inventory-service недоступен, откатываем успешные резервы", e);
            inventoryDegraded = true;
            compensate(successfulReserves);
            successfulReserves.clear();
        } catch (OrderProcessingException e) {
            compensate(successfulReserves);
            throw e;
        }

        boolean degraded = productDegraded || inventoryDegraded;
        Order order = buildOrder(request, quantityByProductId, productCache, degraded);
        return orderService.save(order);
    }

    private ProductDto fetchAndValidateProduct(Long productId) {
        ProductDto product;
        try {
            product = productClient.getProductById(productId);
        } catch (ProductServiceUnavailableException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException("Товар с id=" + productId + " не найден");
        } catch (FeignException e) {
            log.error("Неожиданная ошибка product-service id={}", productId, e);
            throw new ProductServiceUnavailableException(productId, e);
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
        } catch (InventoryServiceUnavailableException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException(
                    "Складская запись для товара " + request.productId() + " не найдена");
        } catch (FeignException.Conflict e) {
            throw new OrderProcessingException(
                    "Недостаточно товара " + request.productId() + " на складе");
        } catch (FeignException e) {
            log.error("Неожиданная ошибка inventory-service id={}", request.productId(), e);
            throw new InventoryServiceUnavailableException(request.productId(), e);
        }

        if (!response.success()) {
            throw new OrderProcessingException(
                    "Недостаточно товара " + request.productId() + " на складе");
        }
    }

    private void compensate(List<ReserveRequest> reserves) {
        for (ReserveRequest request : reserves) {
            try {
                inventoryClient.releaseStock(request);
                log.info("Снят резерв по товару {} в количестве {}",
                        request.productId(), request.quantity());
            } catch (Exception ex) {
                log.error("Не удалось снять резерв по товару {} в количестве {}",
                        request.productId(), request.quantity(), ex);
            }
        }
    }

    private Order buildOrder(CreateOrderRequest request,
                             Map<Long, Integer> quantityByProductId,
                             Map<Long, ProductDto> productCache,
                             boolean degraded) {
        Order order = new Order();
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(degraded ? OrderStatus.PENDING_CONFIRMATION : OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());
        if (degraded) {
            order.setStatusDetails(
                    "Заказ требует ручной проверки: часть данных о товарах или остатках недоступна");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            ProductDto product = productCache.get(productId);

            OrderItem item = new OrderItem();
            item.setProductId(productId);
            item.setQuantity(quantity);
            if (product != null) {
                item.setProductName(product.name());
                item.setPrice(product.price());
            } else {
                item.setProductName("Товар #" + productId + " (ожидает проверки)");
                item.setPrice(BigDecimal.ZERO);
            }
            order.addItem(item);

            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotalPrice(total);
        return order;
    }
}