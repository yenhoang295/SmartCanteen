USE SmartCanteen;
GO

-- Admin & staff mặc định (mật khẩu demo: 123456 -> đã hash SHA-256)
-- Hash được sinh bởi PasswordUtil.hash("123456")
INSERT INTO Users (Role, FullName, Email, Phone, PasswordHash)
VALUES
('ADMIN', N'Quản trị viên', 'admin@canteen.edu.vn', '0900000000', '8D969EEF6ECAD3C29A3A629280E686CF0C3F5D5A86AFF3CA12020C923ADC6C92'),
('STAFF', N'Nhân viên canteen', 'staff@canteen.edu.vn', '0900000001', '8D969EEF6ECAD3C29A3A629280E686CF0C3F5D5A86AFF3CA12020C923ADC6C92');
GO

-- Danh mục
INSERT INTO Categories (Name, Icon) VALUES
(N'Món mặn', 'ti-soup'), (N'Món mì', 'ti-bowl'), (N'Nước uống', 'ti-glass'),
(N'Combo sáng', 'ti-sun'), (N'Combo chiều', 'ti-sunset');
GO

-- Nguyên liệu
INSERT INTO Ingredients (Name, Quantity, Unit, MinQuantity) VALUES
(N'Gạo', 20, 'kg', 5),
(N'Thịt gà', 5, 'kg', 2),
(N'Thịt bò', 4, 'kg', 2),
(N'Mì sợi', 10, 'kg', 3),
(N'Rau', 8, 'kg', 2),
(N'Trứng', 100, N'quả', 20),
(N'Nước ngọt', 50, N'lon', 10);
GO

-- Món ăn
INSERT INTO MenuItems (CategoryId, Name, Price, Description, IsAvailable) VALUES
(1, N'Cơm gà', 30000, N'Cơm, gà chiên, rau', 1),
(1, N'Cơm bò', 35000, N'Cơm, bò xào, rau', 1),
(2, N'Mì gà', 28000, N'Mì nước, gà, rau', 1),
(2, N'Mì bò', 32000, N'Mì nước, bò, rau', 1),
(3, N'Trà đào', 15000, N'Trà đào mát lạnh', 1),
(3, N'Coca', 10000, N'Nước ngọt có ga', 1);
GO

-- Công thức (recipe) cho từng món
INSERT INTO MenuItemIngredients (ItemId, IngredientId, QuantityNeeded) VALUES
(1, 1, 0.2), (1, 2, 0.15), (1, 5, 0.05),   -- Cơm gà
(2, 1, 0.2), (2, 3, 0.15), (2, 5, 0.05),   -- Cơm bò
(3, 4, 0.15), (3, 2, 0.1), (3, 5, 0.05),   -- Mì gà
(4, 4, 0.15), (4, 3, 0.1), (4, 5, 0.05),   -- Mì bò
(6, 7, 1);                                  -- Coca dùng 1 lon
GO
