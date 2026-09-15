package com.smartcanteen.dao;

import com.smartcanteen.db.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;

public class ReportDao {

    public JSONObject revenueSummary() throws Exception {
        JSONObject result = new JSONObject();
        try (Connection c = DBConnection.getConnection()) {
            result.put("revenueToday", scalarDecimal(c,
                    "SELECT ISNULL(SUM(TotalAmount),0) FROM Orders WHERE Status='COMPLETED' AND CAST(CreatedAt AS DATE) = CAST(GETDATE() AS DATE)"));
            result.put("revenueThisWeek", scalarDecimal(c,
                    "SELECT ISNULL(SUM(TotalAmount),0) FROM Orders WHERE Status='COMPLETED' AND DATEDIFF(day, CreatedAt, GETDATE()) <= 7"));
            result.put("totalOrders", scalarInt(c, "SELECT COUNT(*) FROM Orders"));
            result.put("completedOrders", scalarInt(c, "SELECT COUNT(*) FROM Orders WHERE Status='COMPLETED'"));
            result.put("rejectedOrders", scalarInt(c, "SELECT COUNT(*) FROM Orders WHERE Status='REJECTED'"));
            result.put("cancelledOrders", scalarInt(c, "SELECT COUNT(*) FROM Orders WHERE Status='CANCELLED'"));
        }
        return result;
    }

    public JSONArray topSellingItems(int limit) throws Exception {
        String sql = "SELECT TOP (?) m.Name, SUM(oi.Quantity) AS SoldQty FROM OrderItems oi " +
                "JOIN Orders o ON o.OrderId = oi.OrderId JOIN MenuItems m ON m.ItemId = oi.ItemId " +
                "WHERE o.Status = 'COMPLETED' GROUP BY m.Name ORDER BY SoldQty DESC";
        JSONArray arr = new JSONArray();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject o = new JSONObject();
                    o.put("name", rs.getString("Name"));
                    o.put("soldQuantity", rs.getInt("SoldQty"));
                    arr.put(o);
                }
            }
        }
        return arr;
    }

    private java.math.BigDecimal scalarDecimal(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }

    private int scalarInt(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
