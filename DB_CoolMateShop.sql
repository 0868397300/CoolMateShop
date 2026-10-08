USE [master];
GO

-- Tự động ngắt kết nối và xóa database cũ nếu đã tồn tại
IF DB_ID(N'CoolMate_DB') IS NOT NULL
BEGIN
    ALTER DATABASE [CoolMate_DB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [CoolMate_DB];
END
GO

-- Tạo mới database CoolMate_DB theo đường dẫn mặc định của hệ thống
CREATE DATABASE [CoolMate_DB];
GO

USE [CoolMate_DB];
GO
ALTER DATABASE [CoolMate_DB] SET COMPATIBILITY_LEVEL = 160
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [CoolMate_DB].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [CoolMate_DB] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [CoolMate_DB] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [CoolMate_DB] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [CoolMate_DB] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [CoolMate_DB] SET ARITHABORT OFF 
GO
ALTER DATABASE [CoolMate_DB] SET AUTO_CLOSE OFF 
GO
ALTER DATABASE [CoolMate_DB] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [CoolMate_DB] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [CoolMate_DB] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [CoolMate_DB] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [CoolMate_DB] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [CoolMate_DB] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [CoolMate_DB] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [CoolMate_DB] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [CoolMate_DB] SET  ENABLE_BROKER 
GO
ALTER DATABASE [CoolMate_DB] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [CoolMate_DB] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [CoolMate_DB] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [CoolMate_DB] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [CoolMate_DB] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [CoolMate_DB] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [CoolMate_DB] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [CoolMate_DB] SET RECOVERY FULL 
GO
ALTER DATABASE [CoolMate_DB] SET  MULTI_USER 
GO
ALTER DATABASE [CoolMate_DB] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [CoolMate_DB] SET DB_CHAINING OFF 
GO
ALTER DATABASE [CoolMate_DB] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [CoolMate_DB] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [CoolMate_DB] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [CoolMate_DB] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
EXEC sys.sp_db_vardecimal_storage_format N'CoolMate_DB', N'ON'
GO
ALTER DATABASE [CoolMate_DB] SET QUERY_STORE = ON
GO
ALTER DATABASE [CoolMate_DB] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [CoolMate_DB]
GO
/****** Object:  Table [dbo].[Cart_Items] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Cart_Items](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[cart_id] [bigint] NOT NULL,
	[variant_id] [bigint] NOT NULL,
	[quantity] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Carts] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Carts](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NULL,
	[session_id] [varchar](100) NULL,
	[updated_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Categories] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Categories](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[parent_id] [int] NULL,
	[name] [nvarchar](150) NOT NULL,
	[slug] [varchar](150) NOT NULL,
	[description] [nvarchar](500) NULL,
	[image_url] [varchar](500) NULL,
	[display_order] [int] NOT NULL,
	[is_active] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Collections] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Collections](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](150) NOT NULL,
	[slug] [varchar](150) NOT NULL,
	[banner_url] [varchar](500) NULL,
	[description] [nvarchar](1000) NULL,
	[is_active] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Colors] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Colors](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](50) NOT NULL,
	[hex_code] [varchar](10) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Combo_Rules] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Combo_Rules](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](200) NOT NULL,
	[category_id] [int] NOT NULL,
	[min_quantity] [int] NOT NULL,
	[discount_percentage] [decimal](5, 2) NOT NULL,
	[is_active] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[CoolCash_Transactions] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CoolCash_Transactions](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[order_id] [bigint] NULL,
	[amount] [decimal](18, 2) NOT NULL,
	[transaction_type] [varchar](30) NOT NULL,
	[status] [varchar](20) NOT NULL,
	[idempotency_key] [nvarchar](100) NULL,
	[description] [nvarchar](255) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Inventory_Receipts] - PHIẾU NHẬP KHO ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Inventory_Receipts](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[receipt_code] [varchar](50) NOT NULL,
	[supplier_name] [nvarchar](255) NOT NULL,
	[created_by] [bigint] NOT NULL,
	[total_amount] [decimal](18, 2) NOT NULL,
	[note] [nvarchar](500) NULL,
	[status] [varchar](20) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Inventory_Receipt_Items] - CHI TIẾT PHIẾU NHẬP ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Inventory_Receipt_Items](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[receipt_id] [bigint] NOT NULL,
	[variant_id] [bigint] NOT NULL,
	[quantity] [int] NOT NULL,
	[import_price] [decimal](18, 2) NOT NULL,
	[total_price] [decimal](18, 2) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Order_Items] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Order_Items](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[order_id] [bigint] NOT NULL,
	[variant_id] [bigint] NOT NULL,
	[product_name_snapshot] [nvarchar](250) NOT NULL,
	[sku_snapshot] [varchar](100) NOT NULL,
	[color_name_snapshot] [nvarchar](50) NOT NULL,
	[size_name_snapshot] [varchar](20) NOT NULL,
	[quantity] [int] NOT NULL,
	[unit_price] [decimal](18, 2) NOT NULL,
	[total_price] [decimal](18, 2) NOT NULL,
	[is_reviewed] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Order_Returns] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Order_Returns](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[order_id] [bigint] NOT NULL,
	[order_item_id] [bigint] NOT NULL,
	[user_id] [bigint] NOT NULL,
	[return_type] [varchar](30) NOT NULL,
	[target_variant_id] [bigint] NULL,
	[quantity] [int] NOT NULL,
	[reason] [nvarchar](500) NOT NULL,
	[evidence_images] [nvarchar](1000) NULL,
	[status] [varchar](30) NOT NULL,
	[refund_amount] [decimal](18, 2) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Orders] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Orders](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[order_code] [varchar](50) NOT NULL,
	[user_id] [bigint] NULL,
	[promotion_id] [bigint] NULL,
	[recipient_name] [nvarchar](100) NOT NULL,
	[recipient_phone] [varchar](20) NOT NULL,
	[recipient_email] [varchar](150) NULL,
	[shipping_address] [nvarchar](500) NOT NULL,
	[note] [nvarchar](500) NULL,
	[subtotal_amount] [decimal](18, 2) NOT NULL,
	[combo_discount_amount] [decimal](18, 2) NOT NULL,
	[voucher_discount_amount] [decimal](18, 2) NOT NULL,
	[coolcash_used] [decimal](18, 2) NOT NULL,
	[shipping_fee] [decimal](18, 2) NOT NULL,
	[final_amount] [decimal](18, 2) NOT NULL,
	[coolcash_earned] [decimal](18, 2) NOT NULL,
	[payment_method] [varchar](30) NOT NULL,
	[payment_status] [varchar](30) NOT NULL,
	[vnpay_transaction_no] [varchar](100) NULL,
	[order_status] [varchar](30) NOT NULL,
	[delivered_at] [datetime2](7) NULL,
	[cancelled_at] [datetime2](7) NULL,
	[cancelled_reason] [nvarchar](500) NULL,
	[payment_response_code] [varchar](50) NULL,
	[payment_bank_code] [varchar](50) NULL,
	[payment_failure_reason] [nvarchar](500) NULL,
	[vnpay_txn_ref] [varchar](100) NULL,
	[payment_paid_at] [datetime2](7) NULL,
	[refund_status] [nvarchar](30) NULL,
	[refund_reference] [nvarchar](100) NULL,
	[refund_amount] [decimal](18, 2) NULL,
	[refund_processed_at] [datetime2](7) NULL,
	[refund_note] [nvarchar](500) NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Product_Images] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Product_Images](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[product_id] [bigint] NOT NULL,
	[color_id] [int] NULL,
	[image_url] [varchar](500) NOT NULL,
	[is_thumbnail] [bit] NOT NULL,
	[display_order] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Product_Variants] (CÓ GIÁ VỐN IMPORT_PRICE) ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Product_Variants](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[product_id] [bigint] NOT NULL,
	[color_id] [int] NOT NULL,
	[size_id] [int] NOT NULL,
	[sku] [varchar](100) NOT NULL,
	[original_price] [decimal](18, 2) NOT NULL,
	[sale_price] [decimal](18, 2) NOT NULL,
	[import_price] [decimal](18, 2) NOT NULL,
	[stock_quantity] [int] NOT NULL,
	[weight_gram] [int] NOT NULL,
	[is_active] [bit] NOT NULL,
	[version] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Products] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Products](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[category_id] [int] NOT NULL,
	[collection_id] [int] NULL,
	[name] [nvarchar](250) NOT NULL,
	[slug] [varchar](250) NOT NULL,
	[short_description] [nvarchar](500) NULL,
	[description] [nvarchar](max) NULL,
	[material] [nvarchar](200) NULL,
	[fit_type] [nvarchar](50) NULL,
	[features] [nvarchar](500) NULL,
	[base_price] [decimal](18, 2) NOT NULL,
	[rating_avg] [decimal](3, 2) NOT NULL,
	[review_count] [int] NOT NULL,
	[sold_count] [int] NOT NULL,
	[status] [varchar](30) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Promotions] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Promotions](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[code] [varchar](50) NOT NULL,
	[name] [nvarchar](200) NOT NULL,
	[discount_type] [varchar](20) NOT NULL,
	[discount_value] [decimal](18, 2) NOT NULL,
	[max_discount_amount] [decimal](18, 2) NULL,
	[min_order_value] [decimal](18, 2) NOT NULL,
	[usage_limit] [int] NOT NULL,
	[used_count] [int] NOT NULL,
	[start_date] [datetime2](7) NOT NULL,
	[end_date] [datetime2](7) NOT NULL,
	[is_active] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Reviews] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Reviews](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[product_id] [bigint] NOT NULL,
	[user_id] [bigint] NOT NULL,
	[order_item_id] [bigint] NOT NULL,
	[rating] [int] NOT NULL,
	[comment] [nvarchar](1000) NOT NULL,
	[customer_height_cm] [int] NULL,
	[customer_weight_kg] [int] NULL,
	[purchased_color] [nvarchar](50) NOT NULL,
	[purchased_size] [varchar](20) NOT NULL,
	[fit_feedback] [varchar](30) NOT NULL,
	[image_urls] [nvarchar](1000) NULL,
	[admin_reply] [nvarchar](1000) NULL,
	[is_approved] [bit] NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Roles] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Roles](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[role_name] [varchar](50) NOT NULL,
	[description] [nvarchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Sizes] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Sizes](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [varchar](20) NOT NULL,
	[min_height_cm] [int] NOT NULL,
	[max_height_cm] [int] NOT NULL,
	[min_weight_kg] [int] NOT NULL,
	[max_weight_kg] [int] NOT NULL,
	[display_order] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[User_Addresses] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[User_Addresses](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[recipient_name] [nvarchar](100) NOT NULL,
	[recipient_phone] [varchar](20) NOT NULL,
	[province_name] [nvarchar](100) NOT NULL,
	[district_name] [nvarchar](100) NOT NULL,
	[ward_name] [nvarchar](100) NOT NULL,
	[street_address] [nvarchar](255) NOT NULL,
	[is_default] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[User_Roles] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[User_Roles](
	[user_id] [bigint] NOT NULL,
	[role_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[user_id] ASC,
	[role_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Users] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Users](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[full_name] [nvarchar](100) NOT NULL,
	[email] [varchar](150) NOT NULL,
	[phone] [varchar](20) NULL,
	[password_hash] [varchar](255) NOT NULL,
	[gender] [nvarchar](10) NULL,
	[birth_date] [date] NULL,
	[height_cm] [int] NULL,
	[weight_kg] [int] NULL,
	[membership_tier] [varchar](30) NOT NULL,
	[coolcash_balance] [decimal](18, 2) NOT NULL,
	[total_spent] [decimal](18, 2) NOT NULL,
	[is_active] [bit] NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- =============================================
-- INSERT DỮ LIỆU MẪU
-- =============================================

SET IDENTITY_INSERT [dbo].[Roles] ON 
INSERT [dbo].[Roles] ([id], [role_name], [description]) VALUES (1, N'ROLE_ADMIN', N'Quản trị viên toàn quyền hệ thống Coolmate')
INSERT [dbo].[Roles] ([id], [role_name], [description]) VALUES (2, N'ROLE_STAFF', N'Nhân viên vận hành đơn hàng, kho SKU và chăm sóc khách hàng')
INSERT [dbo].[Roles] ([id], [role_name], [description]) VALUES (3, N'ROLE_CUSTOMER', N'Khách hàng thành viên hội viên CoolClub')
SET IDENTITY_INSERT [dbo].[Roles] OFF
GO

SET IDENTITY_INSERT [dbo].[Users] ON 
INSERT [dbo].[Users] ([id], [full_name], [email], [phone], [password_hash], [gender], [birth_date], [height_cm], [weight_kg], [membership_tier], [coolcash_balance], [total_spent], [is_active], [created_at], [updated_at]) VALUES (1, N'Quản Trị Viên Coolmate', N'admin@coolmate.me', N'0901000001', N'$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', N'Nam', CAST(N'1995-05-15' AS Date), 175, 70, N'PLATINUM', CAST(500000.00 AS Decimal(18, 2)), CAST(10000000.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2), CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2))
INSERT [dbo].[Users] ([id], [full_name], [email], [phone], [password_hash], [gender], [birth_date], [height_cm], [weight_kg], [membership_tier], [coolcash_balance], [total_spent], [is_active], [created_at], [updated_at]) VALUES (2, N'Nhân Viên Kho Quận 12', N'staff@coolmate.me', N'0901000002', N'$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', N'Nam', CAST(N'1998-08-20' AS Date), 170, 65, N'SILVER', CAST(50000.00 AS Decimal(18, 2)), CAST(1200000.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2), CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2))
INSERT [dbo].[Users] ([id], [full_name], [email], [phone], [password_hash], [gender], [birth_date], [height_cm], [weight_kg], [membership_tier], [coolcash_balance], [total_spent], [is_active], [created_at], [updated_at]) VALUES (3, N'Nguyễn Minh Tuấn', N'tuan.nguyen@gmail.com', N'0988111222', N'$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', N'Nam', CAST(N'1999-11-10' AS Date), 173, 68, N'GOLD', CAST(85000.00 AS Decimal(18, 2)), CAST(3450000.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2), CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2))
INSERT [dbo].[Users] ([id], [full_name], [email], [phone], [password_hash], [gender], [birth_date], [height_cm], [weight_kg], [membership_tier], [coolcash_balance], [total_spent], [is_active], [created_at], [updated_at]) VALUES (4, N'Trần Hoàng Nam', N'hoangnam.tran@gmail.com', N'0977333444', N'$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', N'Nam', CAST(N'2001-03-25' AS Date), 178, 76, N'PLATINUM', CAST(210000.00 AS Decimal(18, 2)), CAST(6890000.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2), CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2))
INSERT [dbo].[Users] ([id], [full_name], [email], [phone], [password_hash], [gender], [birth_date], [height_cm], [weight_kg], [membership_tier], [coolcash_balance], [total_spent], [is_active], [created_at], [updated_at]) VALUES (5, N'Lê Quốc Bảo', N'quocbao.le@gmail.com', N'0912555666', N'$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', N'Nam', CAST(N'2002-07-19' AS Date), 166, 58, N'NEW', CAST(15000.00 AS Decimal(18, 2)), CAST(450000.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2), CAST(N'2026-10-03T13:47:28.2366667' AS DateTime2))
SET IDENTITY_INSERT [dbo].[Users] OFF
GO

INSERT [dbo].[User_Roles] ([user_id], [role_id]) VALUES (1, 1)
INSERT [dbo].[User_Roles] ([user_id], [role_id]) VALUES (1, 2)
INSERT [dbo].[User_Roles] ([user_id], [role_id]) VALUES (2, 2)
INSERT [dbo].[User_Roles] ([user_id], [role_id]) VALUES (3, 3)
INSERT [dbo].[User_Roles] ([user_id], [role_id]) VALUES (4, 3)
INSERT [dbo].[User_Roles] ([user_id], [role_id]) VALUES (5, 3)
GO

SET IDENTITY_INSERT [dbo].[User_Addresses] ON 
INSERT [dbo].[User_Addresses] ([id], [user_id], [recipient_name], [recipient_phone], [province_name], [district_name], [ward_name], [street_address], [is_default]) VALUES (1, 3, N'Nguyễn Minh Tuấn', N'0988111222', N'TP. Hồ Chí Minh', N'Quận Gò Vấp', N'Phường 10', N'190 Quang Trung, Khu phố 2', 1)
INSERT [dbo].[User_Addresses] ([id], [user_id], [recipient_name], [recipient_phone], [province_name], [district_name], [ward_name], [street_address], [is_default]) VALUES (2, 3, N'Nguyễn Minh Tuấn (Công ty)', N'0988111222', N'TP. Hồ Chí Minh', N'Quận 1', N'Phường Bến Nghé', N'Tòa nhà Bitexco, Số 2 Hải Triều', 0)
INSERT [dbo].[User_Addresses] ([id], [user_id], [recipient_name], [recipient_phone], [province_name], [district_name], [ward_name], [street_address], [is_default]) VALUES (3, 4, N'Trần Hoàng Nam', N'0977333444', N'TP. Hà Nội', N'Quận Cầu Giấy', N'Phường Dịch Vọng Hậu', N'Số 85 Xuân Thủy', 1)
INSERT [dbo].[User_Addresses] ([id], [user_id], [recipient_name], [recipient_phone], [province_name], [district_name], [ward_name], [street_address], [is_default]) VALUES (4, 5, N'Lê Quốc Bảo', N'0912555666', N'TP. Đà Nẵng', N'Quận Hải Châu', N'Phường Thạch Thang', N'Số 42 Nguyễn Chí Thanh', 1)
SET IDENTITY_INSERT [dbo].[User_Addresses] OFF
GO

SET IDENTITY_INSERT [dbo].[Categories] ON 
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (1, NULL, N'Áo Nam', N'ao-nam', N'Tất cả các dòng áo nam thiết kế tối giản, chất liệu công nghệ cao', N'https://media3.coolmate.me/cdn-cgi/image/width=672,height=990,quality=85/uploads/March2024/ao-nam-thumb.jpg', 1, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (2, NULL, N'Quần Nam', N'quan-nam', N'Quần short, quần dài, quần jeans co giãn thoải mái suốt ngày dài', N'https://media3.coolmate.me/cdn-cgi/image/width=672,height=990,quality=85/uploads/March2024/quan-nam-thumb.jpg', 2, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (3, NULL, N'Đồ Lót Nam', N'do-lot-nam', N'Quần lót nam kháng khuẩn, thoáng khí Bamboo, Excool, Modal', N'https://media3.coolmate.me/cdn-cgi/image/width=672,height=990,quality=85/uploads/March2024/do-lot-thumb.jpg', 3, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (4, NULL, N'Phụ Kiện Nam', N'phu-kien-nam', N'Tất/vớ thể thao, mũ lưỡi trai, túi tote, thắt lưng nam', N'https://media3.coolmate.me/cdn-cgi/image/width=672,height=990,quality=85/uploads/March2024/phu-kien-thumb.jpg', 4, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (5, 1, N'Áo Thun Nam', N'ao-thun-nam', N'Áo thun Cotton Compact chống nhăn, thoáng mát', NULL, 1, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (6, 1, N'Áo Polo Nam', N'ao-polo-nam', N'Áo Polo Excool, Café khử mùi, lịch sự năng động', NULL, 2, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (7, 1, N'Áo Tanktop & Singlet', N'ao-singlet-nam', N'Áo ba lỗ chạy bộ, tập gym siêu nhẹ nhanh khô', NULL, 3, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (8, 1, N'Áo Khoác Nam', N'ao-khoac-nam', N'Áo khoác gió trượt nước, chống tia UV SPF50+', NULL, 4, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (9, 2, N'Quần Short Nam', N'quan-short-nam', N'Quần short thể thao, chạy bộ 5 inch, 7 inch, quần short mặc nhà', NULL, 1, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (10, 2, N'Quần Dài & Pants', N'quan-dai-nam', N'Quần dài UT Pants, Jogger, Kaki co giãn chống nhăn', NULL, 2, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (11, 3, N'Quần Lót Trunk (Boxer)', N'quan-lot-trunk', N'Quần lót dáng đùi ôm vừa vặn, không cuộn ống', NULL, 1, 1)
INSERT [dbo].[Categories] ([id], [parent_id], [name], [slug], [description], [image_url], [display_order], [is_active]) VALUES (12, 4, N'Tất / Vớ Nam', N'tat-vo-nam', N'Tất cổ ngắn, tất chạy bộ chống trượt kháng khuẩn', NULL, 1, 1)
SET IDENTITY_INSERT [dbo].[Categories] OFF
GO

SET IDENTITY_INSERT [dbo].[Collections] ON 
INSERT [dbo].[Collections] ([id], [name], [slug], [banner_url], [description], [is_active]) VALUES (1, N'Mặc Hàng Ngày (Everyday Wear)', N'mac-hang-ngay', N'https://media3.coolmate.me/uploads/banner-everyday.jpg', N'Tủ đồ cơ bản tiện lợi, chất liệu Cotton Compact mềm mịn mặc êm suốt 24h.', 1)
INSERT [dbo].[Collections] ([id], [name], [slug], [banner_url], [description], [is_active]) VALUES (2, N'Đồ Chạy Bộ (Coolmate Running)', N'do-chay-bo', N'https://media3.coolmate.me/uploads/banner-running.jpg', N'Trang phục chạy bộ chuyên nghiệp: Siêu nhẹ, thoáng khí, không gây cọ xát (Anti-Chafing).', 1)
INSERT [dbo].[Collections] ([id], [name], [slug], [banner_url], [description], [is_active]) VALUES (3, N'Công Nghệ Làm Mát Excool', N'cong-nghe-excool', N'https://media3.coolmate.me/uploads/banner-excool.jpg', N'Sợi Sorona thực vật kết hợp công nghệ dệt làm mát tức thì, chống tia UV vượt trội.', 1)
INSERT [dbo].[Collections] ([id], [name], [slug], [banner_url], [description], [is_active]) VALUES (4, N'Care & Share - Thời Trang Thiện Nguyện', N'care-and-share', N'https://media3.coolmate.me/uploads/banner-careshare.jpg', N'Trích 10% doanh thu từ mỗi sản phẩm bán ra để đóng góp cho quỹ trẻ em có hoàn cảnh khó khăn.', 1)
SET IDENTITY_INSERT [dbo].[Collections] OFF
GO

SET IDENTITY_INSERT [dbo].[Colors] ON 
INSERT [dbo].[Colors] ([id], [name], [hex_code]) VALUES (1, N'Đen', N'#111111')
INSERT [dbo].[Colors] ([id], [name], [hex_code]) VALUES (2, N'Trắng', N'#FFFFFF')
INSERT [dbo].[Colors] ([id], [name], [hex_code]) VALUES (3, N'Xanh Navy', N'#1B2A4A')
INSERT [dbo].[Colors] ([id], [name], [hex_code]) VALUES (4, N'Xám Melange', N'#8E9196')
INSERT [dbo].[Colors] ([id], [name], [hex_code]) VALUES (5, N'Xanh Rêu', N'#3B5323')
INSERT [dbo].[Colors] ([id], [name], [hex_code]) VALUES (6, N'Xanh Biển (Aqua)', N'#0077BE')
INSERT [dbo].[Colors] ([id], [name], [hex_code]) VALUES (7, N'Be (Sand)', N'#E1D5C9')
SET IDENTITY_INSERT [dbo].[Colors] OFF
GO

SET IDENTITY_INSERT [dbo].[Sizes] ON 
INSERT [dbo].[Sizes] ([id], [name], [min_height_cm], [max_height_cm], [min_weight_kg], [max_weight_kg], [display_order]) VALUES (1, N'S', 155, 164, 48, 56, 1)
INSERT [dbo].[Sizes] ([id], [name], [min_height_cm], [max_height_cm], [min_weight_kg], [max_weight_kg], [display_order]) VALUES (2, N'M', 165, 169, 57, 64, 2)
INSERT [dbo].[Sizes] ([id], [name], [min_height_cm], [max_height_cm], [min_weight_kg], [max_weight_kg], [display_order]) VALUES (3, N'L', 170, 174, 65, 72, 3)
INSERT [dbo].[Sizes] ([id], [name], [min_height_cm], [max_height_cm], [min_weight_kg], [max_weight_kg], [display_order]) VALUES (4, N'XL', 175, 179, 73, 80, 4)
INSERT [dbo].[Sizes] ([id], [name], [min_height_cm], [max_height_cm], [min_weight_kg], [max_weight_kg], [display_order]) VALUES (5, N'2XL', 180, 185, 81, 88, 5)
INSERT [dbo].[Sizes] ([id], [name], [min_height_cm], [max_height_cm], [min_weight_kg], [max_weight_kg], [display_order]) VALUES (6, N'3XL', 185, 195, 89, 100, 6)
SET IDENTITY_INSERT [dbo].[Sizes] OFF
GO

SET IDENTITY_INSERT [dbo].[Products] ON 
INSERT [dbo].[Products] ([id], [category_id], [collection_id], [name], [slug], [short_description], [description], [material], [fit_type], [features], [base_price], [rating_avg], [review_count], [sold_count], [status], [created_at], [updated_at]) VALUES (1, 5, 1, N'Áo Thun Nam Cotton Compact In Logo Care & Share', N'ao-thun-nam-cotton-compact-premium', N'Áo thun quốc dân chống nhăn, không xù lông sau 50 lần giặt, mềm mại vượt trội.', N'Được dệt từ sợi Cotton Compact 100% chải kỹ giúp bề mặt vải mịn màng, hạn chế tối đa tình trạng đổ lông và bai dão cổ áo.', N'95% Cotton Compact, 5% Spandex', N'Regular Fit', N'Co giãn 4 chiều, Thấm hút mồ hôi, Hạn chế xù lông', CAST(299000.00 AS Decimal(18, 2)), CAST(4.90 AS Decimal(3, 2)), 2, 15420, N'ACTIVE', CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2), CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2))
INSERT [dbo].[Products] ([id], [category_id], [collection_id], [name], [slug], [short_description], [description], [material], [fit_type], [features], [base_price], [rating_avg], [review_count], [sold_count], [status], [created_at], [updated_at]) VALUES (2, 6, 3, N'Áo Polo Nam Công Nghệ Excool Trơn Khử Mùi', N'ao-polo-nam-excool-khu-mui', N'Áo Polo nhẹ chỉ 160g, mát lạnh tức thì, khô nhanh gấp 2 lần áo Polo thông thường.', N'Ứng dụng công nghệ sợi Sorona có nguồn gốc từ thực vật giúp áo có độ đàn hồi tự nhiên, chống tia UV UPF 50+ và khử mùi hiệu quả.', N'56% Polyester, 44% Sorona (Excool)', N'Regular Fit', N'Chống tia UV UPF50+, Nhanh khô, Chống nhăn không cần ủi', CAST(399000.00 AS Decimal(18, 2)), CAST(5.00 AS Decimal(3, 2)), 1, 9850, N'ACTIVE', CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2), CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2))
INSERT [dbo].[Products] ([id], [category_id], [collection_id], [name], [slug], [short_description], [description], [material], [fit_type], [features], [base_price], [rating_avg], [review_count], [sold_count], [status], [created_at], [updated_at]) VALUES (3, 7, 2, N'Áo Singlet Chạy Bộ Nam ProMax-S1 Siêu Nhẹ', N'ao-singlet-chay-bo-nam-promax-s1', N'Trọng lượng siêu nhẹ chỉ 75g, đường may ép nhiệt chống cọ xát khi chạy Half/Full Marathon.', N'Thiết kế chuyên biệt cho vận động viên và người yêu thích chạy bộ đường dài với cấu trúc dệt lỗ thoáng khí toàn thân.', N'100% Polyester Recycled Quick-Dry', N'Athletic Fit', N'Siêu nhẹ 75g, Ép nhiệt Ultrasonic, Thoáng khí tối đa', CAST(199000.00 AS Decimal(18, 2)), CAST(4.85 AS Decimal(3, 2)), 0, 6230, N'ACTIVE', CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2), CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2))
INSERT [dbo].[Products] ([id], [category_id], [collection_id], [name], [slug], [short_description], [description], [material], [fit_type], [features], [base_price], [rating_avg], [review_count], [sold_count], [status], [created_at], [updated_at]) VALUES (4, 8, 1, N'Áo Khoác Nam Có Mũ Daily Wear Trượt Nước Chống UV', N'ao-khoac-nam-daily-wear-truot-nuoc', N'Áo khoác gió 1 lớp mỏng nhẹ, trượt nước mưa nhẹ và cản gió, chống nắng UPF 50+.', N'Lớp phủ DWR (Durable Water Repellent) giúp hạt nước trượt ngay trên bề mặt lá sen, có thể gấp gọn vào túi trong chỉ bằng nắm tay.', N'100% Polyester phủ trượt nước DWR', N'Regular Fit', N'Trượt nước DWR, Chống nắng UPF50+, Cản gió, Gấp gọn tiện lợi', CAST(499000.00 AS Decimal(18, 2)), CAST(4.95 AS Decimal(3, 2)), 0, 7400, N'ACTIVE', CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2), CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2))
INSERT [dbo].[Products] ([id], [category_id], [collection_id], [name], [slug], [short_description], [description], [material], [fit_type], [features], [base_price], [rating_avg], [review_count], [sold_count], [status], [created_at], [updated_at]) VALUES (5, 9, 2, N'Quần Short Nam Chạy Bộ 5 Inch Advanced Running', N'quan-short-nam-chay-bo-5-inch', N'Quần short chạy bộ 2 lớp có lót đùi chống ma sát, tích hợp túi khóa kéo đựng điện thoại/Gel.', N'Độ dài 5 inch chuẩn chạy bộ giải phóng hoàn toàn chuyển động của đùi, đai lưng bản rộng thấm mồ hôi cực nhanh.', N'92% Polyester, 8% Spandex', N'Athletic Fit', N'2 lớp chống cọ đùi, Có túi khóa kéo sau lưng, Phản quang chạy đêm', CAST(299000.00 AS Decimal(18, 2)), CAST(4.90 AS Decimal(3, 2)), 1, 11200, N'ACTIVE', CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2), CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2))
INSERT [dbo].[Products] ([id], [category_id], [collection_id], [name], [slug], [short_description], [description], [material], [fit_type], [features], [base_price], [rating_avg], [review_count], [sold_count], [status], [created_at], [updated_at]) VALUES (6, 10, 1, N'Quần Dài Nam UT Pants Đa Năng Co Giãn 4 Chiều', N'quan-dai-nam-ut-pants-da-nang', N'Chiếc quần dài mặc đi làm, đi chơi hay đi du lịch đều lịch sự và thoải mái như quần thể thao.', N'Sử dụng sợi Nylon kết hợp Spandex co giãn 4 chiều, đầu gối không bị phồng sau thời gian dài ngồi văn phòng.', N'88% Nylon (Polyamide), 12% Spandex', N'Slim Fit', N'Co giãn 4 chiều, Trượt nước nhẹ, Không nhăn nhàu', CAST(499000.00 AS Decimal(18, 2)), CAST(4.80 AS Decimal(3, 2)), 0, 5310, N'ACTIVE', CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2), CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2))
INSERT [dbo].[Products] ([id], [category_id], [collection_id], [name], [slug], [short_description], [description], [material], [fit_type], [features], [base_price], [rating_avg], [review_count], [sold_count], [status], [created_at], [updated_at]) VALUES (7, 11, 1, N'Combo 3 Quần Lót Nam Trunk Bamboo Kháng Khuẩn', N'combo-3-quan-lot-nam-trunk-bamboo', N'Vải sợi tre tự nhiên mềm mịn gấp 2 lần Cotton, kháng khuẩn và khử mùi tự nhiên suốt ngày dài.', N'Đai lưng dệt Microfiber siêu êm không để lại vết hằn trên da, thiết kế dáng Trunk ôm gọn không bị cuộn ống quần.', N'95% Sợi tre tự nhiên (Bamboo), 5% Spandex', N'Slim Fit', N'Kháng khuẩn tự nhiên, Mềm mượt mát lạnh, Đai lưng chống hằn', CAST(289000.00 AS Decimal(18, 2)), CAST(5.00 AS Decimal(3, 2)), 1, 28900, N'ACTIVE', CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2), CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2))
INSERT [dbo].[Products] ([id], [category_id], [collection_id], [name], [slug], [short_description], [description], [material], [fit_type], [features], [base_price], [rating_avg], [review_count], [sold_count], [status], [created_at], [updated_at]) VALUES (8, 12, 2, N'Combo 3 Đôi Tất Nam Cổ Ngắn Thể Thao Đệm Êm Chân', N'combo-3-doi-tat-nam-co-ngan-the-thao', N'Tất thể thao dệt đệm xù phần gót và mũi chân giúp giảm chấn, mặt trên dệt lưới thoáng khí.', N'Công nghệ kháng khuẩn ion bạc ngăn mùi hôi chân hiệu quả ngay cả khi vận động cường độ cao.', N'80% Cotton Organic, 17% Spandex, 3% Rubber', N'Regular Fit', N'Đệm gót chống phồng rộp, Lưới thoáng khí mu bàn chân, Chống tuột gót', CAST(119000.00 AS Decimal(18, 2)), CAST(4.90 AS Decimal(3, 2)), 0, 19400, N'ACTIVE', CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2), CAST(N'2026-10-03T13:47:28.3500000' AS DateTime2))
SET IDENTITY_INSERT [dbo].[Products] OFF
GO

SET IDENTITY_INSERT [dbo].[Product_Variants] ON 
-- Variants kèm giá vốn import_price chuẩn xác để tính lãi lỗ:
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (1, 1, 1, 2, N'CM-TS01-BLK-M', CAST(299000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), CAST(95000.00 AS Decimal(18, 2)), 45, 220, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (2, 1, 1, 3, N'CM-TS01-BLK-L', CAST(299000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), CAST(95000.00 AS Decimal(18, 2)), 80, 230, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (3, 1, 1, 4, N'CM-TS01-BLK-XL', CAST(299000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), CAST(95000.00 AS Decimal(18, 2)), 60, 240, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (4, 1, 1, 5, N'CM-TS01-BLK-2XL', CAST(299000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), CAST(95000.00 AS Decimal(18, 2)), 25, 250, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (5, 1, 2, 2, N'CM-TS01-WHT-M', CAST(299000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), CAST(95000.00 AS Decimal(18, 2)), 50, 220, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (6, 1, 2, 3, N'CM-TS01-WHT-L', CAST(299000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), CAST(95000.00 AS Decimal(18, 2)), 70, 230, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (7, 1, 3, 3, N'CM-TS01-NVY-L', CAST(299000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), CAST(95000.00 AS Decimal(18, 2)), 65, 230, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (8, 1, 3, 4, N'CM-TS01-NVY-XL', CAST(299000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), CAST(95000.00 AS Decimal(18, 2)), 40, 240, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (9, 2, 3, 2, N'CM-PL02-NVY-M', CAST(399000.00 AS Decimal(18, 2)), CAST(349000.00 AS Decimal(18, 2)), CAST(145000.00 AS Decimal(18, 2)), 35, 180, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (10, 2, 3, 3, N'CM-PL02-NVY-L', CAST(399000.00 AS Decimal(18, 2)), CAST(349000.00 AS Decimal(18, 2)), CAST(145000.00 AS Decimal(18, 2)), 90, 190, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (11, 2, 3, 4, N'CM-PL02-NVY-XL', CAST(399000.00 AS Decimal(18, 2)), CAST(349000.00 AS Decimal(18, 2)), CAST(145000.00 AS Decimal(18, 2)), 55, 200, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (12, 2, 1, 3, N'CM-PL02-BLK-L', CAST(399000.00 AS Decimal(18, 2)), CAST(349000.00 AS Decimal(18, 2)), CAST(145000.00 AS Decimal(18, 2)), 75, 190, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (13, 2, 2, 3, N'CM-PL02-WHT-L', CAST(399000.00 AS Decimal(18, 2)), CAST(349000.00 AS Decimal(18, 2)), CAST(145000.00 AS Decimal(18, 2)), 40, 190, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (14, 3, 6, 2, N'CM-SG03-AQU-M', CAST(199000.00 AS Decimal(18, 2)), CAST(169000.00 AS Decimal(18, 2)), CAST(65000.00 AS Decimal(18, 2)), 30, 90, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (15, 3, 6, 3, N'CM-SG03-AQU-L', CAST(199000.00 AS Decimal(18, 2)), CAST(169000.00 AS Decimal(18, 2)), CAST(65000.00 AS Decimal(18, 2)), 50, 95, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (16, 3, 1, 3, N'CM-SG03-BLK-L', CAST(199000.00 AS Decimal(18, 2)), CAST(169000.00 AS Decimal(18, 2)), CAST(65000.00 AS Decimal(18, 2)), 45, 95, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (17, 4, 1, 3, N'CM-JK04-BLK-L', CAST(499000.00 AS Decimal(18, 2)), CAST(429000.00 AS Decimal(18, 2)), CAST(185000.00 AS Decimal(18, 2)), 40, 320, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (18, 4, 1, 4, N'CM-JK04-BLK-XL', CAST(499000.00 AS Decimal(18, 2)), CAST(429000.00 AS Decimal(18, 2)), CAST(185000.00 AS Decimal(18, 2)), 50, 340, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (19, 4, 5, 3, N'CM-JK04-OLV-L', CAST(499000.00 AS Decimal(18, 2)), CAST(429000.00 AS Decimal(18, 2)), CAST(185000.00 AS Decimal(18, 2)), 30, 320, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (20, 5, 1, 2, N'CM-SH05-BLK-M', CAST(299000.00 AS Decimal(18, 2)), CAST(259000.00 AS Decimal(18, 2)), CAST(105000.00 AS Decimal(18, 2)), 60, 160, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (21, 5, 1, 3, N'CM-SH05-BLK-L', CAST(299000.00 AS Decimal(18, 2)), CAST(259000.00 AS Decimal(18, 2)), CAST(105000.00 AS Decimal(18, 2)), 85, 170, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (22, 5, 1, 4, N'CM-SH05-BLK-XL', CAST(299000.00 AS Decimal(18, 2)), CAST(259000.00 AS Decimal(18, 2)), CAST(105000.00 AS Decimal(18, 2)), 45, 180, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (23, 5, 4, 3, N'CM-SH05-GRY-L', CAST(299000.00 AS Decimal(18, 2)), CAST(259000.00 AS Decimal(18, 2)), CAST(105000.00 AS Decimal(18, 2)), 40, 170, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (24, 6, 1, 3, N'CM-PT06-BLK-L', CAST(499000.00 AS Decimal(18, 2)), CAST(449000.00 AS Decimal(18, 2)), CAST(195000.00 AS Decimal(18, 2)), 55, 350, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (25, 6, 7, 4, N'CM-PT06-SND-XL', CAST(499000.00 AS Decimal(18, 2)), CAST(449000.00 AS Decimal(18, 2)), CAST(195000.00 AS Decimal(18, 2)), 35, 360, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (26, 7, 1, 3, N'CM-UN07-BLK-L', CAST(289000.00 AS Decimal(18, 2)), CAST(259000.00 AS Decimal(18, 2)), CAST(110000.00 AS Decimal(18, 2)), 120, 210, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (27, 7, 3, 4, N'CM-UN07-NVY-XL', CAST(289000.00 AS Decimal(18, 2)), CAST(259000.00 AS Decimal(18, 2)), CAST(110000.00 AS Decimal(18, 2)), 95, 220, 1, 0)
INSERT [dbo].[Product_Variants] ([id], [product_id], [color_id], [size_id], [sku], [original_price], [sale_price], [import_price], [stock_quantity], [weight_gram], [is_active], [version]) VALUES (28, 8, 2, 3, N'CM-SK08-WHT-L', CAST(119000.00 AS Decimal(18, 2)), CAST(99000.00 AS Decimal(18, 2)), CAST(38000.00 AS Decimal(18, 2)), 150, 120, 1, 0)
SET IDENTITY_INSERT [dbo].[Product_Variants] OFF
GO

SET IDENTITY_INSERT [dbo].[Product_Images] ON 
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (1, 1, 1, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/ao-thun-compact-den-1.jpg', 1, 1)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (2, 1, 1, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/ao-thun-compact-den-2.jpg', 0, 2)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (3, 1, 2, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/ao-thun-compact-trang-1.jpg', 0, 3)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (4, 1, 3, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/ao-thun-compact-navy-1.jpg', 0, 4)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (5, 2, 3, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/polo-excool-navy-1.jpg', 1, 1)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (6, 2, 1, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/polo-excool-den-1.jpg', 0, 2)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (7, 3, 6, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/singlet-promax-xanh-1.jpg', 1, 1)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (8, 4, 1, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/ao-khoac-daily-den-1.jpg', 1, 1)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (9, 5, 1, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/short-running-5inch-den-1.jpg', 1, 1)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (10, 6, 1, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/quan-ut-pants-den-1.jpg', 1, 1)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (11, 7, 1, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/boxer-bamboo-den-1.jpg', 1, 1)
INSERT [dbo].[Product_Images] ([id], [product_id], [color_id], [image_url], [is_thumbnail], [display_order]) VALUES (12, 8, 2, N'https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/tat-the-thao-trang-1.jpg', 1, 1)
SET IDENTITY_INSERT [dbo].[Product_Images] OFF
GO

SET IDENTITY_INSERT [dbo].[Combo_Rules] ON 
INSERT [dbo].[Combo_Rules] ([id], [name], [category_id], [min_quantity], [discount_percentage], [is_active]) VALUES (1, N'Mix & Match: Mua từ 3 Áo Thun Nam giảm thêm 10%', 5, 3, CAST(10.00 AS Decimal(5, 2)), 1)
INSERT [dbo].[Combo_Rules] ([id], [name], [category_id], [min_quantity], [discount_percentage], [is_active]) VALUES (2, N'Tủ đồ tiết kiệm: Mua từ 2 Áo Polo Nam giảm thêm 8%', 6, 2, CAST(8.00 AS Decimal(5, 2)), 1)
INSERT [dbo].[Combo_Rules] ([id], [name], [category_id], [min_quantity], [discount_percentage], [is_active]) VALUES (3, N'Combo Đồ Lót: Mua từ 2 hộp Quần Lót Trunk giảm thêm 12%', 11, 2, CAST(12.00 AS Decimal(5, 2)), 1)
SET IDENTITY_INSERT [dbo].[Combo_Rules] OFF
GO

SET IDENTITY_INSERT [dbo].[Promotions] ON 
INSERT [dbo].[Promotions] ([id], [code], [name], [discount_type], [discount_value], [max_discount_amount], [min_order_value], [usage_limit], [used_count], [start_date], [end_date], [is_active]) VALUES (1, N'COOLNEW50', N'Giảm 50.000đ cho đơn hàng đầu tiên từ 299.000đ', N'FIXED_AMOUNT', CAST(50000.00 AS Decimal(18, 2)), CAST(50000.00 AS Decimal(18, 2)), CAST(299000.00 AS Decimal(18, 2)), 1000, 12, CAST(N'2026-09-03T13:47:28.4133333' AS DateTime2), CAST(N'2027-04-03T13:47:28.4133333' AS DateTime2), 1)
INSERT [dbo].[Promotions] ([id], [code], [name], [discount_type], [discount_value], [max_discount_amount], [min_order_value], [usage_limit], [used_count], [start_date], [end_date], [is_active]) VALUES (2, N'COOLCLUB10', N'Ưu đãi Hội viên CoolClub: Giảm 10% tối đa 100K cho đơn từ 499K', N'PERCENTAGE', CAST(10.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(499000.00 AS Decimal(18, 2)), 500, 45, CAST(N'2026-09-03T13:47:28.4133333' AS DateTime2), CAST(N'2027-04-03T13:47:28.4133333' AS DateTime2), 1)
INSERT [dbo].[Promotions] ([id], [code], [name], [discount_type], [discount_value], [max_discount_amount], [min_order_value], [usage_limit], [used_count], [start_date], [end_date], [is_active]) VALUES (3, N'RUN2026', N'Giảm 15% tối đa 150K cho Bộ sưu tập Chạy bộ đơn từ 599K', N'PERCENTAGE', CAST(15.00 AS Decimal(18, 2)), CAST(150000.00 AS Decimal(18, 2)), CAST(599000.00 AS Decimal(18, 2)), 300, 8, CAST(N'2026-09-23T13:47:28.4133333' AS DateTime2), CAST(N'2027-01-03T13:47:28.4133333' AS DateTime2), 1)
SET IDENTITY_INSERT [dbo].[Promotions] OFF
GO

-- =============================================
-- DỮ LIỆU MẪU PHIẾU NHẬP KHO (INVENTORY RECEIPTS)
-- =============================================
SET IDENTITY_INSERT [dbo].[Inventory_Receipts] ON 
INSERT [dbo].[Inventory_Receipts] ([id], [receipt_code], [supplier_name], [created_by], [total_amount], [note], [status], [created_at]) VALUES (1, N'PNK20260901001', N'Công ty Cổ phần Dệt may Thành Công', 1, CAST(42650000.00 AS Decimal(18, 2)), N'Nhập lô hàng áo thun Cotton Compact và Polo Excool đợt đầu tháng 9', N'COMPLETED', CAST(N'2026-09-01T09:00:00.0000000' AS DateTime2))
INSERT [dbo].[Inventory_Receipts] ([id], [receipt_code], [supplier_name], [created_by], [total_amount], [note], [status], [created_at]) VALUES (2, N'PNK20260910002', N'Xưởng may Thể thao Tân Bình Pro', 2, CAST(35450000.00 AS Decimal(18, 2)), N'Nhập bổ sung quần short chạy bộ 5 inch và quần lót Trunk Bamboo', N'COMPLETED', CAST(N'2026-09-10T14:30:00.0000000' AS DateTime2))
INSERT [dbo].[Inventory_Receipts] ([id], [receipt_code], [supplier_name], [created_by], [total_amount], [note], [status], [created_at]) VALUES (3, N'PNK20260920003', N'Công ty TNHH Phụ kiện Dệt may Nam Định', 2, CAST(5700000.00 AS Decimal(18, 2)), N'Nhập lô tất nam thể thao đệm gót trắng', N'COMPLETED', CAST(N'2026-09-20T10:15:00.0000000' AS DateTime2))
SET IDENTITY_INSERT [dbo].[Inventory_Receipts] OFF
GO

-- =============================================
-- DỮ LIỆU MẪU CHI TIẾT PHIẾU NHẬP KHO (INVENTORY RECEIPT ITEMS)
-- =============================================
SET IDENTITY_INSERT [dbo].[Inventory_Receipt_Items] ON 
-- Phiếu 1
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (1, 1, 1, 50, CAST(95000.00 AS Decimal(18, 2)), CAST(4750000.00 AS Decimal(18, 2)))
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (2, 1, 2, 100, CAST(95000.00 AS Decimal(18, 2)), CAST(9500000.00 AS Decimal(18, 2)))
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (3, 1, 3, 80, CAST(95000.00 AS Decimal(18, 2)), CAST(7600000.00 AS Decimal(18, 2)))
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (4, 1, 9, 40, CAST(145000.00 AS Decimal(18, 2)), CAST(5800000.00 AS Decimal(18, 2)))
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (5, 1, 10, 100, CAST(145000.00 AS Decimal(18, 2)), CAST(14500000.00 AS Decimal(18, 2)))
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (6, 1, 14, 50, CAST(65000.00 AS Decimal(18, 2)), CAST(3250000.00 AS Decimal(18, 2)))

-- Phiếu 2
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (7, 2, 20, 80, CAST(105000.00 AS Decimal(18, 2)), CAST(8400000.00 AS Decimal(18, 2)))
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (8, 2, 21, 100, CAST(105000.00 AS Decimal(18, 2)), CAST(10500000.00 AS Decimal(18, 2)))
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (9, 2, 26, 150, CAST(110000.00 AS Decimal(18, 2)), CAST(16500000.00 AS Decimal(18, 2)))

-- Phiếu 3
INSERT [dbo].[Inventory_Receipt_Items] ([id], [receipt_id], [variant_id], [quantity], [import_price], [total_price]) VALUES (10, 3, 28, 150, CAST(38000.00 AS Decimal(18, 2)), CAST(5700000.00 AS Decimal(18, 2)))
SET IDENTITY_INSERT [dbo].[Inventory_Receipt_Items] OFF
GO

SET IDENTITY_INSERT [dbo].[Carts] ON 
INSERT [dbo].[Carts] ([id], [user_id], [session_id], [updated_at]) VALUES (1, 3, NULL, CAST(N'2026-10-03T13:47:28.4233333' AS DateTime2))
INSERT [dbo].[Carts] ([id], [user_id], [session_id], [updated_at]) VALUES (2, 5, NULL, CAST(N'2026-10-03T13:47:28.4233333' AS DateTime2))
SET IDENTITY_INSERT [dbo].[Carts] OFF
GO

SET IDENTITY_INSERT [dbo].[Cart_Items] ON 
INSERT [dbo].[Cart_Items] ([id], [cart_id], [variant_id], [quantity]) VALUES (1, 1, 2, 2)
INSERT [dbo].[Cart_Items] ([id], [cart_id], [variant_id], [quantity]) VALUES (2, 1, 10, 1)
INSERT [dbo].[Cart_Items] ([id], [cart_id], [variant_id], [quantity]) VALUES (3, 2, 26, 1)
SET IDENTITY_INSERT [dbo].[Cart_Items] OFF
GO

SET IDENTITY_INSERT [dbo].[Orders] ON 
INSERT [dbo].[Orders] ([id], [order_code], [user_id], [promotion_id], [recipient_name], [recipient_phone], [recipient_email], [shipping_address], [note], [subtotal_amount], [combo_discount_amount], [voucher_discount_amount], [coolcash_used], [shipping_fee], [final_amount], [coolcash_earned], [payment_method], [payment_status], [vnpay_transaction_no], [order_status], [delivered_at], [created_at], [updated_at]) VALUES (1, N'CM20260915001', 3, 2, N'Nguyễn Minh Tuấn', N'0988111222', N'tuan.nguyen@gmail.com', N'190 Quang Trung, Phường 10, Quận Gò Vấp, TP. Hồ Chí Minh', N'Giao giờ hành chính giúp mình', CAST(847000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(84700.00 AS Decimal(18, 2)), CAST(20000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(742300.00 AS Decimal(18, 2)), CAST(51961.00 AS Decimal(18, 2)), N'VNPAY', N'PAID', N'VNP14589231', N'COMPLETED', CAST(N'2026-09-18T13:47:28.4333333' AS DateTime2), CAST(N'2026-09-15T13:47:28.4333333' AS DateTime2), CAST(N'2026-10-03T13:47:28.4333333' AS DateTime2))
INSERT [dbo].[Orders] ([id], [order_code], [user_id], [promotion_id], [recipient_name], [recipient_phone], [recipient_email], [shipping_address], [note], [subtotal_amount], [combo_discount_amount], [voucher_discount_amount], [coolcash_used], [shipping_fee], [final_amount], [coolcash_earned], [payment_method], [payment_status], [vnpay_transaction_no], [order_status], [delivered_at], [created_at], [updated_at]) VALUES (2, N'CM20260925002', 4, 1, N'Trần Hoàng Nam', N'0977333444', N'hoangnam.tran@gmail.com', N'Số 85 Xuân Thủy, Phường Dịch Vọng Hậu, Quận Cầu Giấy, TP. Hà Nội', N'Gọi trước khi giao 15 phút', CAST(518000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(50000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(468000.00 AS Decimal(18, 2)), CAST(46800.00 AS Decimal(18, 2)), N'COD', N'PAID', NULL, N'DELIVERED', CAST(N'2026-09-28T13:47:28.4333333' AS DateTime2), CAST(N'2026-09-25T13:47:28.4333333' AS DateTime2), CAST(N'2026-10-03T13:47:28.4333333' AS DateTime2))
INSERT [dbo].[Orders] ([id], [order_code], [user_id], [promotion_id], [recipient_name], [recipient_phone], [recipient_email], [shipping_address], [note], [subtotal_amount], [combo_discount_amount], [voucher_discount_amount], [coolcash_used], [shipping_fee], [final_amount], [coolcash_earned], [payment_method], [payment_status], [vnpay_transaction_no], [order_status], [delivered_at], [created_at], [updated_at]) VALUES (3, N'CM20261003003', 5, NULL, N'Lê Quốc Bảo', N'0912555666', N'quocbao.le@gmail.com', N'Số 42 Nguyễn Chí Thanh, Phường Thạch Thang, Quận Hải Châu, TP. Đà Nẵng', NULL, CAST(249000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), CAST(7470.00 AS Decimal(18, 2)), N'COD', N'UNPAID', NULL, N'PENDING', NULL, CAST(N'2026-10-03T13:47:28.4333333' AS DateTime2), CAST(N'2026-10-03T13:47:28.4333333' AS DateTime2))
SET IDENTITY_INSERT [dbo].[Orders] OFF
GO

SET IDENTITY_INSERT [dbo].[Order_Items] ON 
INSERT [dbo].[Order_Items] ([id], [order_id], [variant_id], [product_name_snapshot], [sku_snapshot], [color_name_snapshot], [size_name_snapshot], [quantity], [unit_price], [total_price], [is_reviewed]) VALUES (1, 1, 2, N'Áo Thun Nam Cotton Compact In Logo Care & Share', N'CM-TS01-BLK-L', N'Đen', N'L', 2, CAST(249000.00 AS Decimal(18, 2)), CAST(498000.00 AS Decimal(18, 2)), 1)
INSERT [dbo].[Order_Items] ([id], [order_id], [variant_id], [product_name_snapshot], [sku_snapshot], [color_name_snapshot], [size_name_snapshot], [quantity], [unit_price], [total_price], [is_reviewed]) VALUES (2, 1, 10, N'Áo Polo Nam Công Nghệ Excool Trơn Khử Mùi', N'CM-PL02-NVY-L', N'Xanh Navy', N'L', 1, CAST(349000.00 AS Decimal(18, 2)), CAST(349000.00 AS Decimal(18, 2)), 1)
INSERT [dbo].[Order_Items] ([id], [order_id], [variant_id], [product_name_snapshot], [sku_snapshot], [color_name_snapshot], [size_name_snapshot], [quantity], [unit_price], [total_price], [is_reviewed]) VALUES (3, 2, 21, N'Quần Short Nam Chạy Bộ 5 Inch Advanced Running', N'CM-SH05-BLK-L', N'Đen', N'L', 1, CAST(259000.00 AS Decimal(18, 2)), CAST(259000.00 AS Decimal(18, 2)), 1)
INSERT [dbo].[Order_Items] ([id], [order_id], [variant_id], [product_name_snapshot], [sku_snapshot], [color_name_snapshot], [size_name_snapshot], [quantity], [unit_price], [total_price], [is_reviewed]) VALUES (4, 2, 27, N'Combo 3 Quần Lót Nam Trunk Bamboo Kháng Khuẩn', N'CM-UN07-NVY-XL', N'Xanh Navy', N'XL', 1, CAST(259000.00 AS Decimal(18, 2)), CAST(259000.00 AS Decimal(18, 2)), 1)
INSERT [dbo].[Order_Items] ([id], [order_id], [variant_id], [product_name_snapshot], [sku_snapshot], [color_name_snapshot], [size_name_snapshot], [quantity], [unit_price], [total_price], [is_reviewed]) VALUES (5, 3, 1, N'Áo Thun Nam Cotton Compact In Logo Care & Share', N'CM-TS01-BLK-M', N'Đen', N'M', 1, CAST(249000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)), 0)
SET IDENTITY_INSERT [dbo].[Order_Items] OFF
GO

SET IDENTITY_INSERT [dbo].[Order_Returns] ON 
INSERT [dbo].[Order_Returns] ([id], [order_id], [order_item_id], [user_id], [return_type], [target_variant_id], [quantity], [reason], [evidence_images], [status], [refund_amount], [created_at]) VALUES (1, 2, 3, 4, N'EXCHANGE_SIZE', 22, 1, N'Mình cao 1m78 nặng 76kg mặc quần Short Size L hơi ôm đùi, muốn đổi sang Size XL cho thoải mái khi chạy bộ.', N'https://media3.coolmate.me/uploads/returns/return-short-1.jpg', N'APPROVED', CAST(0.00 AS Decimal(18, 2)), CAST(N'2026-10-01T13:47:28.4400000' AS DateTime2))
SET IDENTITY_INSERT [dbo].[Order_Returns] OFF
GO

SET IDENTITY_INSERT [dbo].[Reviews] ON 
INSERT [dbo].[Reviews] ([id], [product_id], [user_id], [order_item_id], [rating], [comment], [customer_height_cm], [customer_weight_kg], [purchased_color], [purchased_size], [fit_feedback], [image_urls], [admin_reply], [is_approved], [created_at]) VALUES (1, 1, 3, 1, 5, N'Vải Cotton Compact dày dặn nhưng mặc rất mát, giặt máy không bị nhăn cổ áo. Mình cao 1m73 nặng 68kg chọn Size L theo công cụ gợi ý của web mặc vừa in!', 173, 68, N'Đen', N'L', N'TRUE_TO_SIZE', N'https://media3.coolmate.me/uploads/reviews/rv-compact-1.jpg', N'Coolmate cảm ơn bạn Minh Tuấn đã tin tưởng lựa chọn sản phẩm Care & Share nhé!', 1, CAST(N'2026-09-21T13:47:28.4466667' AS DateTime2))
INSERT [dbo].[Reviews] ([id], [product_id], [user_id], [order_item_id], [rating], [comment], [customer_height_cm], [customer_weight_kg], [purchased_color], [purchased_size], [fit_feedback], [image_urls], [admin_reply], [is_approved], [created_at]) VALUES (2, 2, 3, 2, 5, N'Áo Polo Excool nhẹ tênh, mặc đi làm cả ngày ở Sài Gòn không hề bị bí mồ hôi. Màu Xanh Navy bên ngoài rất sang.', 173, 68, N'Xanh Navy', N'L', N'TRUE_TO_SIZE', NULL, N'Cảm ơn bạn Tuấn, chúc bạn luôn có trải nghiệm tuyệt vời cùng dòng sản phẩm Excool!', 1, CAST(N'2026-09-21T13:47:28.4466667' AS DateTime2))
INSERT [dbo].[Reviews] ([id], [product_id], [user_id], [order_item_id], [rating], [comment], [customer_height_cm], [customer_weight_kg], [purchased_color], [purchased_size], [fit_feedback], [image_urls], [admin_reply], [is_approved], [created_at]) VALUES (3, 5, 4, 3, 5, N'Quần chạy bộ 5 inch có lớp lót đùi rất êm, túi khóa kéo phía sau đựng vừa điện thoại. Mình 1m78 - 76kg mặc L hơi ôm nên đã được CSKH hỗ trợ đổi lên XL siêu nhanh.', 178, 76, N'Đen', N'L', N'SMALL', NULL, N'Dạ Coolmate đã gửi đơn đổi Size XL tới bạn Nam, cảm ơn bạn đã ủng hộ Coolmate Running!', 1, CAST(N'2026-09-30T13:47:28.4466667' AS DateTime2))
INSERT [dbo].[Reviews] ([id], [product_id], [user_id], [order_item_id], [rating], [comment], [customer_height_cm], [customer_weight_kg], [purchased_color], [purchased_size], [fit_feedback], [image_urls], [admin_reply], [is_approved], [created_at]) VALUES (4, 7, 4, 4, 5, N'Quần lót vải sợi tre Bamboo mềm thật sự, cạp quần co giãn tốt không bị hằn bụng. Sẽ mua thêm combo nữa.', 178, 76, N'Xanh Navy', N'XL', N'TRUE_TO_SIZE', NULL, NULL, 1, CAST(N'2026-09-30T13:47:28.4466667' AS DateTime2))
SET IDENTITY_INSERT [dbo].[Reviews] OFF
GO

SET IDENTITY_INSERT [dbo].[CoolCash_Transactions] ON 
INSERT [dbo].[CoolCash_Transactions] ([id], [user_id], [order_id], [amount], [transaction_type], [status], [description], [created_at]) VALUES (1, 3, 1, CAST(-20000.00 AS Decimal(18, 2)), N'SPEND_ORDER', N'COMPLETED', N'Sử dụng 20.000 CoolCash thanh toán đơn hàng #CM20260915001', CAST(N'2026-09-15T13:47:28.4500000' AS DateTime2))
INSERT [dbo].[CoolCash_Transactions] ([id], [user_id], [order_id], [amount], [transaction_type], [status], [description], [created_at]) VALUES (2, 3, 1, CAST(51961.00 AS Decimal(18, 2)), N'EARN_ORDER', N'COMPLETED', N'Hoàn tiền 7% hạng Vàng (GOLD) từ đơn hàng #CM20260915001', CAST(N'2026-09-18T13:47:28.4500000' AS DateTime2))
INSERT [dbo].[CoolCash_Transactions] ([id], [user_id], [order_id], [amount], [transaction_type], [status], [description], [created_at]) VALUES (3, 3, 1, CAST(2000.00 AS Decimal(18, 2)), N'EARN_REVIEW', N'COMPLETED', N'Thưởng 2.000 CoolCash khi đánh giá sản phẩm Áo Thun Cotton Compact', CAST(N'2026-09-21T13:47:28.4500000' AS DateTime2))
INSERT [dbo].[CoolCash_Transactions] ([id], [user_id], [order_id], [amount], [transaction_type], [status], [description], [created_at]) VALUES (4, 4, 2, CAST(46800.00 AS Decimal(18, 2)), N'EARN_ORDER', N'PENDING', N'Hoàn tiền 10% hạng Bạch Kim (PLATINUM) tạm tính từ đơn hàng #CM20260925002', CAST(N'2026-09-28T13:47:28.4500000' AS DateTime2))
SET IDENTITY_INSERT [dbo].[CoolCash_Transactions] OFF
GO

-- =============================================
-- INDEXES & CONSTRAINTS
-- =============================================
ALTER TABLE [dbo].[Cart_Items] ADD CONSTRAINT [UQ_Cart_Variant] UNIQUE NONCLUSTERED ([cart_id] ASC, [variant_id] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Carts] ADD UNIQUE NONCLUSTERED ([user_id] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Categories] ADD CONSTRAINT [UK_Categories_Slug] UNIQUE NONCLUSTERED ([slug] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Collections] ADD UNIQUE NONCLUSTERED ([slug] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Orders] ADD CONSTRAINT [UK_Orders_Code] UNIQUE NONCLUSTERED ([order_code] ASC) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_Orders_User_Status] ON [dbo].[Orders] ([user_id] ASC, [order_status] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Product_Variants] ADD CONSTRAINT [UK_Variants_Sku] UNIQUE NONCLUSTERED ([sku] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Product_Variants] ADD CONSTRAINT [UQ_Product_Color_Size] UNIQUE NONCLUSTERED ([product_id] ASC, [color_id] ASC, [size_id] ASC) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_Variants_Price] ON [dbo].[Product_Variants] ([sale_price] ASC) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_Variants_Product_Color_Size] ON [dbo].[Product_Variants] ([product_id] ASC, [color_id] ASC, [size_id] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Products] ADD CONSTRAINT [UK_Products_Slug] UNIQUE NONCLUSTERED ([slug] ASC) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_Products_Category] ON [dbo].[Products] ([category_id] ASC, [status] ASC) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_Products_Collection] ON [dbo].[Products] ([collection_id] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Promotions] ADD CONSTRAINT [UK_Promotions_Code] UNIQUE NONCLUSTERED ([code] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Reviews] ADD CONSTRAINT [UQ_Reviews_OrderItem] UNIQUE NONCLUSTERED ([order_item_id] ASC) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_Reviews_Product] ON [dbo].[Reviews] ([product_id] ASC, [is_approved] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Roles] ADD UNIQUE NONCLUSTERED ([role_name] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Sizes] ADD UNIQUE NONCLUSTERED ([name] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Users] ADD UNIQUE NONCLUSTERED ([email] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Users] ADD UNIQUE NONCLUSTERED ([phone] ASC) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Inventory_Receipts] ADD CONSTRAINT [UK_Receipt_Code] UNIQUE NONCLUSTERED ([receipt_code] ASC) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_Receipts_CreatedAt] ON [dbo].[Inventory_Receipts] ([created_at] DESC) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_ReceiptItems_Receipt] ON [dbo].[Inventory_Receipt_Items] ([receipt_id] ASC) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_ReceiptItems_Variant] ON [dbo].[Inventory_Receipt_Items] ([variant_id] ASC) ON [PRIMARY]
GO

-- =============================================
-- DEFAULT VALUES
-- =============================================
ALTER TABLE [dbo].[Carts] ADD DEFAULT (getdate()) FOR [updated_at]
GO
ALTER TABLE [dbo].[Categories] ADD DEFAULT ((0)) FOR [display_order]
GO
ALTER TABLE [dbo].[Categories] ADD DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[Collections] ADD DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[Combo_Rules] ADD DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[CoolCash_Transactions] ADD DEFAULT ('COMPLETED') FOR [status]
GO
ALTER TABLE [dbo].[CoolCash_Transactions] ADD DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[Order_Items] ADD DEFAULT ((0)) FOR [is_reviewed]
GO
ALTER TABLE [dbo].[Order_Returns] ADD DEFAULT ('REQUESTED') FOR [status]
GO
ALTER TABLE [dbo].[Order_Returns] ADD DEFAULT ((0)) FOR [refund_amount]
GO
ALTER TABLE [dbo].[Order_Returns] ADD DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[Orders] ADD DEFAULT ((0)) FOR [combo_discount_amount]
GO
ALTER TABLE [dbo].[Orders] ADD DEFAULT ((0)) FOR [voucher_discount_amount]
GO
ALTER TABLE [dbo].[Orders] ADD DEFAULT ((0)) FOR [coolcash_used]
GO
ALTER TABLE [dbo].[Orders] ADD DEFAULT ((0)) FOR [shipping_fee]
GO
ALTER TABLE [dbo].[Orders] ADD DEFAULT ((0)) FOR [coolcash_earned]
GO
ALTER TABLE [dbo].[Orders] ADD DEFAULT ('UNPAID') FOR [payment_status]
GO
ALTER TABLE [dbo].[Orders] ADD DEFAULT ('PENDING') FOR [order_status]
GO
ALTER TABLE [dbo].[Orders] ADD DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[Orders] ADD DEFAULT (getdate()) FOR [updated_at]
GO
ALTER TABLE [dbo].[Product_Images] ADD DEFAULT ((0)) FOR [is_thumbnail]
GO
ALTER TABLE [dbo].[Product_Images] ADD DEFAULT ((0)) FOR [display_order]
GO
ALTER TABLE [dbo].[Product_Variants] ADD DEFAULT ((0)) FOR [import_price]
GO
ALTER TABLE [dbo].[Product_Variants] ADD DEFAULT ((0)) FOR [stock_quantity]
GO
ALTER TABLE [dbo].[Product_Variants] ADD DEFAULT ((250)) FOR [weight_gram]
GO
ALTER TABLE [dbo].[Product_Variants] ADD DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[Product_Variants] ADD DEFAULT ((0)) FOR [version]
GO
ALTER TABLE [dbo].[Products] ADD DEFAULT ((5.00)) FOR [rating_avg]
GO
ALTER TABLE [dbo].[Products] ADD DEFAULT ((0)) FOR [review_count]
GO
ALTER TABLE [dbo].[Products] ADD DEFAULT ((0)) FOR [sold_count]
GO
ALTER TABLE [dbo].[Products] ADD DEFAULT ('ACTIVE') FOR [status]
GO
ALTER TABLE [dbo].[Products] ADD DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[Products] ADD DEFAULT (getdate()) FOR [updated_at]
GO
ALTER TABLE [dbo].[Promotions] ADD DEFAULT ((0)) FOR [min_order_value]
GO
ALTER TABLE [dbo].[Promotions] ADD DEFAULT ((0)) FOR [used_count]
GO
ALTER TABLE [dbo].[Promotions] ADD DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[Reviews] ADD DEFAULT ('TRUE_TO_SIZE') FOR [fit_feedback]
GO
ALTER TABLE [dbo].[Reviews] ADD DEFAULT ((1)) FOR [is_approved]
GO
ALTER TABLE [dbo].[Reviews] ADD DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[User_Addresses] ADD DEFAULT ((0)) FOR [is_default]
GO
ALTER TABLE [dbo].[Users] ADD DEFAULT (N'Nam') FOR [gender]
GO
ALTER TABLE [dbo].[Users] ADD DEFAULT ('NEW') FOR [membership_tier]
GO
ALTER TABLE [dbo].[Users] ADD DEFAULT ((0)) FOR [coolcash_balance]
GO
ALTER TABLE [dbo].[Users] ADD DEFAULT ((0)) FOR [total_spent]
GO
ALTER TABLE [dbo].[Users] ADD DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[Users] ADD DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[Users] ADD DEFAULT (getdate()) FOR [updated_at]
GO
ALTER TABLE [dbo].[Inventory_Receipts] ADD DEFAULT ((0)) FOR [total_amount]
GO
ALTER TABLE [dbo].[Inventory_Receipts] ADD DEFAULT ('COMPLETED') FOR [status]
GO
ALTER TABLE [dbo].[Inventory_Receipts] ADD DEFAULT (getdate()) FOR [created_at]
GO

-- =============================================
-- FOREIGN KEYS
-- =============================================
ALTER TABLE [dbo].[Cart_Items] WITH CHECK ADD CONSTRAINT [FK_CartItems_Cart] FOREIGN KEY([cart_id]) REFERENCES [dbo].[Carts] ([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Cart_Items] CHECK CONSTRAINT [FK_CartItems_Cart]
GO
ALTER TABLE [dbo].[Cart_Items] WITH CHECK ADD CONSTRAINT [FK_CartItems_Variant] FOREIGN KEY([variant_id]) REFERENCES [dbo].[Product_Variants] ([id])
GO
ALTER TABLE [dbo].[Cart_Items] CHECK CONSTRAINT [FK_CartItems_Variant]
GO
ALTER TABLE [dbo].[Carts] WITH CHECK ADD CONSTRAINT [FK_Carts_User] FOREIGN KEY([user_id]) REFERENCES [dbo].[Users] ([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Carts] CHECK CONSTRAINT [FK_Carts_User]
GO
ALTER TABLE [dbo].[Categories] WITH CHECK ADD CONSTRAINT [FK_Categories_Parent] FOREIGN KEY([parent_id]) REFERENCES [dbo].[Categories] ([id])
GO
ALTER TABLE [dbo].[Categories] CHECK CONSTRAINT [FK_Categories_Parent]
GO
ALTER TABLE [dbo].[Combo_Rules] WITH CHECK ADD CONSTRAINT [FK_ComboRules_Category] FOREIGN KEY([category_id]) REFERENCES [dbo].[Categories] ([id])
GO
ALTER TABLE [dbo].[Combo_Rules] CHECK CONSTRAINT [FK_ComboRules_Category]
GO
ALTER TABLE [dbo].[CoolCash_Transactions] WITH CHECK ADD CONSTRAINT [FK_CoolCash_Order] FOREIGN KEY([order_id]) REFERENCES [dbo].[Orders] ([id])
GO
ALTER TABLE [dbo].[CoolCash_Transactions] CHECK CONSTRAINT [FK_CoolCash_Order]
GO
ALTER TABLE [dbo].[CoolCash_Transactions] WITH CHECK ADD CONSTRAINT [FK_CoolCash_User] FOREIGN KEY([user_id]) REFERENCES [dbo].[Users] ([id])
GO
ALTER TABLE [dbo].[CoolCash_Transactions] CHECK CONSTRAINT [FK_CoolCash_User]
GO
ALTER TABLE [dbo].[Order_Items] WITH CHECK ADD CONSTRAINT [FK_OrderItems_Order] FOREIGN KEY([order_id]) REFERENCES [dbo].[Orders] ([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Order_Items] CHECK CONSTRAINT [FK_OrderItems_Order]
GO
ALTER TABLE [dbo].[Order_Items] WITH CHECK ADD CONSTRAINT [FK_OrderItems_Variant] FOREIGN KEY([variant_id]) REFERENCES [dbo].[Product_Variants] ([id])
GO
ALTER TABLE [dbo].[Order_Items] CHECK CONSTRAINT [FK_OrderItems_Variant]
GO
ALTER TABLE [dbo].[Order_Returns] WITH CHECK ADD CONSTRAINT [FK_Returns_Order] FOREIGN KEY([order_id]) REFERENCES [dbo].[Orders] ([id])
GO
ALTER TABLE [dbo].[Order_Returns] CHECK CONSTRAINT [FK_Returns_Order]
GO
ALTER TABLE [dbo].[Order_Returns] WITH CHECK ADD CONSTRAINT [FK_Returns_OrderItem] FOREIGN KEY([order_item_id]) REFERENCES [dbo].[Order_Items] ([id])
GO
ALTER TABLE [dbo].[Order_Returns] CHECK CONSTRAINT [FK_Returns_OrderItem]
GO
ALTER TABLE [dbo].[Order_Returns] WITH CHECK ADD CONSTRAINT [FK_Returns_TargetVariant] FOREIGN KEY([target_variant_id]) REFERENCES [dbo].[Product_Variants] ([id])
GO
ALTER TABLE [dbo].[Order_Returns] CHECK CONSTRAINT [FK_Returns_TargetVariant]
GO
ALTER TABLE [dbo].[Order_Returns] WITH CHECK ADD CONSTRAINT [FK_Returns_User] FOREIGN KEY([user_id]) REFERENCES [dbo].[Users] ([id])
GO
ALTER TABLE [dbo].[Order_Returns] CHECK CONSTRAINT [FK_Returns_User]
GO
ALTER TABLE [dbo].[Orders] WITH CHECK ADD CONSTRAINT [FK_Orders_Promotion] FOREIGN KEY([promotion_id]) REFERENCES [dbo].[Promotions] ([id])
GO
ALTER TABLE [dbo].[Orders] CHECK CONSTRAINT [FK_Orders_Promotion]
GO
ALTER TABLE [dbo].[Orders] WITH CHECK ADD CONSTRAINT [FK_Orders_User] FOREIGN KEY([user_id]) REFERENCES [dbo].[Users] ([id])
GO
ALTER TABLE [dbo].[Orders] CHECK CONSTRAINT [FK_Orders_User]
GO
ALTER TABLE [dbo].[Product_Images] WITH CHECK ADD CONSTRAINT [FK_Images_Color] FOREIGN KEY([color_id]) REFERENCES [dbo].[Colors] ([id])
GO
ALTER TABLE [dbo].[Product_Images] CHECK CONSTRAINT [FK_Images_Color]
GO
ALTER TABLE [dbo].[Product_Images] WITH CHECK ADD CONSTRAINT [FK_Images_Product] FOREIGN KEY([product_id]) REFERENCES [dbo].[Products] ([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Product_Images] CHECK CONSTRAINT [FK_Images_Product]
GO
ALTER TABLE [dbo].[Product_Variants] WITH CHECK ADD CONSTRAINT [FK_Variants_Color] FOREIGN KEY([color_id]) REFERENCES [dbo].[Colors] ([id])
GO
ALTER TABLE [dbo].[Product_Variants] CHECK CONSTRAINT [FK_Variants_Color]
GO
ALTER TABLE [dbo].[Product_Variants] WITH CHECK ADD CONSTRAINT [FK_Variants_Product] FOREIGN KEY([product_id]) REFERENCES [dbo].[Products] ([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Product_Variants] CHECK CONSTRAINT [FK_Variants_Product]
GO
ALTER TABLE [dbo].[Product_Variants] WITH CHECK ADD CONSTRAINT [FK_Variants_Size] FOREIGN KEY([size_id]) REFERENCES [dbo].[Sizes] ([id])
GO
ALTER TABLE [dbo].[Product_Variants] CHECK CONSTRAINT [FK_Variants_Size]
GO
ALTER TABLE [dbo].[Products] WITH CHECK ADD CONSTRAINT [FK_Products_Category] FOREIGN KEY([category_id]) REFERENCES [dbo].[Categories] ([id])
GO
ALTER TABLE [dbo].[Products] CHECK CONSTRAINT [FK_Products_Category]
GO
ALTER TABLE [dbo].[Products] WITH CHECK ADD CONSTRAINT [FK_Products_Collection] FOREIGN KEY([collection_id]) REFERENCES [dbo].[Collections] ([id])
GO
ALTER TABLE [dbo].[Products] CHECK CONSTRAINT [FK_Products_Collection]
GO
ALTER TABLE [dbo].[Reviews] WITH CHECK ADD CONSTRAINT [FK_Reviews_OrderItem] FOREIGN KEY([order_item_id]) REFERENCES [dbo].[Order_Items] ([id])
GO
ALTER TABLE [dbo].[Reviews] CHECK CONSTRAINT [FK_Reviews_OrderItem]
GO
ALTER TABLE [dbo].[Reviews] WITH CHECK ADD CONSTRAINT [FK_Reviews_Product] FOREIGN KEY([product_id]) REFERENCES [dbo].[Products] ([id])
GO
ALTER TABLE [dbo].[Reviews] CHECK CONSTRAINT [FK_Reviews_Product]
GO
ALTER TABLE [dbo].[Reviews] WITH CHECK ADD CONSTRAINT [FK_Reviews_User] FOREIGN KEY([user_id]) REFERENCES [dbo].[Users] ([id])
GO
ALTER TABLE [dbo].[Reviews] CHECK CONSTRAINT [FK_Reviews_User]
GO
ALTER TABLE [dbo].[User_Addresses] WITH CHECK ADD CONSTRAINT [FK_Addresses_User] FOREIGN KEY([user_id]) REFERENCES [dbo].[Users] ([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[User_Addresses] CHECK CONSTRAINT [FK_Addresses_User]
GO
ALTER TABLE [dbo].[User_Roles] WITH CHECK ADD CONSTRAINT [FK_UserRoles_Role] FOREIGN KEY([role_id]) REFERENCES [dbo].[Roles] ([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[User_Roles] CHECK CONSTRAINT [FK_UserRoles_Role]
GO
ALTER TABLE [dbo].[User_Roles] WITH CHECK ADD CONSTRAINT [FK_UserRoles_User] FOREIGN KEY([user_id]) REFERENCES [dbo].[Users] ([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[User_Roles] CHECK CONSTRAINT [FK_UserRoles_User]
GO
ALTER TABLE [dbo].[Inventory_Receipts] WITH CHECK ADD CONSTRAINT [FK_Receipts_User] FOREIGN KEY([created_by]) REFERENCES [dbo].[Users] ([id])
GO
ALTER TABLE [dbo].[Inventory_Receipts] CHECK CONSTRAINT [FK_Receipts_User]
GO
ALTER TABLE [dbo].[Inventory_Receipt_Items] WITH CHECK ADD CONSTRAINT [FK_ReceiptItems_Receipt] FOREIGN KEY([receipt_id]) REFERENCES [dbo].[Inventory_Receipts] ([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Inventory_Receipt_Items] CHECK CONSTRAINT [FK_ReceiptItems_Receipt]
GO
ALTER TABLE [dbo].[Inventory_Receipt_Items] WITH CHECK ADD CONSTRAINT [FK_ReceiptItems_Variant] FOREIGN KEY([variant_id]) REFERENCES [dbo].[Product_Variants] ([id])
GO
ALTER TABLE [dbo].[Inventory_Receipt_Items] CHECK CONSTRAINT [FK_ReceiptItems_Variant]
GO

-- =============================================
-- CHECK CONSTRAINTS
-- =============================================
ALTER TABLE [dbo].[Cart_Items] WITH CHECK ADD CHECK (([quantity]>(0)))
GO
ALTER TABLE [dbo].[Order_Items] WITH CHECK ADD CHECK (([quantity]>(0)))
GO
ALTER TABLE [dbo].[Order_Returns] WITH CHECK ADD CHECK (([quantity]>(0)))
GO
ALTER TABLE [dbo].[Product_Variants] WITH CHECK ADD CHECK (([stock_quantity]>=(0)))
GO
ALTER TABLE [dbo].[Product_Variants] WITH CHECK ADD CHECK (([import_price]>=(0)))
GO
ALTER TABLE [dbo].[Inventory_Receipt_Items] WITH CHECK ADD CHECK (([quantity]>(0)))
GO
ALTER TABLE [dbo].[Inventory_Receipt_Items] WITH CHECK ADD CHECK (([import_price]>=(0)))
GO
ALTER TABLE [dbo].[Reviews] WITH CHECK ADD CHECK (([rating]>=(1) AND [rating]<=(5)))
GO
ALTER TABLE [dbo].[Users] WITH CHECK ADD CHECK (([coolcash_balance]>=(0)))
GO
USE [master]
GO
ALTER DATABASE [CoolMate_DB] SET READ_WRITE 
GO


-- ==========================================
-- D2C EXTENSION & MIGRATIONS
-- ==========================================
-- ===================================================================
-- COOLMATESHOP - D2C SYSTEM DATABASE MIGRATION SCRIPT
-- SQL Server 2016+ Compatible - Idempotent Execution
-- ===================================================================

USE [CoolMate_DB];
GO

-- 1. Order_Items: cost_price_snapshot for historical COGS calculation
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Items]') AND name = 'cost_price_snapshot')
BEGIN
    ALTER TABLE [dbo].[Order_Items] ADD [cost_price_snapshot] [decimal](18, 2) NULL;
    PRINT 'Added cost_price_snapshot to Order_Items';
END
GO

-- 2. Orders: VNPAY transaction reference, cancellation and payment tracking
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'vnpay_txn_ref')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [vnpay_txn_ref] [varchar](100) NULL;
    PRINT 'Added vnpay_txn_ref to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_paid_at')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_paid_at] [datetime2](7) NULL;
    PRINT 'Added payment_paid_at to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'cancelled_at')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [cancelled_at] [datetime2](7) NULL;
    PRINT 'Added cancelled_at to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'cancelled_reason')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [cancelled_reason] [nvarchar](500) NULL;
    PRINT 'Added cancelled_reason to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_response_code')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_response_code] [varchar](50) NULL;
    PRINT 'Added payment_response_code to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_bank_code')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_bank_code] [varchar](50) NULL;
    PRINT 'Added payment_bank_code to Orders';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Orders]') AND name = 'payment_failure_reason')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD [payment_failure_reason] [nvarchar](500) NULL;
    PRINT 'Added payment_failure_reason to Orders';
END
GO


-- 2B. Orders: Refund Tracking Columns
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

-- 3. Inventory_Receipts: approval workflow and audit
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Receipts]') AND name = 'approved_by')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipts] ADD [approved_by] [bigint] NULL;
    ALTER TABLE [dbo].[Inventory_Receipts] WITH CHECK ADD CONSTRAINT [FK_Receipt_ApprovedBy] FOREIGN KEY([approved_by]) REFERENCES [dbo].[Users] ([id]);
    PRINT 'Added approved_by to Inventory_Receipts';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Receipts]') AND name = 'approved_at')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipts] ADD [approved_at] [datetime2](7) NULL;
    PRINT 'Added approved_at to Inventory_Receipts';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Receipts]') AND name = 'rejected_reason')
BEGIN
    ALTER TABLE [dbo].[Inventory_Receipts] ADD [rejected_reason] [nvarchar](500) NULL;
    PRINT 'Added rejected_reason to Inventory_Receipts';
END
GO

-- 4. Order_Returns: refund tracking and processing
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'processed_at')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [processed_at] [datetime2](7) NULL;
    PRINT 'Added processed_at to Order_Returns';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'refund_method')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [refund_method] [varchar](50) NULL;
    PRINT 'Added refund_method to Order_Returns';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'refund_reference')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [refund_reference] [varchar](100) NULL;
    PRINT 'Added refund_reference to Order_Returns';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'refund_status')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [refund_status] [varchar](50) NULL;
    PRINT 'Added refund_status to Order_Returns';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Order_Returns]') AND name = 'rejection_reason')
BEGIN
    ALTER TABLE [dbo].[Order_Returns] ADD [rejection_reason] [nvarchar](500) NULL;
    PRINT 'Added rejection_reason to Order_Returns';
END
GO

-- 5. Reviews: admin response and status
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Reviews]') AND name = 'status')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD [status] [varchar](30) NOT NULL CONSTRAINT DF_Reviews_Status DEFAULT 'PENDING';
    PRINT 'Added status to Reviews';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Reviews]') AND name = 'admin_reply')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD [admin_reply] [nvarchar](1000) NULL;
    PRINT 'Added admin_reply to Reviews';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Reviews]') AND name = 'admin_replied_at')
BEGIN
    ALTER TABLE [dbo].[Reviews] ADD [admin_replied_at] [datetime2](7) NULL;
    PRINT 'Added admin_replied_at to Reviews';
END
GO

-- 6. Table: Inventory_Movements
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Inventory_Movements]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Inventory_Movements](
        [id] [bigint] IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [variant_id] [bigint] NOT NULL,
        [movement_type] [varchar](30) NOT NULL, -- IMPORT, ORDER_RESERVE, ORDER_CONSUME, ORDER_RELEASE, RETURN_RESTOCK, MANUAL_ADJUST
        [quantity] [int] NOT NULL,
        [before_quantity] [int] NULL,
        [after_quantity] [int] NULL,
        [reference_type] [varchar](30) NULL, -- INVENTORY_RECEIPT, ORDER, ORDER_RETURN, MANUAL
        [reference_id] [bigint] NULL,
        [created_by] [bigint] NULL,
        [note] [nvarchar](500) NULL,
        [created_at] [datetime2](7) NOT NULL DEFAULT GETDATE(),
        CONSTRAINT [FK_InvMovement_Variant] FOREIGN KEY([variant_id]) REFERENCES [dbo].[Product_Variants] ([id]),
        CONSTRAINT [FK_InvMovement_User] FOREIGN KEY([created_by]) REFERENCES [dbo].[Users] ([id])
    );
    PRINT 'Created Table Inventory_Movements';
END
GO

-- 7. Table: Promotion_Usages (per-user & per-order usage tracking)
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Promotion_Usages]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Promotion_Usages](
        [id] [bigint] IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [promotion_id] [bigint] NOT NULL,
        [user_id] [bigint] NULL,
        [order_id] [bigint] NOT NULL,
        [discount_amount] [decimal](18, 2) NOT NULL DEFAULT 0,
        [status] [nvarchar](30) NOT NULL DEFAULT 'RESERVED',
        [used_at] [datetime2](7) NOT NULL DEFAULT GETDATE(),
        CONSTRAINT [FK_PromoUsage_Promotion] FOREIGN KEY([promotion_id]) REFERENCES [dbo].[Promotions] ([id]),
        CONSTRAINT [FK_PromoUsage_Order] FOREIGN KEY([order_id]) REFERENCES [dbo].[Orders] ([id])
    );
    PRINT 'Created Table Promotion_Usages';
END
GO

-- 8. Table: Wishlists & Wishlist_Items
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Wishlists]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Wishlists](
        [id] [bigint] IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [user_id] [bigint] NOT NULL UNIQUE,
        [created_at] [datetime2](7) NOT NULL DEFAULT GETDATE(),
        CONSTRAINT [FK_Wishlists_User] FOREIGN KEY([user_id]) REFERENCES [dbo].[Users] ([id]) ON DELETE CASCADE
    );
    PRINT 'Created Table Wishlists';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Wishlist_Items]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Wishlist_Items](
        [id] [bigint] IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [wishlist_id] [bigint] NOT NULL,
        [product_id] [bigint] NOT NULL,
        [created_at] [datetime2](7) NOT NULL DEFAULT GETDATE(),
        CONSTRAINT [FK_WishlistItems_Wishlist] FOREIGN KEY([wishlist_id]) REFERENCES [dbo].[Wishlists] ([id]) ON DELETE CASCADE,
        CONSTRAINT [FK_WishlistItems_Product] FOREIGN KEY([product_id]) REFERENCES [dbo].[Products] ([id])
    );
    PRINT 'Created Table Wishlist_Items';
END
GO

-- 9. Table: Password_Reset_Tokens
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Password_Reset_Tokens]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Password_Reset_Tokens](
        [id] [bigint] IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [user_id] [bigint] NOT NULL,
        [token] [nvarchar](100) NOT NULL UNIQUE,
        [expiry_date] [datetime2](7) NOT NULL,
        [is_used] [bit] NOT NULL DEFAULT 0,
        [created_at] [datetime2](7) NOT NULL DEFAULT GETDATE(),
        CONSTRAINT [FK_PwdReset_User] FOREIGN KEY([user_id]) REFERENCES [dbo].[Users] ([id]) ON DELETE CASCADE
    );
    PRINT 'Created Table Password_Reset_Tokens';
END
GO

-- 10. Fix Carts table unique constraints for Guest sessions (Filtered Unique Indexes)
-- Drop existing non-filtered unique constraint on Carts.user_id if present
DECLARE @constraintName NVARCHAR(200);
SELECT @constraintName = name FROM sys.key_constraints 
WHERE parent_object_id = OBJECT_ID(N'[dbo].[Carts]') AND type = 'UQ';

IF @constraintName IS NOT NULL
BEGIN
    EXEC('ALTER TABLE [dbo].[Carts] DROP CONSTRAINT [' + @constraintName + ']');
    PRINT 'Dropped legacy unique constraint ' + @constraintName + ' on Carts';
END
GO

-- Create Filtered Unique Indexes so multiple guest carts with user_id NULL can coexist safely
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_Carts_UserId_Filtered' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_Carts_UserId_Filtered] ON [dbo].[Carts]([user_id]) 
    WHERE [user_id] IS NOT NULL;
    PRINT 'Created filtered unique index UQ_Carts_UserId_Filtered';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_Carts_SessionId_Filtered' AND object_id = OBJECT_ID(N'[dbo].[Carts]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_Carts_SessionId_Filtered] ON [dbo].[Carts]([session_id]) 
    WHERE [session_id] IS NOT NULL;
    PRINT 'Created filtered unique index UQ_Carts_SessionId_Filtered';
END
GO

-- 11. Backfill cost_price_snapshot on existing Order_Items if empty
UPDATE oi
SET oi.cost_price_snapshot = pv.import_price
FROM [dbo].[Order_Items] oi
INNER JOIN [dbo].[Product_Variants] pv ON oi.variant_id = pv.id
WHERE oi.cost_price_snapshot IS NULL;
PRINT 'Backfilled historical cost_price_snapshot on existing order items';
GO

PRINT '=========================================================';
PRINT 'MIGRATION COMPLETED SUCCESSFULLY FOR COOLMATE D2C ENGINE';
PRINT '=========================================================';

-- Unique filtered indexes for idempotency & concurrency
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_CoolCash_IdempotencyKey' AND object_id = OBJECT_ID(N'[dbo].[CoolCash_Transactions]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_CoolCash_IdempotencyKey] ON [dbo].[CoolCash_Transactions]([idempotency_key]) 
    WHERE [idempotency_key] IS NOT NULL;
    PRINT 'Created unique index UQ_CoolCash_IdempotencyKey';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UQ_PromotionUsage_Order_Promo' AND object_id = OBJECT_ID(N'[dbo].[Promotion_Usages]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_PromotionUsage_Order_Promo]
    ON [dbo].[Promotion_Usages]([order_id], [promotion_id])
    WHERE [order_id] IS NOT NULL;
    PRINT 'Created unique index UQ_PromotionUsage_Order_Promo';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UQ_InvMovements_BusinessKey' AND object_id = OBJECT_ID(N'[dbo].[Inventory_Movements]'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX [UQ_InvMovements_BusinessKey]
    ON [dbo].[Inventory_Movements] ([reference_type], [reference_id], [variant_id], [movement_type])
    WHERE [reference_type] IS NOT NULL AND [reference_id] IS NOT NULL;
    PRINT 'Created unique index UQ_InvMovements_BusinessKey';
END
GO


-- -------------------------------------------------------------
-- CHECK CONSTRAINTS FOR DATA INTEGRITY
-- -------------------------------------------------------------
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

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Orders_Amounts')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD CONSTRAINT [CK_Orders_Amounts] CHECK (
        [subtotal_amount] >= 0 AND [final_amount] >= 0 AND [shipping_fee] >= 0 AND [coolcash_used] >= 0
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Orders_RefundAmount')
BEGIN
    ALTER TABLE [dbo].[Orders] ADD CONSTRAINT [CK_Orders_RefundAmount] CHECK ([refund_amount] IS NULL OR [refund_amount] >= 0);
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
