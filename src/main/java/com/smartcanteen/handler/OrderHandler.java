package com.smartcanteen.handler;

import com.smartcanteen.dao.OrderDao;
import com.smartcanteen.dao.UserDao;
import com.smartcanteen.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderHandler implements HttpHandler {

    private final OrderDao orderDao = new OrderDao();
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            if (method.equals("POST") && path.equals("/api/orders")) {
                createOrder(ex);
            } else if (method.equals("GET") && path.equals("/api/orders")) {
                listByUser(ex);
            } else if (method.equals("GET") && path.matches("/api/orders/\\d+")) {
                getOrder(ex, idFromPath(path, 3));
            } else if (method.equals("POST") && path.matches("/api/orders/\\d+/cancel")) {
                cancelOrder(ex, idFromPath(path, 3));
            } else if (method.equals("POST") && path.matches("/api/orders/\\d+/pay")) {
                payOrder(ex, idFromPath(path, 3));
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

    private void createOrder(HttpExchange ex) throws Exception {
        JSONObject body = HttpUtil.readBody(ex);
        int userId = body.getInt("userId");
        JSONObject user = userDao.findById(userId);
        if (user == null) {
            HttpUtil.sendError(ex, 404, "Không tìm thấy người dùng");
            return;
        }
        JSONObject order = orderDao.createOrder(
                userId,
                user.getString("role"),
                user.optString("studentId", null),
                user.optString("phone", null),
                body.getJSONArray("items"),
                body.getString("slotStart"),
                body.getString("slotEnd"),
                body.getString("paymentMethod")
        );
        HttpUtil.sendJson(ex, 201, order.toString());
    }

    private void listByUser(HttpExchange ex) throws Exception {
        Map<String, String> params = HttpUtil.queryParams(ex);
        if (!params.containsKey("userId")) {
            HttpUtil.sendError(ex, 400, "Thiếu userId");
            return;
        }
        int userId = Integer.parseInt(params.get("userId"));
        JSONObject res = new JSONObject().put("orders", orderDao.listByUser(userId));
        HttpUtil.sendJson(ex, 200, res.toString());
    }

    private void getOrder(HttpExchange ex, int orderId) throws Exception {
        JSONObject order = orderDao.getOrderById(orderId);
        if (order == null) HttpUtil.sendError(ex, 404, "Không tìm thấy đơn hàng");
        else HttpUtil.sendJson(ex, 200, order.toString());
    }

    private void cancelOrder(HttpExchange ex, int orderId) throws Exception {
        boolean ok = orderDao.cancelIfWithinLimit(orderId);
        if (!ok) {
            HttpUtil.sendError(ex, 400, "Không thể hủy: đã quá 5 phút hoặc đơn không ở trạng thái chờ");
            return;
        }
        HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã hủy đơn").toString());
    }

    /** Thanh toán demo: đánh dấu đã thanh toán, không tích hợp cổng thật */
    private void payOrder(HttpExchange ex, int orderId) throws Exception {
        orderDao.markPaid(orderId);
        HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Thanh toán thành công (demo)").toString());
    }
}
