package dev.kien.shopcart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.kien.shopcart.domain.Product;
import dev.kien.shopcart.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceTest {

    @Mock
    private ProductRepository products;

    @InjectMocks
    private ProductCatalogService service;

    @Test
    void getCatalogReturnsProductsInRepositoryOrder() {
        List<Product> expected = List.of();
        when(products.findAllByOrderByIdAsc()).thenReturn(expected);

        assertEquals(expected, service.getCatalog());
        verify(products).findAllByOrderByIdAsc();
    }
}
