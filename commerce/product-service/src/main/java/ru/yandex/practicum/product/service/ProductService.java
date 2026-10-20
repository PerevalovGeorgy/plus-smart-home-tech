package ru.yandex.practicum.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.repository.CategoryRepository;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductDto> findActive() {
        return productRepository.findAllByActiveTrue().stream()
                .map(ProductService::toDto)
                .toList();
    }

    public ProductDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public List<ProductDto> findByCategory(Long categoryId) {
        return productRepository.findAllByCategoryIdAndActiveTrue(categoryId).stream()
                .map(ProductService::toDto)
                .toList();
    }

    public List<ProductDto> search(String query) {
        return productRepository.searchActiveByName(query).stream()
                .map(ProductService::toDto)
                .toList();
    }

    @Transactional
    public ProductDto create(CreateProductRequest request) {
        Category category = resolveCategory(request.categoryId());
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                category,
                request.imageUrl()
        );
        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto update(Long id, UpdateProductRequest request) {
        Product product = getOrThrow(id);

        if (request.name() != null)        product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null)       product.setPrice(request.price());
        if (request.categoryId() != null)  product.setCategory(resolveCategory(request.categoryId()));
        if (request.imageUrl() != null)    product.setImageUrl(request.imageUrl());
        if (request.active() != null)      product.setActive(request.active());

        return toDto(product);
    }

    private Product getOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Товар с id=" + id + " не найден"));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));
    }

    static ProductDto toDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                CategoryService.toDto(product.getCategory()),
                product.getImageUrl(),
                product.getActive()
        );
    }
}