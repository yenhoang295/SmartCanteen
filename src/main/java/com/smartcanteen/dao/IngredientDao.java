package com.smartcanteen.dao;

import com.smartcanteen.db.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;

public class IngredientDao {

    private JSONObject rowToJson(ResultSet rs) throws SQLException {
        JSONObject o = new JSONObject();
        o.put("ingredientId", rs.getInt("IngredientId"));
        o.put("name", rs.getString("Name"));
        o.put("quantity", rs.getBigDecimal("Quantity"));
        o.put("unit", rs.getString("Unit"));
        o.put("minQuantity", rs.getBigDecimal("MinQuantity"));
        o.put("lastUpdated", rs.getTimestamp("LastUpdated").toString());
        o.put("lowStock", rs.getBigDecimal("Quantity").compareTo(rs.getBigDecimal("MinQuantity")) <= 0);
        return o;
    }

    public JSONArray listAll() throws Exception {
        String sql = "SELECT * FROM Ingredients ORDER BY Name";
        JSONArray arr = new JSONArray();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) arr.put(rowToJson(rs));
        }
        return arr;
    }

    public int create(JSONObject ing) throws Exception {
        String sql = "INSERT INTO Ingredients (Name, Quantity, Unit, MinQuantity) VALUES (?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ing.getString("name"));
            ps.setBigDecimal(2, ing.getBigDecimal("quantity"));
            ps.setString(3, ing.getString("unit"));
            ps.setBigDecimal(4, ing.getBigDecimal("minQuantity"));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Cập nhật số lượng tồn kho (theo yêu cầu: cập nhật mỗi 1 giờ) */
    public void updateQuantity(int ingredientId, double newQuantity) throws Exception {
        String sql = "UPDATE Ingredients SET Quantity=?, LastUpdated=GETDATE() WHERE IngredientId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, newQuantity);
            ps.setInt(2, ingredientId);
            ps.executeUpdate();
        }
    }

    public void setMinQuantity(int ingredientId, double minQuantity) throws Exception {
        String sql = "UPDATE Ingredients SET MinQuantity=? WHERE IngredientId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, minQuantity);
            ps.setInt(2, ingredientId);
            ps.executeUpdate();
        }
    }

    /** Trừ nguyên liệu sau khi nhân viên nhận đơn (đã xác nhận đủ) */
    public void deductForOrderItem(Connection c, int itemId, int quantity) throws SQLException {
        String sql = "UPDATE i SET i.Quantity = i.Quantity - (mii.QuantityNeeded * ?), i.LastUpdated = GETDATE() " +
                "FROM Ingredients i JOIN MenuItemIngredients mii ON mii.IngredientId = i.IngredientId " +
                "WHERE mii.ItemId = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        }
    }

    public JSONArray listLowStock() throws Exception {
        String sql = "SELECT * FROM Ingredients WHERE Quantity <= MinQuantity ORDER BY Name";
        JSONArray arr = new JSONArray();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) arr.put(rowToJson(rs));
        }
        return arr;
    }
}
