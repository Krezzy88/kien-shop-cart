package dev.kien.shopcart.service;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.kien.shopcart.domain.Product;
import dev.kien.shopcart.repository.ProductRepository;

@Service
public class ProductCatalogService {

    private final ProductRepository products;

    public ProductCatalogService(ProductRepository products) {
        this.products = products;
    }

    public List<Product> getCatalog() {
        return products.findAllByOrderByIdAsc();
    }
}
