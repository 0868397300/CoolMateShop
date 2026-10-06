-- ===================================================================
-- COOLMATESHOP - APPLICATION SCHEMA DEFINITION
-- ===================================================================

CREATE TABLE IF NOT EXISTS Roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS Users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    role_id BIGINT NOT NULL,
    email NVARCHAR(100) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    phone_number NVARCHAR(20) NULL,
    avatar_url NVARCHAR(255) NULL,
    gender NVARCHAR(10) NULL,
    date_of_birth DATE NULL,
    height_cm INT NULL,
    weight_kg INT NULL,
    coolcash_balance DECIMAL(18,2) NOT NULL DEFAULT 0,
    membership_tier NVARCHAR(20) NOT NULL DEFAULT 'NEW',
    total_spent DECIMAL(18,2) NOT NULL DEFAULT 0,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NULL
);

CREATE TABLE IF NOT EXISTS Categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    slug NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(500) NULL,
    image_url NVARCHAR(255) NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Collections (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    slug NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(500) NULL,
    banner_url NVARCHAR(255) NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    category_id BIGINT NOT NULL,
    collection_id BIGINT NULL,
    name NVARCHAR(200) NOT NULL,
    slug NVARCHAR(200) NOT NULL UNIQUE,
    short_description NVARCHAR(500) NULL,
    description NVARCHAR(MAX) NULL,
    material NVARCHAR(255) NULL,
    fit_type NVARCHAR(50) NULL,
    features NVARCHAR(MAX) NULL,
    base_price DECIMAL(18,2) NOT NULL,
    rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    sold_count INT NOT NULL DEFAULT 0,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NULL
);

CREATE TABLE IF NOT EXISTS Colors (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL,
    hex_code NVARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS Sizes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(20) NOT NULL,
    display_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS Product_Variants (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    color_id BIGINT NOT NULL,
    size_id BIGINT NOT NULL,
    sku NVARCHAR(100) NOT NULL UNIQUE,
    original_price DECIMAL(18,2) NOT NULL,
    sale_price DECIMAL(18,2) NOT NULL,
    import_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    stock_quantity INT NOT NULL DEFAULT 0,
    weight_gram INT NOT NULL DEFAULT 200,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NULL
);

CREATE TABLE IF NOT EXISTS Carts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NULL,
    session_id NVARCHAR(100) NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NULL
);

CREATE TABLE IF NOT EXISTS Cart_Items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NULL
);

CREATE TABLE IF NOT EXISTS Orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_code NVARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    promotion_id BIGINT NULL,
    recipient_name NVARCHAR(100) NOT NULL,
    recipient_phone NVARCHAR(20) NOT NULL,
    recipient_email NVARCHAR(100) NULL,
    shipping_address NVARCHAR(500) NOT NULL,
    note NVARCHAR(500) NULL,
    order_status NVARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method NVARCHAR(20) NOT NULL DEFAULT 'COD',
    payment_status NVARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    subtotal_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    combo_discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    voucher_discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    coolcash_used DECIMAL(18,2) NOT NULL DEFAULT 0,
    coolcash_earned DECIMAL(18,2) NOT NULL DEFAULT 0,
    shipping_fee DECIMAL(18,2) NOT NULL DEFAULT 0,
    final_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    vnpay_transaction_no NVARCHAR(100) NULL,
    vnpay_txn_ref NVARCHAR(100) NULL,
    payment_paid_at DATETIME2 NULL,
    cancelled_at DATETIME2 NULL,
    cancelled_reason NVARCHAR(500) NULL,
    payment_response_code NVARCHAR(50) NULL,
    payment_bank_code NVARCHAR(50) NULL,
    payment_failure_reason NVARCHAR(500) NULL,
    delivered_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NULL
);

CREATE TABLE IF NOT EXISTS Order_Items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    product_name_snapshot NVARCHAR(200) NOT NULL,
    sku_snapshot NVARCHAR(100) NOT NULL,
    color_name_snapshot NVARCHAR(50) NULL,
    size_name_snapshot NVARCHAR(20) NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    total_price DECIMAL(18,2) NOT NULL,
    cost_price_snapshot DECIMAL(18,2) NULL
);

CREATE TABLE IF NOT EXISTS Inventory_Receipts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    receipt_code NVARCHAR(50) NOT NULL UNIQUE,
    supplier_name NVARCHAR(200) NOT NULL,
    created_by BIGINT NULL,
    approved_by BIGINT NULL,
    approved_at DATETIME2 NULL,
    rejected_reason NVARCHAR(500) NULL,
    note NVARCHAR(500) NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Inventory_Movements (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    variant_id BIGINT NOT NULL,
    movement_type NVARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    before_quantity INT NULL,
    after_quantity INT NULL,
    reference_type NVARCHAR(50) NULL,
    reference_id BIGINT NULL,
    created_by BIGINT NULL,
    note NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Promotion_Usages (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    order_id BIGINT NULL,
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    status NVARCHAR(30) NOT NULL DEFAULT 'RESERVED',
    used_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Wishlists (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NULL
);

CREATE TABLE IF NOT EXISTS Wishlist_Items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    wishlist_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT UQ_WishlistItems_Wishlist_Product UNIQUE (wishlist_id, product_id)
);

CREATE TABLE IF NOT EXISTS Password_Reset_Tokens (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    token NVARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME2 NOT NULL,
    is_used BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);
