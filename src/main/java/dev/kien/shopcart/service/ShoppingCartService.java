package dev.kien.shopcart.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.kien.shopcart.domain.CartLine;
import dev.kien.shopcart.domain.Product;
import dev.kien.shopcart.domain.ShoppingCart;
import dev.kien.shopcart.error.ResourceNotFoundException;
import dev.kien.shopcart.repository.CartLineRepository;
import dev.kien.shopcart.repository.ProductRepository;
import dev.kien.shopcart.repository.ShoppingCartRepository;

@Service
public class ShoppingCartService {

    private final ShoppingCartRepository carts;
    private final CartLineRepository lines;
    private final ProductRepository products;

    public ShoppingCartService(
            ShoppingCartRepository carts,
            CartLineRepository lines,
            ProductRepository products) {
        this.carts = carts;
        this.lines = lines;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<CartLine> getLines(Long cartId) {
        requireCart(cartId);
        return lines.findAllWithProductByCartId(cartId);
    }

    @Transactional
    public void addProduct(Long cartId, Long productId) {
        ShoppingCart cart = requireCart(cartId);
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        CartLine line = lines.findByCartIdAndProductId(cartId, productId)
                .map(existing -> {
                    existing.increase();
                    return existing;
                })
                .orElseGet(() -> new CartLine(cart, product, 1));
        lines.save(line);
    }

    @Transactional
    public void increase(Long cartId, Long lineId) {
        CartLine line = requireLine(cartId, lineId);
        line.increase();
        lines.save(line);
    }

    @Transactional
    public void decrease(Long cartId, Long lineId) {
        CartLine line = requireLine(cartId, lineId);
        if (line.decrease()) {
            lines.save(line);
        } else {
            lines.delete(line);
        }
    }

    @Transactional
    public void remove(Long cartId, Long lineId) {
        lines.delete(requireLine(cartId, lineId));
    }

    @Transactional
    public void clear(Long cartId) {
        requireCart(cartId);
        lines.deleteAllByCartId(cartId);
    }

    @Transactional(readOnly = true)
    public BigDecimal total(Long cartId) {
        return getLines(cartId).stream()
                .map(line -> line.getProduct().getPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ShoppingCart requireCart(Long cartId) {
        return carts.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
    }

    private CartLine requireLine(Long cartId, Long lineId) {
        requireCart(cartId);
        return lines.findByIdAndCartId(lineId, cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart line", lineId));
    }
}
