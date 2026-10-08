-- ===================================================================
-- COOLMATESHOP - D2C IDEMPOTENT CANONICAL DATABASE MIGRATION SCRIPT
-- SQL Server 2016+ Compatible - Safe for Production Execution
-- ===================================================================
USE [CoolMate_DB];
GO

PRINT 'Starting CoolMateShop D2C Canonical Database Migration...';
GO

-- 1. Orders Table Extensions (VNPAY, Cancellation, and Refund Workflow)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'vnpay_txn_ref')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [vnpay_txn_ref] NVARCHAR(100) NULL;
    PRINT 'Added vnpay_txn_ref to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_paid_at')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_paid_at] DATETIME2 NULL;
    PRINT 'Added payment_paid_at to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'cancelled_at')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [cancelled_at] DATETIME2 NULL;
    PRINT 'Added cancelled_at to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'cancelled_reason')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [cancelled_reason] NVARCHAR(500) NULL;
    PRINT 'Added cancelled_reason to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_response_code')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_response_code] NVARCHAR(50) NULL;
    PRINT 'Added payment_response_code to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_bank_code')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_bank_code] NVARCHAR(50) NULL;
    PRINT 'Added payment_bank_code to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_failure_reason')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_failure_reason] NVARCHAR(500) NULL;
    PRINT 'Added payment_failure_reason to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'refund_status')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [refund_status] NVARCHAR(30) NULL;
    PRINT 'Added refund_status to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'refund_reference')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [refund_reference] NVARCHAR(100) NULL;
    PRINT 'Added refund_reference to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'refund_amount')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [refund_amount] DECIMAL(18,2) NULL;
    PRINT 'Added refund_amount to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'refund_processed_at')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [refund_processed_at] DATETIME2 NULL;
    PRINT 'Added refund_processed_at to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'refund_note')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [refund_note] NVARCHAR(500) NULL;
    PRINT 'Added refund_note to Orders';
END
GO

-- 2. Order_Items Table Extensions (Historical COGS snapshot)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Items]') AND name = 'cost_price_snapshot')
BEGIN
    ALTER TABLE [dbo].[Order_Items] ADD [cost_price_snapshot] DECIMAL(18,2) NULL;
    PRINT 'Added cost_price_snapshot to Order_Items';
END
GO

-- 3. Inventory_Receipts Table Extensions & Reconciliation
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Receipts]') AND name = 'approved_by')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipts] ADD [approved_by] BIGINT NULL CONSTRAINT [FK_InventoryReceipts_ApprovedBy] FOREIGN KEY REFERENCES [dbo].[Users]([id]);
    PRINT 'Added approved_by to Inventory_Receipts';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Receipts]') AND name = 'approved_at')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipts] ADD [approved_at] DATETIME2 NULL;
    PRINT 'Added approved_at to Inventory_Receipts';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Receipts]') AND name = 'rejected_reason')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipts] ADD [rejected_reason] NVARCHAR(500) NULL;
    PRINT 'Added rejected_reason to Inventory_Receipts';
END
GO

-- 4. Order_Returns Table Extensions
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'processed_at')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [processed_at] DATETIME2 NULL;
    PRINT 'Added processed_at to Order_Returns';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'refund_method')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [refund_method] NVARCHAR(50) NULL;
    PRINT 'Added refund_method to Order_Returns';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'refund_reference')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [refund_reference] NVARCHAR(100) NULL;
    PRINT 'Added refund_reference to Order_Returns';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'refund_status')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [refund_status] NVARCHAR(50) NULL;
    PRINT 'Added refund_status to Order_Returns';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'rejection_reason')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [rejection_reason] NVARCHAR(500) NULL;
    PRINT 'Added rejection_reason to Order_Returns';
END
GO

-- 5. Reviews Table Extensions & Status Backfill
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Reviews]') AND name = 'status')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD [status] NVARCHAR(30) NOT NULL CONSTRAINT [DF_Reviews_Status] DEFAULT 'PENDING';
    PRINT 'Added status to Reviews';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Reviews]') AND name = 'admin_reply')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD [admin_reply] NVARCHAR(1000) NULL;
    PRINT 'Added admin_reply to Reviews';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Reviews]') AND name = 'admin_replied_at')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD [admin_replied_at] DATETIME2 NULL;
    PRINT 'Added admin_replied_at to Reviews';
END
GO

-- Backfill Review Status to prevent contradiction: is_approved=1 -> APPROVED, is_approved=0 -> PENDING
UPDATE [dbo].[Reviews]
SET [status] = 'APPROVED'
WHERE [is_approved] = 1 AND ([status] IS NULL OR [status] = 'PENDING');

