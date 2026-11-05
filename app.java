package com.hospital.opd;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/", new StaticHandler());
        server.createContext("/api/patients", new PatientHandler());
        server.createContext("/api/doctors", new DoctorHandler());
        server.createContext("/api/appointments", new AppointmentHandler());
        server.createContext("/api/consultations", new ConsultationHandler());
        server.createContext("/api/bills", new BillHandler());
        server.createContext("/api/payments", new PaymentHandler());
        server.createContext("/api/reports", new ReportHandler());
        
        server.setExecutor(null);
        System.out.println("Hospital Management System OPD server running on http://localhost:8080");
        server.start();
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        String clean = json.trim();
        if (clean.startsWith("{")) clean = clean.substring(1);
        if (clean.endsWith("}")) clean = clean.substring(0, clean.length() - 1);
        String[] pairs = clean.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim().replace("\"", "");
                map.put(key, value);
            }
        }
        return map;
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            File file = new File("." + path);
            if (!file.exists() || file.isDirectory()) {
                file = new File("src/main/resources" + path);
            }
            if (file.exists() && !file.isDirectory()) {
                String contentType = "text/html";
                if (path.endsWith(".css")) contentType = "text/css";
                else if (path.endsWith(".js")) contentType = "application/javascript";
                
                byte[] bytes = Files.readAllBytes(file.toPath());
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } else {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                OutputStream os = exchange.getResponseBody();
                os.write(notFound.getBytes());
                os.close();
            }
        }
    }

    static class PatientHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                Map<String, String> data = parseJsonToMap(body);
                String sql = "INSERT INTO patient (name, gender, dob, phone, email, blood_group, address) VALUES (?, ?, ?::date, ?, ?, ?, ?)";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, data.get("name"));
                    pstmt.setString(2, data.get("gender"));
                    pstmt.setString(3, data.get("dob"));
                    pstmt.setString(4, data.get("phone"));
                    pstmt.setString(5, data.get("email"));
                    pstmt.setString(6, data.get("blood_group"));
                    pstmt.setString(7, data.get("address"));
                    pstmt.executeUpdate();
                    sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"Patient registered successfully\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                }
            } else if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String sql = "SELECT patient_id, name, phone, blood_group FROM patient ORDER BY patient_id DESC";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"patient_id\":%d,\"name\":\"%s\",\"phone\":\"%s\",\"blood_group\":\"%s\"}",
                                rs.getInt("patient_id"),
                                rs.getString("name"),
                                rs.getString("phone") != null ? rs.getString("phone") : "",
                                rs.getString("blood_group") != null ? rs.getString("blood_group") : ""));
                        first = false;
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                }
            }
        }
    }

    static class DoctorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String sql = "SELECT doctor_id, name, specialization, consultation_fee FROM doctor ORDER BY name";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"doctor_id\":%d,\"name\":\"%s\",\"specialization\":\"%s\",\"consultation_fee\":%.2f}",
                                rs.getInt("doctor_id"),
                                rs.getString("name"),
                                rs.getString("specialization") != null ? rs.getString("specialization") : "",
                                rs.getDouble("consultation_fee")));
                        first = false;
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                }
            }
        }
    }

    static class AppointmentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                Map<String, String> data = parseJsonToMap(body);
                String sql = "INSERT INTO appointment (patient_id, doctor_id, appointment_date, status) VALUES (?, ?, ?::date, 'Pending')";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, Integer.parseInt(data.get("patient_id")));
                    pstmt.setInt(2, Integer.parseInt(data.get("doctor_id")));
                    pstmt.setString(3, data.get("appointment_date"));
                    pstmt.executeUpdate();
                    sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"Appointment booked successfully\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                }
            } else if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String sql = "SELECT a.appointment_id, a.patient_id, p.name AS patient_name, a.doctor_id, d.name AS doctor_name, d.consultation_fee, a.appointment_date, a.status FROM appointment a JOIN patient p ON a.patient_id = p.patient_id JOIN doctor d ON a.doctor_id = d.doctor_id WHERE a.status = 'Pending' ORDER BY a.appointment_date ASC";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"appointment_id\":%d,\"patient_id\":%d,\"patient_name\":\"%s\",\"doctor_id\":%d,\"doctor_name\":\"%s\",\"consultation_fee\":%.2f,\"appointment_date\":\"%s\",\"status\":\"%s\"}",
                                rs.getInt("appointment_id"),
                                rs.getInt("patient_id"),
                                rs.getString("patient_name"),
                                rs.getInt("doctor_id"),
                                rs.getString("doctor_name"),
                                rs.getDouble("consultation_fee"),
                                rs.getString("appointment_date"),
                                rs.getString("status")));
                        first = false;
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                }
            }
        }
    }

    static class ConsultationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                Map<String, String> data = parseJsonToMap(body);
                int appointmentId = Integer.parseInt(data.get("appointment_id"));
                String diagnosis = data.get("diagnosis");
                String notes = data.get("notes");
                String followUpDate = data.get("follow_up_date");

                try (Connection conn = DBConnection.getConnection()) {
                    conn.setAutoCommit(false);
                    int patientId = 0;
                    double fee = 0.0;
                    String getApptSql = "SELECT a.patient_id, d.consultation_fee FROM appointment a JOIN doctor d ON a.doctor_id = d.doctor_id WHERE a.appointment_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(getApptSql)) {
                        pstmt.setInt(1, appointmentId);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            patientId = rs.getInt("patient_id");
                            fee = rs.getDouble("consultation_fee");
                        }
                    }

                    int consultationId = 0;
                    String insertConsultSql = "INSERT INTO consultation (appointment_id, diagnosis, notes, follow_up_date) VALUES (?, ?, ?, ?::date) RETURNING consultation_id";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertConsultSql)) {
                        pstmt.setInt(1, appointmentId);
                        pstmt.setString(2, diagnosis);
                        pstmt.setString(3, notes);
                        pstmt.setString(4, (followUpDate != null && !followUpDate.isEmpty()) ? followUpDate : null);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            consultationId = rs.getInt(1);
                        }
                    }

                    String updateApptSql = "UPDATE appointment SET status = 'Completed' WHERE appointment_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateApptSql)) {
                        pstmt.setInt(1, appointmentId);
                        pstmt.executeUpdate();
                    }

                    String insertBillSql = "INSERT INTO bill (consultation_id, patient_id, total_amount, paid_amount, status) VALUES (?, ?, ?, 0.00, 'Pending')";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertBillSql)) {
                        pstmt.setInt(1, consultationId);
                        pstmt.setInt(2, patientId);
                        pstmt.setDouble(3, fee);
                        pstmt.executeUpdate();
                    }

                    conn.commit();
                    sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"Consultation recorded and bill created\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                }
            }
        }
    }

    static class BillHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String sql = "SELECT b.bill_id, b.patient_id, p.name AS patient_name, b.total_amount, b.paid_amount, (b.total_amount - b.paid_amount) AS balance_due, b.status FROM bill b JOIN patient p ON b.patient_id = p.patient_id WHERE b.status IN ('Pending', 'Partial') ORDER BY b.bill_id ASC";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"bill_id\":%d,\"patient_id\":%d,\"patient_name\":\"%s\",\"total_amount\":%.2f,\"paid_amount\":%.2f,\"balance_due\":%.2f,\"status\":\"%s\"}",
                                rs.getInt("bill_id"),
                                rs.getInt("patient_id"),
                                rs.getString("patient_name"),
                                rs.getDouble("total_amount"),
                                rs.getDouble("paid_amount"),
                                rs.getDouble("balance_due"),
                                rs.getString("status")));
                        first = false;
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                }
            }
        }
    }

    static class PaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                Map<String, String> data = parseJsonToMap(body);
                int billId = Integer.parseInt(data.get("bill_id"));
                double amount = Double.parseDouble(data.get("amount"));
                String paymentMode = data.get("payment_mode");

                try (Connection conn = DBConnection.getConnection()) {
                    conn.setAutoCommit(false);

                    String insertPaySql = "INSERT INTO payment (bill_id, amount, payment_mode, payment_date) VALUES (?, ?, ?, CURRENT_DATE)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertPaySql)) {
                        pstmt.setInt(1, billId);
                        pstmt.setDouble(2, amount);
                        pstmt.setString(3, paymentMode);
                        pstmt.executeUpdate();
                    }

                    double totalAmount = 0.0;
                    double currentPaid = 0.0;
                    String selectBillSql = "SELECT total_amount, paid_amount FROM bill WHERE bill_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(selectBillSql)) {
                        pstmt.setInt(1, billId);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            totalAmount = rs.getDouble("total_amount");
                            currentPaid = rs.getDouble("paid_amount");
                        }
                    }

                    double newPaid = currentPaid + amount;
                    String newStatus = "Pending";
                    if (newPaid >= totalAmount) {
                        newStatus = "Paid";
                    } else if (newPaid > 0) {
                        newStatus = "Partial";
                    }

                    String updateBillSql = "UPDATE bill SET paid_amount = ?, status = ? WHERE bill_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateBillSql)) {
                        pstmt.setDouble(1, newPaid);
                        pstmt.setString(2, newStatus);
                        pstmt.setInt(3, billId);
                        pstmt.executeUpdate();
                    }

                    conn.commit();
                    sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"Payment recorded successfully\",\"new_status\":\"" + newStatus + "\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                }
            }
        }
    }

    static class ReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            try (Connection conn = DBConnection.getConnection()) {
                if (path.endsWith("/daily-appointments")) {
                    String sql = "SELECT appointment_date, COUNT(*) AS total_appointments, SUM(CASE WHEN status='Completed' THEN 1 ELSE 0 END) AS completed, SUM(CASE WHEN status='Pending' THEN 1 ELSE 0 END) AS pending FROM appointment GROUP BY appointment_date ORDER BY appointment_date DESC";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql);
                         ResultSet rs = pstmt.executeQuery()) {
                        StringBuilder json = new StringBuilder("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) json.append(",");
                            json.append(String.format("{\"appointment_date\":\"%s\",\"total_appointments\":%d,\"completed\":%d,\"pending\":%d}",
                                    rs.getString("appointment_date"),
                                    rs.getInt("total_appointments"),
                                    rs.getInt("completed"),
                                    rs.getInt("pending")));
                            first = false;
                        }
                        json.append("]");
                        sendJsonResponse(exchange, 200, json.toString());
                    }
                } else if (path.endsWith("/doctor-patients")) {
                    String sql = "SELECT d.name AS doctor_name, d.specialization, COUNT(a.appointment_id) AS total_patients FROM doctor d LEFT JOIN appointment a ON d.doctor_id = a.doctor_id GROUP BY d.doctor_id, d.name, d.specialization ORDER BY total_patients DESC";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql);
                         ResultSet rs = pstmt.executeQuery()) {
                        StringBuilder json = new StringBuilder("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) json.append(",");
                            json.append(String.format("{\"doctor_name\":\"%s\",\"specialization\":\"%s\",\"total_patients\":%d}",
                                    rs.getString("doctor_name"),
                                    rs.getString("specialization") != null ? rs.getString("specialization") : "",
                                    rs.getInt("total_patients")));
                            first = false;
                        }
                        json.append("]");
                        sendJsonResponse(exchange, 200, json.toString());
                    }
                } else if (path.endsWith("/pending-bills")) {
                    String sql = "SELECT b.bill_id, p.name AS patient_name, b.total_amount, b.paid_amount, (b.total_amount - b.paid_amount) AS amount_due, b.status FROM bill b JOIN patient p ON b.patient_id = p.patient_id WHERE b.status != 'Paid' ORDER BY b.bill_id DESC";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql);
                         ResultSet rs = pstmt.executeQuery()) {
                        StringBuilder json = new StringBuilder("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) json.append(",");
                            json.append(String.format("{\"bill_id\":%d,\"patient_name\":\"%s\",\"total_amount\":%.2f,\"paid_amount\":%.2f,\"amount_due\":%.2f,\"status\":\"%s\"}",
                                    rs.getInt("bill_id"),
                                    rs.getString("patient_name"),
                                    rs.getDouble("total_amount"),
                                    rs.getDouble("paid_amount"),
                                    rs.getDouble("amount_due"),
                                    rs.getString("status")));
                            first = false;
                        }
                        json.append("]");
                        sendJsonResponse(exchange, 200, json.toString());
                    }
                } else if (path.endsWith("/revenue")) {
                    String sql = "SELECT payment_mode, SUM(amount) AS total_collected, COUNT(*) AS transaction_count FROM payment GROUP BY payment_mode";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql);
                         ResultSet rs = pstmt.executeQuery()) {
                        StringBuilder json = new StringBuilder("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) json.append(",");
                            json.append(String.format("{\"payment_mode\":\"%s\",\"total_collected\":%.2f,\"transaction_count\":%d}",
                                    rs.getString("payment_mode"),
                                    rs.getDouble("total_collected"),
                                    rs.getInt("transaction_count")));
                            first = false;
                        }
                        json.append("]");
                        sendJsonResponse(exchange, 200, json.toString());
                    }
                } else {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Endpoint not found\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            }
        }
    }
}