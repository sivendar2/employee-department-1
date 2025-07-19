To fix the SQL injection vulnerability in the `getEmployeesByDepartment` method, you should use parameterized queries. This can be achieved using the `JdbcTemplate`'s `query` method with placeholders (`?`) for parameters. Here's the corrected method:

```java
public List<Employee> getEmployeesByDepartment(String departmentName) {
    // Use a parameterized query to prevent SQL injection
    String sql = "SELECT * FROM employees WHERE department_id = ?";
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Employee.class), departmentName);
}
```

In this version, the `departmentName` is passed as a parameter to the `query` method, which safely handles it, preventing SQL injection attacks.