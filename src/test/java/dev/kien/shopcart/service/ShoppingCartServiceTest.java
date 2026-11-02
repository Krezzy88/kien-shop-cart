package dev.kien.shopcart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.kien.shopcart.domain.CartLine;
import dev.kien.shopcart.domain.Product;
import dev.kien.shopcart.domain.ShoppingCart;
import dev.kien.shopcart.error.ResourceNotFoundException;
import dev.kien.shopcart.repository.CartLineRepository;
import dev.kien.shopcart.repository.ProductRepository;
import dev.kien.shopcart.repository.ShoppingCartRepository;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

    private static final Long CART_ID = 4L;
    private static final Long PRODUCT_ID = 8L;

    @Mock
    private ShoppingCartRepository carts;

    @Mock
    private CartLineRepository lines;

    @Mock
    private ProductRepository products;

    @InjectMocks
    private ShoppingCartService service;

    private ShoppingCart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart("Test cart");
        product = new Product("Notebook", "A simple notebook", "HOME", new BigDecimal("12.50"));
    }

    @Test
    void getLinesRequiresCartAndReturnsLines() {
        List<CartLine> expected = List.of();
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(lines.findAllWithProductByCartId(CART_ID)).thenReturn(expected);

        assertSame(expected, service.getLines(CART_ID));
    }

    @Test
    void getLinesRejectsUnknownCart() {
        when(carts.findById(CART_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getLines(CART_ID));
    }

    @Test
    void addProductCreatesCartLine() {
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(products.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(lines.findByCartIdAndProductId(CART_ID, PRODUCT_ID)).thenReturn(Optional.empty());

        service.addProduct(CART_ID, PRODUCT_ID);

        org.mockito.ArgumentCaptor<CartLine> line = org.mockito.ArgumentCaptor.forClass(CartLine.class);
        verify(lines).save(line.capture());
        assertEquals(1, line.getValue().getQuantity());
        assertSame(product, line.getValue().getProduct());
    }

    @Test
    void addProductIncrementsExistingLine() {
        CartLine existing = new CartLine(cart, product, 2);
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(products.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(lines.findByCartIdAndProductId(CART_ID, PRODUCT_ID)).thenReturn(Optional.of(existing));

        service.addProduct(CART_ID, PRODUCT_ID);

        assertEquals(3, existing.getQuantity());
        verify(lines).save(existing);
    }

    @Test
    void addProductRejectsUnknownProduct() {
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(products.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.addProduct(CART_ID, PRODUCT_ID));
        verify(lines, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void increaseUpdatesLineQuantity() {
        CartLine line = new CartLine(cart, product, 2);
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(lines.findByIdAndCartId(9L, CART_ID)).thenReturn(Optional.of(line));

        service.increase(CART_ID, 9L);

        assertEquals(3, line.getQuantity());
        verify(lines).save(line);
    }

    @Test
    void decreaseUpdatesLineWhenQuantityRemainsPositive() {
        CartLine line = new CartLine(cart, product, 2);
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(lines.findByIdAndCartId(9L, CART_ID)).thenReturn(Optional.of(line));

        service.decrease(CART_ID, 9L);

        assertEquals(1, line.getQuantity());
        verify(lines).save(line);
        verify(lines, never()).delete(line);
    }

    @Test
    void decreaseDeletesLineAtQuantityOne() {
        CartLine line = new CartLine(cart, product, 1);
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(lines.findByIdAndCartId(9L, CART_ID)).thenReturn(Optional.of(line));

        service.decrease(CART_ID, 9L);

        verify(lines).delete(line);
        verify(lines, never()).save(line);
    }

    @Test
    void removeDeletesOwnedLine() {
        CartLine line = new CartLine(cart, product, 1);
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(lines.findByIdAndCartId(9L, CART_ID)).thenReturn(Optional.of(line));

        service.remove(CART_ID, 9L);

        verify(lines).delete(line);
    }

    @Test
    void clearRequiresCartAndDeletesItsLines() {
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));

        service.clear(CART_ID);

        verify(lines).deleteAllByCartId(CART_ID);
    }

    @Test
    void totalSumsProductPriceTimesQuantity() {
        List<CartLine> cartLines = List.of(
                new CartLine(cart, product, 2),
                new CartLine(cart, new Product("Pen", "A pen", "HOME", new BigDecimal("3.25")), 1));
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(lines.findAllWithProductByCartId(CART_ID)).thenReturn(cartLines);

        assertEquals(new BigDecimal("28.25"), service.total(CART_ID));
    }

    @Test
    void lineLookupRejectsUnknownLine() {
        when(carts.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(lines.findByIdAndCartId(9L, CART_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.remove(CART_ID, 9L));
    }
}
