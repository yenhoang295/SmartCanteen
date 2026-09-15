package com.smartcanteen.handler;

import com.smartcanteen.dao.UserDao;
import com.smartcanteen.util.CodeUtil;
import com.smartcanteen.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;

public class AuthHandler implements HttpHandler {

    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            if (method.equals("POST") && path.equals("/api/auth/register")) {
                register(ex);
            } else if (method.equals("POST") && path.equals("/api/auth/login")) {
                login(ex);
            } else if (method.equals("POST") && path.equals("/api/auth/guest/otp")) {
                requestOtp(ex);
            } else if (method.equals("POST") && path.equals("/api/auth/guest/verify")) {
                verifyOtp(ex);
            } else {
                HttpUtil.sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            HttpUtil.sendError(ex, 500, e.getMessage());
        }
    }

    private void register(HttpExchange ex) throws Exception {
        JSONObject body = HttpUtil.readBody(ex);
        String role = body.getString("role").toUpperCase();
        if (!role.equals("STUDENT") && !role.equals("LECTURER")) {
            HttpUtil.sendError(ex, 400, "Chỉ sinh viên hoặc giảng viên mới đăng ký bằng form này");
            return;
        }
        int userId = userDao.registerAccount(
                role,
                body.optString("fullName", null),
                body.optString("email", null),
                body.optString("phone", null),
                body.optString("studentId", null),
                body.optString("className", null),
                body.getString("password")
        );
        JSONObject res = new JSONObject().put("userId", userId).put("message", "Đăng ký thành công");
        HttpUtil.sendJson(ex, 201, res.toString());
    }

    private void login(HttpExchange ex) throws Exception {
        JSONObject body = HttpUtil.readBody(ex);
        String identifier = body.getString("identifier"); // MSSV / Email
        String password = body.getString("password");

        JSONObject user = userDao.findByLogin(identifier);
        if (user == null || user.get("passwordHash") == JSONObject.NULL
                || !CodeUtil.checkPassword(password, user.getString("passwordHash"))) {
            HttpUtil.sendError(ex, 401, "Sai thông tin đăng nhập");
            return;
        }
        user.remove("passwordHash");
        user.remove("otpCode");
        HttpUtil.sendJson(ex, 200, user.toString());
    }

    private void requestOtp(HttpExchange ex) throws Exception {
        JSONObject body = HttpUtil.readBody(ex);
        String phone = body.getString("phone");
        String otp = userDao.requestGuestOtp(phone);
        // Demo: tra ve OTP luon trong response de test (thuc te se gui SMS)
        JSONObject res = new JSONObject().put("message", "Đã gửi OTP (demo)").put("otpDemo", otp);
        HttpUtil.sendJson(ex, 200, res.toString());
    }

    private void verifyOtp(HttpExchange ex) throws Exception {
        JSONObject body = HttpUtil.readBody(ex);
        JSONObject user = userDao.verifyGuestOtp(body.getString("phone"), body.getString("otp"));
        if (user == null) {
            HttpUtil.sendError(ex, 401, "OTP không đúng hoặc đã hết hạn");
            return;
        }
        HttpUtil.sendJson(ex, 200, user.toString());
    }
}