UPDATE [dbo].[Reviews]
SET [status] = 'PENDING'
WHERE [is_approved] = 0 AND ([status] IS NULL OR [status] != 'REJECTED');
PRINT 'Reconciled and backfilled Reviews status from is_approved.';
GO

-- 6. Table: Inventory_Movements (Ledger Authority)
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Movements]') AND type in (N'U'))
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
    PRINT 'Created Table: Inventory_Movements';
END
GO

-- 7. Table: Promotion_Usages (Lifecycle: RESERVED / FINALIZED / RELEASED)
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Promotion_Usages]') AND type in (N'U'))
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
    PRINT 'Created Table: Promotion_Usages';
END
GO

-- 8. Table: Wishlists
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Wishlists]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Wishlists] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [user_id] BIGINT NOT NULL UNIQUE CONSTRAINT [FK_Wishlists_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [created_at] DATETIME2 NOT NULL CONSTRAINT [DF_Wishlists_CreatedAt] DEFAULT SYSUTCDATETIME(),
        [updated_at] DATETIME2 NULL
    );
    PRINT 'Created Table: Wishlists';
END
GO

-- 9. Table: Wishlist_Items
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Wishlist_Items]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Wishlist_Items] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [wishlist_id] BIGINT NOT NULL CONSTRAINT [FK_WishlistItems_Wishlist] FOREIGN KEY REFERENCES [dbo].[Wishlists]([id]) ON DELETE CASCADE,
        [product_id] BIGINT NOT NULL CONSTRAINT [FK_WishlistItems_Product] FOREIGN KEY REFERENCES [dbo].[Products]([id]),
        [created_at] DATETIME2 NOT NULL CONSTRAINT [DF_WishlistItems_CreatedAt] DEFAULT SYSUTCDATETIME(),
        CONSTRAINT [UQ_WishlistItems_Wishlist_Product] UNIQUE ([wishlist_id], [product_id])
    );
    PRINT 'Created Table: Wishlist_Items';
END
GO

-- 10. Table: Password_Reset_Tokens
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Password_Reset_Tokens]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Password_Reset_Tokens] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [token] NVARCHAR(100) NOT NULL UNIQUE,
        [user_id] BIGINT NOT NULL CONSTRAINT [FK_PasswordReset_User] FOREIGN KEY REFERENCES [dbo].[Users]([id]),
        [expiry_date] DATETIME2 NOT NULL,
        [is_used] BIT NOT NULL CONSTRAINT [DF_PasswordReset_IsUsed] DEFAULT 0,
        [created_at] DATETIME2 NOT NULL CONSTRAINT [DF_PasswordReset_CreatedAt] DEFAULT SYSUTCDATETIME()
    );
    PRINT 'Created Table: Password_Reset_Tokens';
END
GO

-- 11. CoolCash_Transactions: Idempotency Key column
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[CoolCash_Transactions]') AND name = 'idempotency_key')
BEGIN
    ALTER TABLE [dbo].[CoolCash_Transactions] ADD [idempotency_key] NVARCHAR(100) NULL;
    PRINT 'Added idempotency_key to CoolCash_Transactions';
END
GO

-- 12. Reconciliation: Existing Legacy COMPLETED Inventory Receipts to APPROVED
IF EXISTS (SELECT 1 FROM [dbo].[Inventory_Receipts] WHERE [status] = 'COMPLETED')
BEGIN
    UPDATE [dbo].[Inventory_Receipts]
    SET [status] = 'APPROVED',
        [approved_at] = COALESCE([approved_at], [created_at]),
        [approved_by] = COALESCE([approved_by], [created_by])
    WHERE [status] = 'COMPLETED';

    -- Audit reconciliation into Inventory_Movements for legacy receipts if missing
    INSERT INTO [dbo].[Inventory_Movements] ([variant_id], [movement_type], [quantity], [before_quantity], [after_quantity], [reference_type], [reference_id], [created_by], [note], [created_at])
    SELECT iri.[variant_id], 'IMPORT', iri.[quantity], pv.[stock_quantity] - iri.[quantity], pv.[stock_quantity], 'RECEIPT', ir.[id], ir.[created_by],
           CONCAT(N'Reconciled historical import from receipt #', ir.[receipt_code]), ir.[created_at]
    FROM [dbo].[Inventory_Receipt_Items] iri
    INNER JOIN [dbo].[Inventory_Receipts] ir ON iri.[receipt_id] = ir.[id]
    INNER JOIN [dbo].[Product_Variants] pv ON iri.[variant_id] = pv.[id]
    WHERE NOT EXISTS (
        SELECT 1 FROM [dbo].[Inventory_Movements] im
        WHERE im.[reference_type] = 'RECEIPT' AND im.[reference_id] = ir.[id] AND im.[variant_id] = iri.[variant_id]
    );

    PRINT 'Reconciled legacy COMPLETED inventory receipts to APPROVED with ledger movements.';
