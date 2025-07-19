To fix the SQL injection vulnerability in the `authenticateUser` method, you should use a `PreparedStatement` instead of concatenating the `username` directly into the SQL query. Here's the corrected method:

```java
package com.ameya.service;

import java.sql.*;

public class VulnerableApp {
    public void authenticateUser(String username) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", "pass");
        // Use PreparedStatement to prevent SQL Injection
        String sql = "SELECT * FROM users WHERE username = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, username);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            System.out.println("User authenticated");
        }
        // Close resources
        rs.close();
        pstmt.close();
        conn.close();
    }
}
```

### Key Changes:
1. **PreparedStatement**: Replaced `Statement` with `PreparedStatement` to safely set the `username` parameter.
2. **Parameter Binding**: Used `pstmt.setString(1, username);` to bind the `username` parameter, which prevents SQL injection.
3. **Resource Management**: Added closing of `ResultSet`, `PreparedStatement`, and `Connection` to prevent resource leaks.