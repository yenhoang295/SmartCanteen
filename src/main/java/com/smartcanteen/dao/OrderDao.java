package com.smartcanteen.dao;

import com.smartcanteen.db.DBConnection;
import com.smartcanteen.util.CodeUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

public class OrderDao {

    private final MenuDao menuDao = new MenuDao();
    private final IngredientDao ingredientDao = new IngredientDao();

    /**
     * Tạo đơn hàng mới. items = [{itemId, quantity, note}]
     * Trả về JSON đơn hàng vừa tạo, gồm orderCode và pickupCode.
     */
    public JSONObject createOrder(int userId, String role, String studentId, String phone,
                                   JSONArray items, String slotStart, String slotEnd,
                                   String paymentMethod) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                BigDecimal total = BigDecimal.ZERO;
                // Tính tổng tiền dựa trên giá hiện tại của món
                JSONArray resolvedItems = new JSONArray();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject it = items.getJSONObject(i);
                    int itemId = it.getInt("itemId");
                    int qty = it.getInt("quantity");
                    JSONObject menuItem = menuDao.findById(itemId);
                    if (menuItem == null || !menuItem.getBoolean("isAvailable")) {
                        throw new RuntimeException("Món ID " + itemId + " hiện không khả dụng");
                    }
                    BigDecimal price = menuItem.getBigDecimal("price");
                    total = total.add(price.multiply(BigDecimal.valueOf(qty)));
                    JSONObject resolved = new JSONObject(it.toString());
                    resolved.put("unitPrice", price);
                    resolvedItems.put(resolved);
                }

                String orderCode = CodeUtil.generateOrderCode();
                String pickupCode = CodeUtil.generatePickupCode(role, studentId, phone);

                String insertOrder = "INSERT INTO Orders (OrderCode, PickupCode, UserId, Status, SlotStart, SlotEnd, " +
                        "TotalAmount, PaymentMethod, PaymentStatus) VALUES (?,?,?,?,?,?,?,?,?)";
                int orderId;
                try (PreparedStatement ps = c.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, orderCode);
                    ps.setString(2, pickupCode);
                    ps.setInt(3, userId);
                    ps.setString(4, "PENDING");
                    ps.setString(5, slotStart);
                    ps.setString(6, slotEnd);
                    ps.setBigDecimal(7, total);
                    ps.setString(8, paymentMethod);
                    ps.setString(9, "UNPAID");
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getInt(1);
                    }
                }

                String insertItem = "INSERT INTO OrderItems (OrderId, ItemId, Quantity, Note, UnitPrice) VALUES (?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(insertItem)) {
                    for (int i = 0; i < resolvedItems.length(); i++) {
                        JSONObject it = resolvedItems.getJSONObject(i);
                        ps.setInt(1, orderId);
                        ps.setInt(2, it.getInt("itemId"));
                        ps.setInt(3, it.getInt("quantity"));
                        ps.setString(4, it.optString("note", null));
                        ps.setBigDecimal(5, it.getBigDecimal("unitPrice"));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                c.commit();
                return getOrderById(orderId);
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public JSONObject getOrderById(int orderId) throws Exception {
        String sql = "SELECT o.*, u.FullName, u.Role FROM Orders o JOIN Users u ON u.UserId=o.UserId WHERE o.OrderId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                JSONObject o = orderRowToJson(rs);
                o.put("items", getOrderItems(c, orderId));
                return o;
            }
        }
    }

    public JSONObject getOrderByCode(String orderCode) throws Exception {
        String sql = "SELECT OrderId FROM Orders WHERE OrderCode=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return getOrderById(rs.getInt("OrderId"));
            }
        }
    }

    private JSONObject orderRowToJson(ResultSet rs) throws SQLException {
        JSONObject o = new JSONObject();
        o.put("orderId", rs.getInt("OrderId"));
        o.put("orderCode", rs.getString("OrderCode"));
        o.put("pickupCode", rs.getString("PickupCode"));
        o.put("userId", rs.getInt("UserId"));
        o.put("customerName", rs.getString("FullName"));
        o.put("customerRole", rs.getString("Role"));
        o.put("status", rs.getString("Status"));
        o.put("rejectReason", rs.getString("RejectReason"));
        o.put("slotStart", rs.getString("SlotStart"));
        o.put("slotEnd", rs.getString("SlotEnd"));
        o.put("totalAmount", rs.getBigDecimal("TotalAmount"));
        o.put("paymentMethod", rs.getString("PaymentMethod"));
        o.put("paymentStatus", rs.getString("PaymentStatus"));
        o.put("createdAt", rs.getTimestamp("CreatedAt").toString());
        return o;
    }

    private JSONArray getOrderItems(Connection c, int orderId) throws SQLException {
        String sql = "SELECT oi.*, m.Name AS ItemName FROM OrderItems oi " +
                "JOIN MenuItems m ON m.ItemId = oi.ItemId WHERE oi.OrderId=?";
        JSONArray arr = new JSONArray();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject it = new JSONObject();
                    it.put("orderItemId", rs.getInt("OrderItemId"));
                    it.put("itemId", rs.getInt("ItemId"));
                    it.put("itemName", rs.getString("ItemName"));
                    it.put("quantity", rs.getInt("Quantity"));
                    it.put("note", rs.getString("Note"));
                    it.put("unitPrice", rs.getBigDecimal("UnitPrice"));
                    arr.put(it);
                }
            }
        }
        return arr;
    }

    public JSONArray listByUser(int userId) throws Exception {
        String sql = "SELECT OrderId FROM Orders WHERE UserId=? ORDER BY OrderId DESC";
        JSONArray arr = new JSONArray();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) arr.put(getOrderById(rs.getInt("OrderId")));
            }
        }
        return arr;
    }

    public JSONArray listByStatus(String status) throws Exception {
        String sql = "SELECT OrderId FROM Orders WHERE Status=? ORDER BY OrderId";
        JSONArray arr = new JSONArray();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) arr.put(getOrderById(rs.getInt("OrderId")));
            }
        }
        return arr;
    }

    /** Kiểm tra nguyên liệu có đủ cho toàn bộ đơn hàng không */
    public boolean checkIngredientsEnough(int orderId) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            String sql = "SELECT ItemId, Quantity FROM OrderItems WHERE OrderId=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (!menuDao.hasEnoughIngredients(c, rs.getInt("ItemId"), rs.getInt("Quantity"))) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /** Nhân viên nhận đơn: trừ nguyên liệu + chuyển trạng thái CONFIRMED */
    public void acceptOrder(int orderId) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String sql = "SELECT ItemId, Quantity FROM OrderItems WHERE OrderId=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ingredientDao.deductForOrderItem(c, rs.getInt("ItemId"), rs.getInt("Quantity"));
                        }
                    }
                }
                updateStatus(c, orderId, "CONFIRMED", null);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void rejectOrder(int orderId, String reason) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            updateStatus(c, orderId, "REJECTED", reason);
        }
    }

    public void updateStatus(int orderId, String status) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            updateStatus(c, orderId, status, null);
        }
    }

    private void updateStatus(Connection c, int orderId, String status, String reason) throws SQLException {
        String sql = "UPDATE Orders SET Status=?, RejectReason=? WHERE OrderId=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setInt(3, orderId);
            ps.executeUpdate();
        }
    }

    /** Hủy đơn: chỉ trong 5 phút kể từ lúc đặt. Trả về false nếu quá hạn. */
    public boolean cancelIfWithinLimit(int orderId) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            String sql = "SELECT CreatedAt, Status FROM Orders WHERE OrderId=?";
            LocalDateTime createdAt;
            String status;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    createdAt = rs.getTimestamp("CreatedAt").toLocalDateTime();
                    status = rs.getString("Status");
                }
            }
            if (!status.equals("PENDING")) return false;
            if (LocalDateTime.now().isAfter(createdAt.plusMinutes(5))) return false;
            updateStatus(c, orderId, "CANCELLED", null);
            return true;
        }
    }

    /** Nhân viên nhập mã nhận hàng khi khách đến lấy món */
    public JSONObject verifyPickupCode(String pickupCode) throws Exception {
        String sql = "SELECT OrderId FROM Orders WHERE PickupCode=? AND Status IN ('READY','PREPARING','CONFIRMED') ORDER BY OrderId DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pickupCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int orderId = rs.getInt("OrderId");
                updateStatus(c, orderId, "COMPLETED", null);
                return getOrderById(orderId);
            }
        }
    }

    public void markPaid(int orderId) throws Exception {
        String sql = "UPDATE Orders SET PaymentStatus='PAID' WHERE OrderId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}
