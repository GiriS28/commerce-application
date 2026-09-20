CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    sku VARCHAR(50) NOT NULL UNIQUE,

    name VARCHAR(150) NOT NULL,

    description TEXT,

    price NUMERIC(12, 2) NOT NULL,

    category VARCHAR(50) NOT NULL,

    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_products_price_positive
        CHECK (price > 0),

    CONSTRAINT chk_products_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED'))
);

CREATE INDEX idx_products_category
    ON products(category);

CREATE INDEX idx_products_status
    ON products(status);