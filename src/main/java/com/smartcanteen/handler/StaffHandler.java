package com.smartcanteen.handler;

import com.smartcanteen.dao.OrderDao;
import com.smartcanteen.dao.UserDao;
import com.smartcanteen.util.CodeUtil;
import com.smartcanteen.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class StaffHandler implements HttpHandler {

    private final OrderDao orderDao = new OrderDao();
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            if (method.equals("POST") && path.equals("/api/staff/login")) {
                login(ex);
            } else if (method.equals("GET") && path.equals("/api/staff/orders")) {
                listOrders(ex);
            } else if (method.equals("GET") && path.matches("/api/staff/orders/\\d+/check")) {
                checkIngredients(ex, idFromPath(path, 2));
            } else if (method.equals("POST") && path.matches("/api/staff/orders/\\d+/accept")) {
                acceptOrder(ex, idFromPath(path, 2));
            } else if (method.equals("POST") && path.matches("/api/staff/orders/\\d+/reject")) {
                rejectOrder(ex, idFromPath(path, 2));
            } else if (method.equals("POST") && path.matches("/api/staff/orders/\\d+/status")) {
                updateStatus(ex, idFromPath(path, 2));
            } else if (method.equals("POST") && path.equals("/api/staff/verify-pickup")) {
                verifyPickup(ex);
            } else {
                HttpUtil.sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            HttpUtil.sendError(ex, 500, e.getMessage());
        }
    }

    private int idFromPath(String path, int segmentFromEnd) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - segmentFromEnd]);
    }

    private void login(HttpExchange ex) throws Exception {
        JSONObject body = HttpUtil.readBody(ex);
        JSONObject user = userDao.findByLogin(body.getString("identifier"));
        if (user == null || !user.getString("role").equals("STAFF")) {
            HttpUtil.sendError(ex, 401, "Không phải tài khoản nhân viên");
            return;
        }
        HttpUtil.sendJson(ex, 200, user.toString());
    }

    private void listOrders(HttpExchange ex) throws Exception {
        Map<String, String> params = HttpUtil.queryParams(ex);
        String status = params.getOrDefault("status", "PENDING");
        JSONObject res = new JSONObject().put("orders", orderDao.listByStatus(status));
        HttpUtil.sendJson(ex, 200, res.toString());
    }

    private void checkIngredients(HttpExchange ex, int orderId) throws Exception {
        boolean enough = orderDao.checkIngredientsEnough(orderId);
        HttpUtil.sendJson(ex, 200, new JSONObject().put("enough", enough).toString());
    }

    private void acceptOrder(HttpExchange ex, int orderId) throws Exception {
        if (!orderDao.checkIngredientsEnough(orderId)) {
            HttpUtil.sendError(ex, 400, "Không đủ nguyên liệu, không thể nhận đơn");
            return;
        }
        orderDao.acceptOrder(orderId);
        HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã nhận đơn").toString());
    }

    private void rejectOrder(HttpExchange ex, int orderId) throws Exception {
        JSONObject body = HttpUtil.readBody(ex);
        String reason = body.optString("reason", "Không đủ nguyên liệu");
        orderDao.rejectOrder(orderId, reason);
        HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã từ chối đơn").toString());
    }

    private void updateStatus(HttpExchange ex, int orderId) throws Exception {
        JSONObject body = HttpUtil.readBody(ex);
        String status = body.getString("status"); // PREPARING, READY
        orderDao.updateStatus(orderId, status);
        HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã cập nhật trạng thái").toString());
    }

    private void verifyPickup(HttpExchange ex) throws Exception {
        JSONObject body = HttpUtil.readBody(ex);
        JSONObject order = orderDao.verifyPickupCode(body.getString("pickupCode"));
        if (order == null) {
            HttpUtil.sendError(ex, 404, "Mã nhận hàng không hợp lệ");
            return;
        }
        HttpUtil.sendJson(ex, 200, order.toString());
    }
}
