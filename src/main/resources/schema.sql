-- ===================================================================
-- COOLMATESHOP - MICROSOFT SQL SERVER SCHEMA DEFINITION
-- ===================================================================

IF OBJECT_ID(N'[dbo].[Roles]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Roles] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [role_name] NVARCHAR(50) NOT NULL UNIQUE,
        [description] NVARCHAR(255) NULL
    );
END;

IF OBJECT_ID(N'[dbo].[Users]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Users] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [email] NVARCHAR(100) NOT NULL UNIQUE,
        [phone] NVARCHAR(20) NULL,
        [password_hash] NVARCHAR(255) NOT NULL,
        [full_name] NVARCHAR(100) NOT NULL,
        [gender] NVARCHAR(10) NULL,
        [birth_date] DATE NULL,
        [height_cm] INT NULL,
        [weight_kg] INT NULL,
        [membership_tier] NVARCHAR(30) NOT NULL DEFAULT 'NEW',
        [coolcash_balance] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [total_spent] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [is_active] BIT NOT NULL DEFAULT 1,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        [updated_at] DATETIME2 NULL
    );
END;

IF OBJECT_ID(N'[dbo].[User_Roles]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[User_Roles] (
        [user_id] BIGINT NOT NULL CONSTRAINT [FK_UserRoles_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]) ON DELETE CASCADE,
        [role_id] INT NOT NULL CONSTRAINT [FK_UserRoles_Role] FOREIGN KEY REFERENCES [dbo].[Roles]([id]) ON DELETE CASCADE,
        PRIMARY KEY ([user_id], [role_id])
    );
END;

IF OBJECT_ID(N'[dbo].[Categories]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Categories] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [parent_id] INT NULL CONSTRAINT [FK_Categories_Parent] FOREIGN KEY REFERENCES [dbo].[Categories]([id]),
        [name] NVARCHAR(150) NOT NULL,
        [slug] NVARCHAR(150) NOT NULL UNIQUE,
        [description] NVARCHAR(500) NULL,
        [image_url] NVARCHAR(500) NULL,
        [display_order] INT NOT NULL DEFAULT 0,
        [is_active] BIT NOT NULL DEFAULT 1
    );
END;

IF OBJECT_ID(N'[dbo].[Collections]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Collections] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [name] NVARCHAR(150) NOT NULL,
        [slug] NVARCHAR(150) NOT NULL UNIQUE,
        [description] NVARCHAR(500) NULL,
        [banner_url] NVARCHAR(500) NULL,
        [is_active] BIT NOT NULL DEFAULT 1
    );
END;

IF OBJECT_ID(N'[dbo].[Colors]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Colors] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [name] NVARCHAR(50) NOT NULL,
        [hex_code] NVARCHAR(20) NOT NULL
    );
END;

IF OBJECT_ID(N'[dbo].[Sizes]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Sizes] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [name] NVARCHAR(20) NOT NULL,
        [display_order] INT NOT NULL DEFAULT 0
    );
END;

IF OBJECT_ID(N'[dbo].[Products]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Products] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [category_id] INT NOT NULL CONSTRAINT [FK_Products_Category] FOREIGN KEY REFERENCES [dbo].[Categories]([id]),
        [collection_id] INT NULL CONSTRAINT [FK_Products_Collection] FOREIGN KEY REFERENCES [dbo].[Collections]([id]),
        [name] NVARCHAR(250) NOT NULL,
        [slug] NVARCHAR(250) NOT NULL UNIQUE,
        [short_description] NVARCHAR(500) NULL,
        [description] NVARCHAR(MAX) NULL,
        [material] NVARCHAR(200) NULL,
        [fit_type] NVARCHAR(50) NULL,
        [features] NVARCHAR(500) NULL,
        [base_price] DECIMAL(18,2) NOT NULL,
        [rating_avg] DECIMAL(3,2) NOT NULL DEFAULT 0,
        [review_count] INT NOT NULL DEFAULT 0,
        [sold_count] INT NOT NULL DEFAULT 0,
        [status] NVARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        [updated_at] DATETIME2 NULL
    );
