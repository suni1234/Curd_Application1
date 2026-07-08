package com.example.employee.controller;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // POST /api/v1/employees
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody EmployeeRequest request) {
        log.info("POST /employees - request received for email={}", request.getEmail());
        EmployeeResponse response = employeeService.createEmployee(request);
        log.info("POST /employees - response id={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/employees
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAll() {
        log.info("GET /employees - fetching all employees");
        List<EmployeeResponse> list = employeeService.getAllEmployees();
        log.info("GET /employees - returning {} records", list.size());
        return ResponseEntity.ok(list);
    }

    // GET /api/v1/employees/{id}
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        log.info("GET /employees/{} - request received", id);
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // PUT /api/v1/employees/{id}
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        log.info("PUT /employees/{} - request received", id);
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    // DELETE /api/v1/employees/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /employees/{} - request received", id);
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
