-- ===================================================================
-- COOLMATESHOP - D2C IDEMPOTENT DATABASE MIGRATION SCRIPT
-- ===================================================================
USE [CoolMate_DB];
GO

-- 1. Orders Table Extensions
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'vnpay_txn_ref')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [vnpay_txn_ref] NVARCHAR(100) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_paid_at')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_paid_at] DATETIME2 NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'cancelled_at')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [cancelled_at] DATETIME2 NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'cancelled_reason')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [cancelled_reason] NVARCHAR(500) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_response_code')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_response_code] NVARCHAR(50) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_bank_code')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_bank_code] NVARCHAR(50) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_failure_reason')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_failure_reason] NVARCHAR(500) NULL;
END
GO

-- 2. Order_Items Table Extensions (Historical COGS snapshot)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Items]') AND name = 'cost_price_snapshot')
BEGIN
    ALTER TABLE [dbo].[Order_Items] ADD [cost_price_snapshot] DECIMAL(18,2) NULL;
END
GO

-- 3. Inventory_Receipts Table Extensions
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Receipts]') AND name = 'approved_by')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipts] ADD [approved_by] BIGINT NULL CONSTRAINT [FK_InventoryReceipts_ApprovedBy] FOREIGN KEY REFERENCES [dbo].[Users]([id]);
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Receipts]') AND name = 'approved_at')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipts] ADD [approved_at] DATETIME2 NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Receipts]') AND name = 'rejected_reason')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipts] ADD [rejected_reason] NVARCHAR(500) NULL;
END
GO

-- 4. Order_Returns Table Extensions
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'processed_at')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [processed_at] DATETIME2 NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'refund_method')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [refund_method] NVARCHAR(50) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'refund_reference')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [refund_reference] NVARCHAR(100) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'refund_status')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [refund_status] NVARCHAR(50) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'rejection_reason')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [rejection_reason] NVARCHAR(500) NULL;
END
GO

-- 5. Reviews Table Extensions
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Reviews]') AND name = 'status')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD [status] NVARCHAR(30) NOT NULL CONSTRAINT [DF_Reviews_Status] DEFAULT 'PENDING';
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Reviews]') AND name = 'admin_reply')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD [admin_reply] NVARCHAR(1000) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Reviews]') AND name = 'admin_replied_at')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD [admin_replied_at] DATETIME2 NULL;
END
GO

-- 6. Table: Inventory_Movements
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Movements]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Inventory_Movements] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [variant_id] BIGINT NOT NULL CONSTRAINT [FK_InvMovements_Variant] FOREIGN KEY REFERENCES [dbo].[Product_Variants]([id]),
        [movement_type] NVARCHAR(30) NOT NULL,
        [quantity] INT NOT NULL,
        [before_quantity] INT NULL,
        [after_quantity] INT NULL,
        [reference_type] NVARCHAR(50) NULL,
        [reference_id] BIGINT NULL,
        [created_by] BIGINT NULL CONSTRAINT [FK_InvMovements_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [note] NVARCHAR(500) NULL,
        [created_at] DATETIME2 NOT NULL CONSTRAINT [DF_InvMovements_CreatedAt] DEFAULT SYSUTCDATETIME()
    );
END
GO

-- 7. Table: Promotion_Usages (with status RESERVED/FINALIZED/RELEASED)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Promotion_Usages]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Promotion_Usages] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [promotion_id] BIGINT NOT NULL CONSTRAINT [FK_PromoUsages_Promo] FOREIGN KEY REFERENCES [dbo].[Promotions]([id]),
        [user_id] BIGINT NULL CONSTRAINT [FK_PromoUsages_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [order_id] BIGINT NULL CONSTRAINT [FK_PromoUsages_Order] FOREIGN KEY REFERENCES [dbo].[Orders]([id]),
        [discount_amount] DECIMAL(18,2) NOT NULL DEFAULT 0,
        [status] NVARCHAR(30) NOT NULL DEFAULT 'RESERVED',
        [used_at] DATETIME2 NOT NULL CONSTRAINT [DF_PromoUsages_UsedAt] DEFAULT SYSUTCDATETIME()
    );
END
GO

-- 8. Table: Wishlists
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Wishlists]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Wishlists] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [user_id] BIGINT NOT NULL UNIQUE CONSTRAINT [FK_Wishlists_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [created_at] DATETIME2 NOT NULL CONSTRAINT [DF_Wishlists_CreatedAt] DEFAULT SYSUTCDATETIME(),
        [updated_at] DATETIME2 NULL
    );
END
GO

-- 9. Table: Wishlist_Items
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Wishlist_Items]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Wishlist_Items] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [wishlist_id] BIGINT NOT NULL CONSTRAINT [FK_WishlistItems_Wishlist] FOREIGN KEY REFERENCES [dbo].[Wishlists]([id]) ON DELETE CASCADE,
        [product_id] BIGINT NOT NULL CONSTRAINT [FK_WishlistItems_Product] FOREIGN KEY REFERENCES [dbo].[Products]([id]),
        [created_at] DATETIME2 NOT NULL CONSTRAINT [DF_WishlistItems_CreatedAt] DEFAULT SYSUTCDATETIME(),
        CONSTRAINT [UQ_WishlistItems_Wishlist_Product] UNIQUE ([wishlist_id], [product_id])
    );
END
GO

-- 10. Table: Password_Reset_Tokens
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Password_Reset_Tokens]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Password_Reset_Tokens] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [token] NVARCHAR(100) NOT NULL UNIQUE,
        [user_id] BIGINT NOT NULL CONSTRAINT [FK_PasswordReset_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [expiry_date] DATETIME2 NOT NULL,
        [is_used] BIT NOT NULL CONSTRAINT [DF_PasswordReset_IsUsed] DEFAULT 0,
        [created_at] DATETIME2 NOT NULL CONSTRAINT [DF_PasswordReset_CreatedAt] DEFAULT SYSUTCDATETIME()
    );
END
GO

-- 11. Fix Single-Guest Cart Constraint Bug with Filtered Indexes
IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_Carts_UserId' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    DROP INDEX [UQ_Carts_UserId] ON [dbo].[Carts];
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_Carts_UserId_Filtered' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_Carts_UserId_Filtered]
    ON [dbo].[Carts]([user_id])
    WHERE [user_id] IS NOT NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_Carts_SessionId_Filtered' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_Carts_SessionId_Filtered]
    ON [dbo].[Carts]([session_id])
    WHERE [session_id] IS NOT NULL;
END
GO

PRINT 'D2C Database Migration completed successfully!';

-- Add idempotency_key to CoolCash_Transactions
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[CoolCash_Transactions]') AND name = 'idempotency_key')
BEGIN
    ALTER TABLE [dbo].[CoolCash_Transactions] ADD [idempotency_key] NVARCHAR(100) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_CoolCash_IdempotencyKey' AND object_id = OBJECT_ID(N'[dbo].[CoolCash_Transactions]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_CoolCash_IdempotencyKey]
    ON [dbo].[CoolCash_Transactions]([idempotency_key])
    WHERE [idempotency_key] IS NOT NULL;
END
GO
