CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(24) NOT NULL,
    price DECIMAL(10, 2) NOT NULL
);

CREATE TABLE shopping_carts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    label VARCHAR(100) NOT NULL
);

CREATE TABLE cart_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT fk_cart_lines_cart FOREIGN KEY (cart_id) REFERENCES shopping_carts(id),
    CONSTRAINT fk_cart_lines_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_cart_line_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT chk_cart_line_quantity CHECK (quantity > 0)
);
