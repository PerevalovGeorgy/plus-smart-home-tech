package ru.yandex.practicum.inventory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.InventoryAlreadyExistsException;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<InventoryDto> findAll() {
        return inventoryRepository.findAll().stream()
                .map(InventoryService::toDto)
                .toList();
    }

    public InventoryDto findByProductId(Long productId) {
        return toDto(getOrThrow(productId));
    }

    @Transactional
    public InventoryDto create(UpdateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new InventoryAlreadyExistsException(
                    "Складская запись для товара " + request.productId() + " уже существует");
        }
        Inventory inventory = new Inventory(request.productId(), request.quantity());
        return toDto(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryDto update(UpdateInventoryRequest request) {
        Inventory inventory = getOrThrow(request.productId());

        if (request.quantity() < inventory.getReservedQuantity()) {
            throw new IllegalArgumentException(
                    "Нельзя установить количество (" + request.quantity()
                            + ") меньше уже зарезервированного (" + inventory.getReservedQuantity() + ")");
        }

        inventory.setQuantity(request.quantity());
        return toDto(inventory);
    }

    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
        Inventory inventory = getOrThrow(request.productId());

        int available = inventory.getAvailableQuantity();
        if (available < request.quantity()) {
            throw new InsufficientStockException(
                    "Недостаточно товара для товара " + request.productId()
                            + ": доступно " + available + ", запрошено " + request.quantity());
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        return new ReserveResponse(
                true,
                inventory.getAvailableQuantity(),
                "Товар успешно зарезервирован"
        );
    }

    private Inventory getOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException(
                        "Складская запись для товара " + productId + " не найдена"));
    }

    static InventoryDto toDto(Inventory inventory) {
        return new InventoryDto(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity()
        );
    }
}