END;

IF OBJECT_ID(N'[dbo].[Product_Variants]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Product_Variants] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [product_id] BIGINT NOT NULL CONSTRAINT [FK_Variants_Product] FOREIGN KEY REFERENCES [dbo].[Products]([id]) ON DELETE CASCADE,
        [color_id] INT NOT NULL CONSTRAINT [FK_Variants_Color] FOREIGN KEY REFERENCES [dbo].[Colors]([id]),
        [size_id] INT NOT NULL CONSTRAINT [FK_Variants_Size] FOREIGN KEY REFERENCES [dbo].[Sizes]([id]),
        [sku] NVARCHAR(100) NOT NULL UNIQUE,
        [original_price] DECIMAL(18,2) NOT NULL,
        [sale_price] DECIMAL(18,2) NOT NULL,
        [import_price] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [stock_quantity] INT NOT NULL DEFAULT 0,
        [weight_gram] INT NOT NULL DEFAULT 250,
        [is_active] BIT NOT NULL DEFAULT 1,
        [version] INT NOT NULL DEFAULT 0
    );
END;

IF OBJECT_ID(N'[dbo].[Product_Images]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Product_Images] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [product_id] BIGINT NOT NULL CONSTRAINT [FK_Images_Product] FOREIGN KEY REFERENCES [dbo].[Products]([id]) ON DELETE CASCADE,
        [color_id] INT NULL CONSTRAINT [FK_Images_Color] FOREIGN KEY REFERENCES [dbo].[Colors]([id]),
        [image_url] NVARCHAR(500) NOT NULL,
        [is_thumbnail] BIT NOT NULL DEFAULT 0,
        [display_order] INT NOT NULL DEFAULT 0
    );
END;

IF OBJECT_ID(N'[dbo].[Carts]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Carts] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [user_id] BIGINT NULL CONSTRAINT [FK_Carts_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]) ON DELETE CASCADE,
        [session_id] NVARCHAR(100) NULL,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        [updated_at] DATETIME2 NULL
    );
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_Carts_UserId_Filtered' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_Carts_UserId_Filtered] ON [dbo].[Carts]([user_id]) WHERE [user_id] IS NOT NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_Carts_SessionId_Filtered' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_Carts_SessionId_Filtered] ON [dbo].[Carts]([session_id]) WHERE [session_id] IS NOT NULL;
END;

IF OBJECT_ID(N'[dbo].[Cart_Items]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Cart_Items] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [cart_id] BIGINT NOT NULL CONSTRAINT [FK_CartItems_Cart] FOREIGN KEY REFERENCES [dbo].[Carts]([id]) ON DELETE CASCADE,
        [variant_id] BIGINT NOT NULL CONSTRAINT [FK_CartItems_Variant] FOREIGN KEY REFERENCES [dbo].[Product_Variants]([id]),
        [quantity] INT NOT NULL DEFAULT 1,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        [updated_at] DATETIME2 NULL
    );
END;

IF OBJECT_ID(N'[dbo].[Promotions]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Promotions] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [code] NVARCHAR(50) NOT NULL UNIQUE,
        [name] NVARCHAR(150) NOT NULL,
        [discount_type] NVARCHAR(20) NOT NULL,
        [discount_value] DECIMAL(18,2) NOT NULL,
        [min_order_value] DECIMAL(18,2) NULL,
        [max_discount_amount] DECIMAL(18,2) NULL,
        [usage_limit] INT NULL,
        [used_count] INT NOT NULL DEFAULT 0,
        [start_date] DATETIME2 NOT NULL,
        [end_date] DATETIME2 NOT NULL,
        [is_active] BIT NOT NULL DEFAULT 1
    );
END;

