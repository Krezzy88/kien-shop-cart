package dev.kien.shopcart.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.kien.shopcart.service.ShoppingCartService;

@Controller
@RequestMapping("/carts/{cartId}")
public class CartController {

    private final ShoppingCartService cart;

    public CartController(ShoppingCartService cart) {
        this.cart = cart;
    }

    @PostMapping("/products/{productId}")
    public String add(@PathVariable Long cartId, @PathVariable Long productId) {
        cart.addProduct(cartId, productId);
        return redirectHome();
    }

    @PostMapping("/lines/{lineId}/increase")
    public String increase(@PathVariable Long cartId, @PathVariable Long lineId) {
        cart.increase(cartId, lineId);
        return redirectHome();
    }

    @PostMapping("/lines/{lineId}/decrease")
    public String decrease(@PathVariable Long cartId, @PathVariable Long lineId) {
        cart.decrease(cartId, lineId);
        return redirectHome();
    }

    @PostMapping("/lines/{lineId}/remove")
    public String remove(@PathVariable Long cartId, @PathVariable Long lineId) {
        cart.remove(cartId, lineId);
        return redirectHome();
    }

    @PostMapping("/clear")
    public String clear(@PathVariable Long cartId) {
        cart.clear(cartId);
        return redirectHome();
    }

    private String redirectHome() {
        return "redirect:/";
    }
}
