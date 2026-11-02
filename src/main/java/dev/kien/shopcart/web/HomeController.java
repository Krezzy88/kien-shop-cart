package dev.kien.shopcart.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dev.kien.shopcart.service.ProductCatalogService;
import dev.kien.shopcart.service.ShoppingCartService;

@Controller
public class HomeController {

    private final ProductCatalogService catalog;
    private final ShoppingCartService cart;
    private final Long demoCartId;

    public HomeController(
            ProductCatalogService catalog,
            ShoppingCartService cart,
            @Value("${shop.demo-cart-id:1}") Long demoCartId) {
        this.catalog = catalog;
        this.cart = cart;
        this.demoCartId = demoCartId;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("products", catalog.getCatalog());
        model.addAttribute("cartLines", cart.getLines(demoCartId));
        model.addAttribute("cartId", demoCartId);
        model.addAttribute("cartTotal", cart.total(demoCartId));
        return "index";
    }
}
