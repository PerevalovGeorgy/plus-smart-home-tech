package ru.yandex.practicum.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.product.entity.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByActiveTrue();

    List<Product> findAllByCategoryIdAndActiveTrue(Long categoryId);

    @Query("""
            select p from Product p
            where lower(p.name) like lower(concat('%', :query, '%'))
              and p.active = true
            """)
    List<Product> searchActiveByName(@Param("query") String query);
}