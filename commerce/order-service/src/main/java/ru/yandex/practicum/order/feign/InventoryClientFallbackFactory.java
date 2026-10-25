package ru.yandex.practicum.order.feign;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.dto.ReserveRequest;
import ru.yandex.practicum.order.dto.ReserveResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    private static final Logger log =
            LoggerFactory.getLogger(InventoryClientFallbackFactory.class);

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {

            @Override
            public ReserveResponse reserveStock(ReserveRequest request) {
                if (cause instanceof FeignException.NotFound nf) {
                    throw nf;
                }
                if (cause instanceof FeignException.Conflict c) {
                    throw c;
                }
                log.warn("inventory-service недоступен при резервировании id={}",
                        request.productId(), cause);
                throw new InventoryServiceUnavailableException(request.productId(), cause);
            }

            @Override
            public ReserveResponse releaseStock(ReserveRequest request) {
                if (cause instanceof FeignException.NotFound nf) {
                    throw nf;
                }
                if (cause instanceof FeignException.Conflict c) {
                    throw c;
                }
                log.warn("inventory-service недоступен при снятии резерва id={}",
                        request.productId(), cause);
                throw new InventoryServiceUnavailableException(request.productId(), cause);
            }
        };
    }
}