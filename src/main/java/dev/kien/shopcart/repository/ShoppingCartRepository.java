package dev.kien.shopcart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.kien.shopcart.domain.ShoppingCart;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
}