END
GO

-- 13. Filtered Unique Indexes (Concurrency & Idempotency)
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_Carts_UserId' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    DROP INDEX [UQ_Carts_UserId] ON [dbo].[Carts];
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_Carts_UserId_Filtered' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_Carts_UserId_Filtered]
    ON [dbo].[Carts]([user_id])
    WHERE [user_id] IS NOT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_Carts_SessionId_Filtered' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_Carts_SessionId_Filtered]
    ON [dbo].[Carts]([session_id])
    WHERE [session_id] IS NOT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_CoolCash_IdempotencyKey' AND object_id = OBJECT_ID(N'[dbo].[CoolCash_Transactions]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_CoolCash_IdempotencyKey]
    ON [dbo].[CoolCash_Transactions]([idempotency_key])
    WHERE [idempotency_key] IS NOT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UQ_PromotionUsage_Order_Promo' AND object_id = OBJECT_ID(N'[dbo].[Promotion_Usages]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_PromotionUsage_Order_Promo]
    ON [dbo].[Promotion_Usages]([order_id], [promotion_id])
    WHERE [order_id] IS NOT NULL;
END
GO

-- 14. Check Constraints for Data Integrity (Money, Quantities, Rating, Stock)
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Users_CoolcashBalance')
BEGIN
    ALTER TABLE [dbo].[Users] ADD CONSTRAINT [CK_Users_CoolcashBalance] CHECK ([coolcash_balance] >= 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Users_TotalSpent')
BEGIN
    ALTER TABLE [dbo].[Users] ADD CONSTRAINT [CK_Users_TotalSpent] CHECK ([total_spent] >= 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_ProductVariants_Stock')
BEGIN
    ALTER TABLE [dbo].[Product_Variants] ADD CONSTRAINT [CK_ProductVariants_Stock] CHECK ([stock_quantity] >= 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_ProductVariants_SalePrice')
BEGIN
    ALTER TABLE [dbo].[Product_Variants] ADD CONSTRAINT [CK_ProductVariants_SalePrice] CHECK ([sale_price] >= 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_ProductVariants_ImportPrice')
BEGIN
    ALTER TABLE [dbo].[Product_Variants] ADD CONSTRAINT [CK_ProductVariants_ImportPrice] CHECK ([import_price] >= 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Products_BasePrice')
BEGIN
    ALTER TABLE [dbo].[Products] ADD CONSTRAINT [CK_Products_BasePrice] CHECK ([base_price] >= 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Products_RatingAvg')
BEGIN
    ALTER TABLE [dbo].[Products] ADD CONSTRAINT [CK_Products_RatingAvg] CHECK ([rating_avg] >= 0.0 AND [rating_avg] <= 5.0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Reviews_Rating')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD CONSTRAINT [CK_Reviews_Rating] CHECK ([rating] >= 1 AND [rating] <= 5);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_CartItems_Quantity')
BEGIN
    ALTER TABLE [dbo].[Cart_Items] ADD CONSTRAINT [CK_CartItems_Quantity] CHECK ([quantity] > 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_OrderItems_Quantity')
BEGIN
    ALTER TABLE [dbo].[Order_Items] ADD CONSTRAINT [CK_OrderItems_Quantity] CHECK ([quantity] > 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_OrderReturns_Quantity')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD CONSTRAINT [CK_OrderReturns_Quantity] CHECK ([quantity] > 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_OrderReturns_RefundAmount')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD CONSTRAINT [CK_OrderReturns_RefundAmount] CHECK ([refund_amount] >= 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_InventoryReceiptItems_Quantity')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipt_Items] ADD CONSTRAINT [CK_InventoryReceiptItems_Quantity] CHECK ([quantity] > 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_InventoryReceiptItems_ImportPrice')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipt_Items] ADD CONSTRAINT [CK_InventoryReceiptItems_ImportPrice] CHECK ([import_price] >= 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_InventoryMovements_Quantity')
BEGIN
    ALTER TABLE [dbo].[Inventory_Movements] ADD CONSTRAINT [CK_InventoryMovements_Quantity] CHECK ([quantity] > 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Orders_RefundAmount')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD CONSTRAINT [CK_Orders_RefundAmount] CHECK ([refund_amount] IS NULL OR [refund_amount] >= 0);
    PRINT 'Added constraint CK_Orders_RefundAmount to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Orders_Amounts')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD CONSTRAINT [CK_Orders_Amounts] CHECK (
        [subtotal_amount] >= 0 AND [final_amount] >= 0 AND [shipping_fee] >= 0 AND [coolcash_used] >= 0
    );
END
GO

PRINT 'CoolMateShop D2C Canonical Database Migration completed successfully 100%!';
GO
