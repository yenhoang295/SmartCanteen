package com.smartcanteen.handler;

import com.smartcanteen.dao.MenuDao;
import com.smartcanteen.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class MenuHandler implements HttpHandler {

    private final MenuDao menuDao = new MenuDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (!ex.getRequestMethod().equals("GET")) {
                HttpUtil.sendError(ex, 405, "Method not allowed");
                return;
            }
            if (path.equals("/api/menu")) {
                listMenu(ex);
            } else if (path.equals("/api/menu/categories")) {
                HttpUtil.sendJson(ex, 200, new JSONObject().put("categories", menuDao.listCategories()).toString());
            } else if (path.matches("/api/menu/\\d+")) {
                int itemId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                JSONObject item = menuDao.findById(itemId);
                if (item == null) HttpUtil.sendError(ex, 404, "Không tìm thấy món");
                else HttpUtil.sendJson(ex, 200, item.toString());
            } else {
                HttpUtil.sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            HttpUtil.sendError(ex, 500, e.getMessage());
        }
    }

    private void listMenu(HttpExchange ex) throws Exception {
        Map<String, String> params = HttpUtil.queryParams(ex);
        String keyword = params.get("q");
        Integer categoryId = params.containsKey("categoryId") ? Integer.parseInt(params.get("categoryId")) : null;
        JSONObject res = new JSONObject().put("items", menuDao.listAll(keyword, categoryId));
        HttpUtil.sendJson(ex, 200, res.toString());
    }
}
