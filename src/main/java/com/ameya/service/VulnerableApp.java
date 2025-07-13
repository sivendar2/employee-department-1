To fix the SQL injection vulnerability in the `authenticateUser` method, you should use a `PreparedStatement` instead of a `Statement`. This will allow you to safely parameterize the SQL query. Here's the corrected method:

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
1. **Use `PreparedStatement`:** This allows you to safely include user input in the SQL query.
2. **Parameterize the Query:** The `?` placeholder is used in the SQL string, and the actual value is set using `pstmt.setString(1, username)`.
3. **Resource Management:** Ensure that the `ResultSet`, `PreparedStatement`, and `Connection` are closed after use to prevent resource leaks.