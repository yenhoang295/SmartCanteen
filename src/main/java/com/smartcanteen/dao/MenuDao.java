package com.smartcanteen.dao;

import com.smartcanteen.db.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;

public class MenuDao {

    private JSONObject rowToJson(ResultSet rs) throws SQLException {
        JSONObject o = new JSONObject();
        o.put("itemId", rs.getInt("ItemId"));
        o.put("categoryId", rs.getInt("CategoryId"));
        o.put("name", rs.getString("Name"));
        o.put("price", rs.getBigDecimal("Price"));
        o.put("description", rs.getString("Description"));
        o.put("imageUrl", rs.getString("ImageUrl"));
        o.put("isAvailable", rs.getBoolean("IsAvailable"));
        return o;
    }

    public JSONArray listAll(String keyword, Integer categoryId) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT * FROM MenuItems WHERE 1=1");
        if (keyword != null && !keyword.isBlank()) sql.append(" AND Name LIKE ?");
        if (categoryId != null) sql.append(" AND CategoryId = ?");
        sql.append(" ORDER BY ItemId");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isBlank()) ps.setString(idx++, "%" + keyword + "%");
            if (categoryId != null) ps.setInt(idx++, categoryId);
            JSONArray arr = new JSONArray();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) arr.put(rowToJson(rs));
            }
            return arr;
        }
    }

    public JSONObject findById(int itemId) throws Exception {
        String sql = "SELECT * FROM MenuItems WHERE ItemId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                JSONObject item = rowToJson(rs);
                item.put("ingredients", getRecipe(c, itemId));
                return item;
            }
        }
    }

    private JSONArray getRecipe(Connection c, int itemId) throws SQLException {
        String sql = "SELECT i.Name, mii.QuantityNeeded, i.Unit FROM MenuItemIngredients mii " +
                "JOIN Ingredients i ON i.IngredientId = mii.IngredientId WHERE mii.ItemId=?";
        JSONArray arr = new JSONArray();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject ing = new JSONObject();
                    ing.put("name", rs.getString("Name"));
                    ing.put("quantityNeeded", rs.getBigDecimal("QuantityNeeded"));
                    ing.put("unit", rs.getString("Unit"));
                    arr.put(ing);
                }
            }
        }
        return arr;
    }

    public JSONArray listCategories() throws Exception {
        String sql = "SELECT * FROM Categories ORDER BY CategoryId";
        JSONArray arr = new JSONArray();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject o = new JSONObject();
                o.put("categoryId", rs.getInt("CategoryId"));
                o.put("name", rs.getString("Name"));
                o.put("icon", rs.getString("Icon"));
                arr.put(o);
            }
        }
        return arr;
    }

    public int create(JSONObject item) throws Exception {
        String sql = "INSERT INTO MenuItems (CategoryId, Name, Price, Description, ImageUrl, IsAvailable) VALUES (?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getInt("categoryId"));
            ps.setString(2, item.getString("name"));
            ps.setBigDecimal(3, item.getBigDecimal("price"));
            ps.setString(4, item.optString("description", null));
            ps.setString(5, item.optString("imageUrl", null));
            ps.setBoolean(6, item.optBoolean("isAvailable", true));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void update(int itemId, JSONObject item) throws Exception {
        String sql = "UPDATE MenuItems SET CategoryId=?, Name=?, Price=?, Description=?, ImageUrl=?, IsAvailable=? WHERE ItemId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, item.getInt("categoryId"));
            ps.setString(2, item.getString("name"));
            ps.setBigDecimal(3, item.getBigDecimal("price"));
            ps.setString(4, item.optString("description", null));
            ps.setString(5, item.optString("imageUrl", null));
            ps.setBoolean(6, item.optBoolean("isAvailable", true));
            ps.setInt(7, itemId);
            ps.executeUpdate();
        }
    }

    public void delete(int itemId) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM MenuItemIngredients WHERE ItemId=?")) {
                ps.setInt(1, itemId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM MenuItems WHERE ItemId=?")) {
                ps.setInt(1, itemId);
                ps.executeUpdate();
            }
        }
    }

    public void setAvailability(int itemId, boolean available) throws Exception {
        String sql = "UPDATE MenuItems SET IsAvailable=? WHERE ItemId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, available);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        }
    }

    /** Kiểm tra nguyên liệu có đủ để làm 'quantity' phần món này không */
    public boolean hasEnoughIngredients(Connection c, int itemId, int quantity) throws SQLException {
        String sql = "SELECT i.Quantity, mii.QuantityNeeded FROM MenuItemIngredients mii " +
                "JOIN Ingredients i ON i.IngredientId = mii.IngredientId WHERE mii.ItemId=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double have = rs.getDouble("Quantity");
                    double need = rs.getDouble("QuantityNeeded") * quantity;
                    if (have < need) return false;
                }
            }
        }
        return true;
    }
}
