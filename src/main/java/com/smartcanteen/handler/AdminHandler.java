package com.smartcanteen.handler;

import com.smartcanteen.dao.IngredientDao;
import com.smartcanteen.dao.MenuDao;
import com.smartcanteen.dao.ReportDao;
import com.smartcanteen.dao.UserDao;
import com.smartcanteen.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;

public class AdminHandler implements HttpHandler {

    private final MenuDao menuDao = new MenuDao();
    private final IngredientDao ingredientDao = new IngredientDao();
    private final UserDao userDao = new UserDao();
    private final ReportDao reportDao = new ReportDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        try {
            // ---- Menu CRUD ----
            if (method.equals("POST") && path.equals("/api/admin/menu")) {
                JSONObject body = HttpUtil.readBody(ex);
                int id = menuDao.create(body);
                HttpUtil.sendJson(ex, 201, new JSONObject().put("itemId", id).toString());
            } else if (method.equals("PUT") && path.matches("/api/admin/menu/\\d+")) {
                int id = idFromPath(path);
                menuDao.update(id, HttpUtil.readBody(ex));
                HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã cập nhật món").toString());
            } else if (method.equals("DELETE") && path.matches("/api/admin/menu/\\d+")) {
                menuDao.delete(idFromPath(path));
                HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã xóa món").toString());
            } else if (method.equals("POST") && path.matches("/api/admin/menu/\\d+/toggle")) {
                JSONObject body = HttpUtil.readBody(ex);
                menuDao.setAvailability(idFromPath(path, 2), body.getBoolean("isAvailable"));
                HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã cập nhật trạng thái món").toString());

            // ---- Ingredient CRUD ----
            } else if (method.equals("GET") && path.equals("/api/admin/ingredients")) {
                HttpUtil.sendJson(ex, 200, new JSONObject().put("ingredients", ingredientDao.listAll()).toString());
            } else if (method.equals("POST") && path.equals("/api/admin/ingredients")) {
                int id = ingredientDao.create(HttpUtil.readBody(ex));
                HttpUtil.sendJson(ex, 201, new JSONObject().put("ingredientId", id).toString());
            } else if (method.equals("PUT") && path.matches("/api/admin/ingredients/\\d+/quantity")) {
                JSONObject body = HttpUtil.readBody(ex);
                ingredientDao.updateQuantity(idFromPath(path, 2), body.getDouble("quantity"));
                HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã cập nhật tồn kho").toString());
            } else if (method.equals("PUT") && path.matches("/api/admin/ingredients/\\d+/min")) {
                JSONObject body = HttpUtil.readBody(ex);
                ingredientDao.setMinQuantity(idFromPath(path, 2), body.getDouble("minQuantity"));
                HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã cập nhật mức tối thiểu").toString());
            } else if (method.equals("GET") && path.equals("/api/admin/ingredients/low-stock")) {
                HttpUtil.sendJson(ex, 200, new JSONObject().put("ingredients", ingredientDao.listLowStock()).toString());

            // ---- Users ----
            } else if (method.equals("GET") && path.equals("/api/admin/users")) {
                HttpUtil.sendJson(ex, 200, userDao.listAll().toString());
            } else if (method.equals("POST") && path.matches("/api/admin/users/\\d+/status")) {
                JSONObject body = HttpUtil.readBody(ex);
                userDao.setActive(idFromPath(path, 2), body.getBoolean("active"));
                HttpUtil.sendJson(ex, 200, new JSONObject().put("message", "Đã cập nhật tài khoản").toString());

            // ---- Reports ----
            } else if (method.equals("GET") && path.equals("/api/admin/reports/revenue")) {
                HttpUtil.sendJson(ex, 200, reportDao.revenueSummary().toString());
            } else if (method.equals("GET") && path.equals("/api/admin/reports/top-items")) {
                HttpUtil.sendJson(ex, 200, new JSONObject().put("items", reportDao.topSellingItems(5)).toString());
            } else {
                HttpUtil.sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            try { HttpUtil.sendError(ex, 500, e.getMessage()); } catch (IOException ignored) {}
        }
    }

    private int idFromPath(String path) { return idFromPath(path, 1); }

    private int idFromPath(String path, int segmentFromEnd) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - segmentFromEnd]);
    }
}
