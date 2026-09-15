-- ============================================
-- SMART CANTEEN - DATABASE SCHEMA (SQL Server)
-- Chạy file này trong SSMS hoặc Azure Data Studio
-- ============================================

CREATE DATABASE SmartCanteen;
GO
USE SmartCanteen;
GO

-- ---------- USERS ----------
CREATE TABLE Users (
    UserId          INT IDENTITY(1,1) PRIMARY KEY,
    Role            VARCHAR(20) NOT NULL CHECK (Role IN ('STUDENT','LECTURER','GUEST','STAFF','ADMIN')),
    FullName        NVARCHAR(150) NULL,
    Email           VARCHAR(150) NULL,
    Phone           VARCHAR(20) NULL,
    StudentId       VARCHAR(20) NULL,
    ClassName       NVARCHAR(50) NULL,
    PasswordHash    VARCHAR(255) NULL,
    OtpCode         VARCHAR(10) NULL,
    OtpExpireAt     DATETIME NULL,
    LoyaltyPoints   INT NOT NULL DEFAULT 0,
    IsActive        BIT NOT NULL DEFAULT 1,
    CreatedAt       DATETIME NOT NULL DEFAULT GETDATE()
);
GO

-- ---------- CATEGORIES ----------
CREATE TABLE Categories (
    CategoryId   INT IDENTITY(1,1) PRIMARY KEY,
    Name         NVARCHAR(100) NOT NULL,
    Icon         VARCHAR(20) NULL
);
GO

-- ---------- INGREDIENTS ----------
CREATE TABLE Ingredients (
    IngredientId   INT IDENTITY(1,1) PRIMARY KEY,
    Name           NVARCHAR(100) NOT NULL,
    Quantity       DECIMAL(10,2) NOT NULL DEFAULT 0,
    Unit           VARCHAR(20) NOT NULL,
    MinQuantity    DECIMAL(10,2) NOT NULL DEFAULT 0,
    LastUpdated    DATETIME NOT NULL DEFAULT GETDATE()
);
GO

-- ---------- MENU ITEMS ----------
CREATE TABLE MenuItems (
    ItemId        INT IDENTITY(1,1) PRIMARY KEY,
    CategoryId    INT NOT NULL FOREIGN KEY REFERENCES Categories(CategoryId),
    Name          NVARCHAR(150) NOT NULL,
    Price         DECIMAL(10,2) NOT NULL,
    Description   NVARCHAR(500) NULL,
    ImageUrl      VARCHAR(300) NULL,
    IsAvailable   BIT NOT NULL DEFAULT 1
);
GO

-- ---------- MENU ITEM <-> INGREDIENT (recipe) ----------
CREATE TABLE MenuItemIngredients (
    ItemId          INT NOT NULL FOREIGN KEY REFERENCES MenuItems(ItemId),
    IngredientId    INT NOT NULL FOREIGN KEY REFERENCES Ingredients(IngredientId),
    QuantityNeeded  DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (ItemId, IngredientId)
);
GO

-- ---------- PROMOTIONS / COMBOS ----------
CREATE TABLE Promotions (
    PromotionId     INT IDENTITY(1,1) PRIMARY KEY,
    Name            NVARCHAR(150) NOT NULL,
    Type            VARCHAR(20) NOT NULL CHECK (Type IN ('DISCOUNT','COMBO')),
    DiscountPercent DECIMAL(5,2) NULL,
    ComboPrice      DECIMAL(10,2) NULL,
    StartDate       DATETIME NOT NULL,
    EndDate         DATETIME NOT NULL,
    IsActive        BIT NOT NULL DEFAULT 1
);
GO

CREATE TABLE PromotionItems (
    PromotionId  INT NOT NULL FOREIGN KEY REFERENCES Promotions(PromotionId),
    ItemId       INT NOT NULL FOREIGN KEY REFERENCES MenuItems(ItemId),
    PRIMARY KEY (PromotionId, ItemId)
);
GO

-- ---------- ORDERS ----------
CREATE TABLE Orders (
    OrderId         INT IDENTITY(1,1) PRIMARY KEY,
    OrderCode       VARCHAR(20) NOT NULL UNIQUE,
    PickupCode      VARCHAR(10) NOT NULL,
    UserId          INT NOT NULL FOREIGN KEY REFERENCES Users(UserId),
    Status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (Status IN ('PENDING','CONFIRMED','PREPARING','READY','COMPLETED','REJECTED','CANCELLED')),
    RejectReason    NVARCHAR(300) NULL,
    SlotStart       VARCHAR(10) NOT NULL,
    SlotEnd         VARCHAR(10) NOT NULL,
    TotalAmount     DECIMAL(10,2) NOT NULL,
    PaymentMethod   VARCHAR(20) NOT NULL CHECK (PaymentMethod IN ('MOMO','QR')),
    PaymentStatus   VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (PaymentStatus IN ('UNPAID','PAID')),
    PromotionId     INT NULL FOREIGN KEY REFERENCES Promotions(PromotionId),
    CreatedAt       DATETIME NOT NULL DEFAULT GETDATE()
);
GO

CREATE TABLE OrderItems (
    OrderItemId   INT IDENTITY(1,1) PRIMARY KEY,
    OrderId       INT NOT NULL FOREIGN KEY REFERENCES Orders(OrderId),
    ItemId        INT NOT NULL FOREIGN KEY REFERENCES MenuItems(ItemId),
    Quantity      INT NOT NULL,
    Note          NVARCHAR(200) NULL,
    UnitPrice     DECIMAL(10,2) NOT NULL
);
GO

-- ---------- RATINGS ----------
CREATE TABLE Ratings (
    RatingId      INT IDENTITY(1,1) PRIMARY KEY,
    OrderItemId   INT NOT NULL FOREIGN KEY REFERENCES OrderItems(OrderItemId),
    Stars         INT NOT NULL CHECK (Stars BETWEEN 1 AND 5),
    Comment       NVARCHAR(300) NULL,
    CreatedAt     DATETIME NOT NULL DEFAULT GETDATE()
);
GO

-- Helpful indexes
CREATE INDEX IX_Orders_UserId ON Orders(UserId);
CREATE INDEX IX_Orders_Status ON Orders(Status);
CREATE INDEX IX_MenuItems_CategoryId ON MenuItems(CategoryId);
GO