IF OBJECT_ID(N'[dbo].[Combo_Rules]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Combo_Rules] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [name] NVARCHAR(200) NOT NULL,
        [category_id] INT NOT NULL CONSTRAINT [FK_ComboRules_Category] FOREIGN KEY REFERENCES [dbo].[Categories]([id]),
        [min_quantity] INT NOT NULL,
        [discount_percentage] DECIMAL(5,2) NOT NULL,
        [is_active] BIT NOT NULL DEFAULT 1
    );
END;

IF OBJECT_ID(N'[dbo].[Orders]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Orders] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [order_code] NVARCHAR(50) NOT NULL UNIQUE,
        [user_id] BIGINT NULL CONSTRAINT [FK_Orders_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [promotion_id] BIGINT NULL CONSTRAINT [FK_Orders_Promotion] FOREIGN KEY REFERENCES [dbo].[Promotions]([id]),
        [recipient_name] NVARCHAR(100) NOT NULL,
        [recipient_phone] NVARCHAR(20) NOT NULL,
        [recipient_email] NVARCHAR(100) NULL,
        [shipping_address] NVARCHAR(500) NOT NULL,
        [note] NVARCHAR(500) NULL,
        [order_status] NVARCHAR(30) NOT NULL DEFAULT 'PENDING',
        [payment_method] NVARCHAR(20) NOT NULL DEFAULT 'COD',
        [payment_status] NVARCHAR(30) NOT NULL DEFAULT 'UNPAID',
        [subtotal_amount] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [combo_discount_amount] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [voucher_discount_amount] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [coolcash_used] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [coolcash_earned] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [shipping_fee] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [final_amount] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [vnpay_transaction_no] NVARCHAR(100) NULL,
        [vnpay_txn_ref] NVARCHAR(100) NULL,
        [payment_paid_at] DATETIME2 NULL,
        [cancelled_at] DATETIME2 NULL,
        [cancelled_reason] NVARCHAR(500) NULL,
        [payment_response_code] NVARCHAR(50) NULL,
        [payment_bank_code] NVARCHAR(50) NULL,
        [payment_failure_reason] NVARCHAR(500) NULL,
        [delivered_at] DATETIME2 NULL,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        [updated_at] DATETIME2 NULL
    );
END;

IF OBJECT_ID(N'[dbo].[Order_Items]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Order_Items] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [order_id] BIGINT NOT NULL CONSTRAINT [FK_OrderItems_Order] FOREIGN KEY REFERENCES [dbo].[Orders]([id]) ON DELETE CASCADE,
        [variant_id] BIGINT NOT NULL CONSTRAINT [FK_OrderItems_Variant] FOREIGN KEY REFERENCES [dbo].[Product_Variants]([id]),
        [product_name_snapshot] NVARCHAR(200) NOT NULL,
        [sku_snapshot] NVARCHAR(100) NOT NULL,
        [color_name_snapshot] NVARCHAR(50) NULL,
        [size_name_snapshot] NVARCHAR(20) NULL,
        [quantity] INT NOT NULL,
        [unit_price] DECIMAL(18,2) NOT NULL,
        [total_price] DECIMAL(18,2) NOT NULL,
        [cost_price_snapshot] DECIMAL(18,2) NULL,
        [is_reviewed] BIT NOT NULL DEFAULT 0
    );
END;

IF OBJECT_ID(N'[dbo].[Promotion_Usages]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Promotion_Usages] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [promotion_id] BIGINT NOT NULL CONSTRAINT [FK_PromoUsages_Promotion] FOREIGN KEY REFERENCES [dbo].[Promotions]([id]),
        [user_id] BIGINT NULL CONSTRAINT [FK_PromoUsages_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [order_id] BIGINT NULL CONSTRAINT [FK_PromoUsages_Order] FOREIGN KEY REFERENCES [dbo].[Orders]([id]),
        [discount_amount] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [status] NVARCHAR(30) NOT NULL DEFAULT 'RESERVED',
        [used_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
END;

IF OBJECT_ID(N'[dbo].[CoolCash_Transactions]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[CoolCash_Transactions] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [user_id] BIGINT NOT NULL CONSTRAINT [FK_CoolCash_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]) ON DELETE CASCADE,
        [order_id] BIGINT NULL CONSTRAINT [FK_CoolCash_Order] FOREIGN KEY REFERENCES [dbo].[Orders]([id]),
        [amount] DECIMAL(18,2) NOT NULL,
        [transaction_type] NVARCHAR(30) NOT NULL,
        [status] NVARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
        [idempotency_key] NVARCHAR(100) NULL,
        [description] NVARCHAR(255) NOT NULL,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_CoolCash_IdempotencyKey' AND object_id = OBJECT_ID(N'[dbo].[CoolCash_Transactions]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_CoolCash_IdempotencyKey] ON [dbo].[CoolCash_Transactions]([idempotency_key]) WHERE [idempotency_key] IS NOT NULL;
END;

IF OBJECT_ID(N'[dbo].[Inventory_Receipts]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Inventory_Receipts] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [receipt_code] NVARCHAR(50) NOT NULL UNIQUE,
        [supplier_name] NVARCHAR(200) NOT NULL,
        [created_by] BIGINT NULL CONSTRAINT [FK_InvReceipts_CreatedBy] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [approved_by] BIGINT NULL CONSTRAINT [FK_InvReceipts_ApprovedBy] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [approved_at] DATETIME2 NULL,
        [rejected_reason] NVARCHAR(500) NULL,
        [note] NVARCHAR(500) NULL,
        [status] NVARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
        [total_amount] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
END;

IF OBJECT_ID(N'[dbo].[Inventory_Receipt_Items]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Inventory_Receipt_Items] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [receipt_id] BIGINT NOT NULL CONSTRAINT [FK_ReceiptItems_Receipt] FOREIGN KEY REFERENCES [dbo].[Inventory_Receipts]([id]) ON DELETE CASCADE,
        [variant_id] BIGINT NOT NULL CONSTRAINT [FK_ReceiptItems_Variant] FOREIGN KEY REFERENCES [dbo].[Product_Variants]([id]),
        [quantity] INT NOT NULL,
        [import_price] DECIMAL(18,2) NOT NULL,
        [total_price] DECIMAL(18,2) NOT NULL
    );
END;

IF OBJECT_ID(N'[dbo].[Inventory_Movements]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Inventory_Movements] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [variant_id] BIGINT NOT NULL CONSTRAINT [FK_InvMovements_Variant] FOREIGN KEY REFERENCES [dbo].[Product_Variants]([id]),
        [movement_type] NVARCHAR(30) NOT NULL,
        [quantity] INT NOT NULL,
        [before_quantity] INT NULL,
        [after_quantity] INT NULL,
        [reference_type] NVARCHAR(50) NULL,
        [reference_id] BIGINT NULL,
        [created_by] BIGINT NULL CONSTRAINT [FK_InvMovements_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [note] NVARCHAR(500) NULL,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
END;

IF OBJECT_ID(N'[dbo].[Order_Returns]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Order_Returns] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [order_id] BIGINT NOT NULL CONSTRAINT [FK_Returns_Order] FOREIGN KEY REFERENCES [dbo].[Orders]([id]),
        [order_item_id] BIGINT NOT NULL CONSTRAINT [FK_Returns_OrderItem] FOREIGN KEY REFERENCES [dbo].[Order_Items]([id]),
        [user_id] BIGINT NOT NULL CONSTRAINT [FK_Returns_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [return_type] NVARCHAR(30) NOT NULL,
        [target_variant_id] BIGINT NULL CONSTRAINT [FK_Returns_TargetVariant] FOREIGN KEY REFERENCES [dbo].[Product_Variants]([id]),
        [quantity] INT NOT NULL,
        [reason] NVARCHAR(500) NOT NULL,
        [evidence_images] NVARCHAR(1000) NULL,
        [status] NVARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
        [refund_amount] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [refund_method] NVARCHAR(30) NULL,
        [refund_status] NVARCHAR(30) NULL,
        [refund_reference] NVARCHAR(100) NULL,
        [rejection_reason] NVARCHAR(500) NULL,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        [processed_at] DATETIME2 NULL
    );
END;

IF OBJECT_ID(N'[dbo].[Reviews]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Reviews] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [product_id] BIGINT NOT NULL CONSTRAINT [FK_Reviews_Product] FOREIGN KEY REFERENCES [dbo].[Products]([id]) ON DELETE CASCADE,
        [user_id] BIGINT NOT NULL CONSTRAINT [FK_Reviews_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [order_item_id] BIGINT NOT NULL CONSTRAINT [FK_Reviews_OrderItem] FOREIGN KEY REFERENCES [dbo].[Order_Items]([id]),
        [rating] INT NOT NULL,
        [comment] NVARCHAR(1000) NOT NULL,
        [purchased_color] NVARCHAR(50) NULL,
        [purchased_size] NVARCHAR(20) NULL,
        [fit_feedback] NVARCHAR(30) NULL,
        [customer_height_cm] INT NULL,
        [customer_weight_kg] INT NULL,
        [image_urls] NVARCHAR(1000) NULL,
        [status] NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
        [is_approved] BIT NOT NULL DEFAULT 0,
        [admin_reply] NVARCHAR(1000) NULL,
        [admin_replied_at] DATETIME2 NULL,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
END;

IF OBJECT_ID(N'[dbo].[User_Addresses]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[User_Addresses] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [user_id] BIGINT NOT NULL CONSTRAINT [FK_UserAddresses_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]) ON DELETE CASCADE,
        [recipient_name] NVARCHAR(100) NOT NULL,
        [recipient_phone] NVARCHAR(20) NOT NULL,
        [province_name] NVARCHAR(100) NOT NULL,
        [district_name] NVARCHAR(100) NOT NULL,
        [ward_name] NVARCHAR(100) NOT NULL,
        [street_address] NVARCHAR(255) NOT NULL,
        [is_default] BIT NOT NULL DEFAULT 0
    );
END;

IF OBJECT_ID(N'[dbo].[Wishlists]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Wishlists] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [user_id] BIGINT NOT NULL UNIQUE CONSTRAINT [FK_Wishlists_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]) ON DELETE CASCADE,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        [updated_at] DATETIME2 NULL
    );
END;

IF OBJECT_ID(N'[dbo].[Wishlist_Items]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Wishlist_Items] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [wishlist_id] BIGINT NOT NULL CONSTRAINT [FK_WishlistItems_Wishlist] FOREIGN KEY REFERENCES [dbo].[Wishlists]([id]) ON DELETE CASCADE,
        [product_id] BIGINT NOT NULL CONSTRAINT [FK_WishlistItems_Product] FOREIGN KEY REFERENCES [dbo].[Products]([id]),
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT [UQ_WishlistItems_Wishlist_Product] UNIQUE ([wishlist_id], [product_id])
    );
END;

IF OBJECT_ID(N'[dbo].[Password_Reset_Tokens]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Password_Reset_Tokens] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [user_id] BIGINT NOT NULL CONSTRAINT [FK_PwdReset_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]) ON DELETE CASCADE,
        [token] NVARCHAR(100) NOT NULL UNIQUE,
        [expiry_date] DATETIME2 NOT NULL,
        [is_used] BIT NOT NULL DEFAULT 0,
        [created_at] DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UQ_PromotionUsage_Order_Promo' AND object_id = OBJECT_ID(N'[dbo].[Promotion_Usages]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_PromotionUsage_Order_Promo] ON [dbo].[Promotion_Usages]([order_id], [promotion_id]) WHERE [order_id] IS NOT NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Users_CoolcashBalance')
BEGIN
    ALTER TABLE [dbo].[Users] ADD CONSTRAINT [CK_Users_CoolcashBalance] CHECK ([coolcash_balance] >= 0);
END;
