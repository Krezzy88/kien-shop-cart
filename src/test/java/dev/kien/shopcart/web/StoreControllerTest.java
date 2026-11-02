package dev.kien.shopcart.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import dev.kien.shopcart.domain.CartLine;
import dev.kien.shopcart.domain.Product;
import dev.kien.shopcart.domain.ShoppingCart;
import dev.kien.shopcart.service.ProductCatalogService;
import dev.kien.shopcart.service.ShoppingCartService;

class StoreControllerTest {

    @Test
    void homePopulatesCatalogAndCart() {
        ProductCatalogService catalog = org.mockito.Mockito.mock(ProductCatalogService.class);
        ShoppingCartService cart = org.mockito.Mockito.mock(ShoppingCartService.class);
        Product product = new Product("Notebook", "A simple notebook", "HOME", new BigDecimal("12.50"));
        List<Product> catalogProducts = List.of(product);
        when(catalog.getCatalog()).thenReturn(catalogProducts);
        when(cart.getLines(1L)).thenReturn(List.of(new CartLine(new ShoppingCart("Demo"), product, 2)));
        when(cart.total(1L)).thenReturn(new BigDecimal("25.00"));
        HomeController controller = new HomeController(catalog, cart, 1L);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("index", controller.home(model));
        assertEquals(catalogProducts, model.get("products"));
        assertEquals(1L, model.get("cartId"));
        assertEquals(new BigDecimal("25.00"), model.get("cartTotal"));
        assertEquals(1, ((List<?>) model.get("cartLines")).size());
    }

    @Test
    void cartActionsDelegateAndRedirectHome() {
        ShoppingCartService service = org.mockito.Mockito.mock(ShoppingCartService.class);
        CartController controller = new CartController(service);

        assertEquals("redirect:/", controller.add(2L, 3L));
        assertEquals("redirect:/", controller.increase(2L, 4L));
        assertEquals("redirect:/", controller.decrease(2L, 4L));
        assertEquals("redirect:/", controller.remove(2L, 4L));
        assertEquals("redirect:/", controller.clear(2L));
        verify(service).addProduct(2L, 3L);
        verify(service).increase(2L, 4L);
        verify(service).decrease(2L, 4L);
        verify(service).remove(2L, 4L);
        verify(service).clear(2L);
    }

    @Test
    void notFoundHandlerReturns404TemplateAndMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("error/404", handler.handleNotFound(
                new dev.kien.shopcart.error.ResourceNotFoundException("Product", 7L), model));
        assertTrue(model.get("message").toString().contains("Product was not found"));
    }
}
