package com.smartcanteen;

import com.smartcanteen.db.DBConnection;
import com.smartcanteen.handler.*;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = DBConnection.getServerPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        // API routes
        server.createContext("/api/auth/", new AuthHandler());
        server.createContext("/api/menu", new MenuHandler());
        server.createContext("/api/orders", new OrderHandler());
        server.createContext("/api/staff/", new StaffHandler());
        server.createContext("/api/admin/", new AdminHandler());

        // Frontend tinh (HTML/CSS/JS) trong thu muc public
        server.createContext("/", new StaticFileHandler("public"));

        server.start();
        System.out.println("=================================================");
        System.out.println(" Smart Canteen dang chay tai http://localhost:" + port);
        System.out.println(" Nhan Ctrl+C de dung server");
        System.out.println("=================================================");
        Thread.currentThread().join();
    }
}
