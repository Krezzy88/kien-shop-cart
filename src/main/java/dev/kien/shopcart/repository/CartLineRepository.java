package dev.kien.shopcart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.kien.shopcart.domain.CartLine;

public interface CartLineRepository extends JpaRepository<CartLine, Long> {

    @Query("select line from CartLine line join fetch line.product where line.cart.id = :cartId order by line.id")
    List<CartLine> findAllWithProductByCartId(@Param("cartId") Long cartId);

    Optional<CartLine> findByCartIdAndProductId(Long cartId, Long productId);

    Optional<CartLine> findByIdAndCartId(Long lineId, Long cartId);

    @Modifying
    @Query("delete from CartLine line where line.cart.id = :cartId")
    int deleteAllByCartId(@Param("cartId") Long cartId);
}
