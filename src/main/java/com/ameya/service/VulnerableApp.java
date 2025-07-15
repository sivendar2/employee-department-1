package com.ameya.service;

import java.sql.*;
import org.springframework.web.util.HtmlUtils;

public class VulnerableApp {

    public void authenticateUser(String username) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", "pass");
        // Vulnerable: SQL Injection here
        String sql = "SELECT * FROM users WHERE username = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            System.out.println("User authenticated");
        }
    }
}
