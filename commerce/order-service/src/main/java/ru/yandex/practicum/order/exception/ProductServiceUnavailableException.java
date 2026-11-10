package ru.yandex.practicum.order.exception;

public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(Long productId, Throwable cause) {
        super("product-service технически недоступен для товара id=" + productId, cause);
    }
}
