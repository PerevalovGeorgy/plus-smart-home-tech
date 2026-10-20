package ru.yandex.practicum.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = @UniqueConstraint(columnNames = "product_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Version
    private Long version;

    public Inventory(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.reservedQuantity = 0;
    }

    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}