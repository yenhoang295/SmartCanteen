package com.smartcanteen.dao;

import com.smartcanteen.db.DBConnection;
import com.smartcanteen.util.CodeUtil;
import org.json.JSONObject;

import java.sql.*;
import java.time.LocalDateTime;

public class UserDao {

    public JSONObject rowToJson(ResultSet rs) throws SQLException {
        JSONObject o = new JSONObject();
        o.put("userId", rs.getInt("UserId"));
        o.put("role", rs.getString("Role"));
        o.put("fullName", rs.getString("FullName"));
        o.put("email", rs.getString("Email"));
        o.put("phone", rs.getString("Phone"));
        o.put("studentId", rs.getString("StudentId"));
        o.put("className", rs.getString("ClassName"));
        o.put("loyaltyPoints", rs.getInt("LoyaltyPoints"));
        return o;
    }

    public JSONObject findByLogin(String identifier) throws Exception {
        String sql = "SELECT * FROM Users WHERE Email = ? OR StudentId = ? OR Phone = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            ps.setString(3, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rowToJsonWithHash(rs);
                return null;
            }
        }
    }

    private JSONObject rowToJsonWithHash(ResultSet rs) throws SQLException {
        JSONObject o = rowToJson(rs);
        o.put("passwordHash", rs.getString("PasswordHash"));
        o.put("otpCode", rs.getString("OtpCode"));
        return o;
    }

    public int registerAccount(String role, String fullName, String email, String phone,
                                String studentId, String className, String rawPassword) throws Exception {
        String sql = "INSERT INTO Users (Role, FullName, Email, Phone, StudentId, ClassName, PasswordHash) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, role);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setString(5, studentId);
            ps.setString(6, className);
            ps.setString(7, rawPassword == null ? null : CodeUtil.hashPassword(rawPassword));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Tạo hoặc lấy tài khoản khách vãng lai theo số điện thoại, rồi gửi OTP */
    public String requestGuestOtp(String phone) throws Exception {
        String otp = CodeUtil.generateOtp();
        try (Connection c = DBConnection.getConnection()) {
            String find = "SELECT UserId FROM Users WHERE Phone = ? AND Role = 'GUEST'";
            try (PreparedStatement ps = c.prepareStatement(find)) {
                ps.setString(1, phone);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        updateOtp(c, rs.getInt("UserId"), otp);
                        return otp;
                    }
                }
            }
            String insert = "INSERT INTO Users (Role, Phone, OtpCode, OtpExpireAt) VALUES ('GUEST', ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(insert)) {
                ps.setString(1, phone);
                ps.setString(2, otp);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusMinutes(5)));
                ps.executeUpdate();
            }
        }
        return otp;
    }

    private void updateOtp(Connection c, int userId, String otp) throws SQLException {
        String sql = "UPDATE Users SET OtpCode=?, OtpExpireAt=? WHERE UserId=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, otp);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().plusMinutes(5)));
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public JSONObject verifyGuestOtp(String phone, String otp) throws Exception {
        String sql = "SELECT * FROM Users WHERE Phone=? AND Role='GUEST' AND OtpCode=? AND OtpExpireAt > GETDATE()";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, otp);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rowToJson(rs);
            }
        }
    }

    public JSONObject findById(int userId) throws Exception {
        String sql = "SELECT * FROM Users WHERE UserId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rowToJson(rs);
                return null;
            }
        }
    }

    public void addLoyaltyPoints(int userId, int points) throws Exception {
        String sql = "UPDATE Users SET LoyaltyPoints = LoyaltyPoints + ? WHERE UserId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public JSONObject listAll() throws Exception {
        JSONObject result = new JSONObject();
        var arr = new org.json.JSONArray();
        String sql = "SELECT * FROM Users ORDER BY UserId DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) arr.put(rowToJson(rs));
        }
        result.put("users", arr);
        return result;
    }

    public void setActive(int userId, boolean active) throws Exception {
        String sql = "UPDATE Users SET IsActive=? WHERE UserId=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }
}
