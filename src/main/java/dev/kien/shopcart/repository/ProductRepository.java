package dev.kien.shopcart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.kien.shopcart.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByIdAsc();
}
