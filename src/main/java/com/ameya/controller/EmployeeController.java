package com.ameya.controller;

import com.ameya.entity.Employee;
import com.ameya.repository.EmployeeRepository;
import com.ameya.service.EmployeeInfoBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

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
    @GetMapping("/by-department/{departmentName}")
    public List<Employee> getEmployeesByDepartmentSortedByName(@PathVariable String departmentName) {
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
