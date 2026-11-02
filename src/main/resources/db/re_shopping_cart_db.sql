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

INSERT INTO products (name, description, category, price) VALUES
('Everyday Phone Case', 'A lightweight protective case with a clean, durable finish.', 'TECH', 155000.00),
('Compact USB-C Charger', 'A small fast charger for your everyday carry.', 'TECH', 150000.00),
('Insulated Bottle', 'A reusable bottle designed to keep drinks at the right temperature.', 'HOME', 220000.00),
('Portable Power Bank', 'Reliable extra power for travel and busy days.', 'TECH', 320000.00);

INSERT INTO shopping_carts (label) VALUES ('Demo cart');

INSERT INTO cart_lines (cart_id, product_id, quantity) VALUES
(1, 1, 1),
(1, 2, 1);
