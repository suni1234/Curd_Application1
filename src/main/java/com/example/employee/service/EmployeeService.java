package com.example.employee.service;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    // ── CREATE ────────────────────────────────────────────────
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        log.info("Creating employee with email={}", request.getEmail());

        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .salary(request.getSalary())
                .build();

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created successfully with id={}", saved.getId());

        return toResponse(saved);
    }

    // ── READ ALL ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        log.info("Fetching all employees");
        List<EmployeeResponse> list = employeeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
        log.info("Total employees fetched: {}", list.size());
        return list;
    }

    // ── READ BY ID ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        log.info("Fetching employee with id={}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Employee not found with id={}", id);
                    return new EmployeeNotFoundException(id);
                });
        log.info("Employee found: id={} email={}", employee.getId(), employee.getEmail());
        return toResponse(employee);
    }

    // ── UPDATE ────────────────────────────────────────────────
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        log.info("Updating employee with id={}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update — employee not found with id={}", id);
                    return new EmployeeNotFoundException(id);
                });

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setDesignation(request.getDesignation());
        employee.setSalary(request.getSalary());

        Employee updated = employeeRepository.save(employee);
        log.info("Employee updated successfully id={}", updated.getId());

        return toResponse(updated);
    }

    // ── DELETE ────────────────────────────────────────────────
    @Transactional
    public void deleteEmployee(Long id) {
        log.info("Deleting employee with id={}", id);

        if (!employeeRepository.existsById(id)) {
            log.warn("Cannot delete — employee not found with id={}", id);
            throw new EmployeeNotFoundException(id);
        }

        employeeRepository.deleteById(id);
        log.info("Employee deleted successfully id={}", id);
    }

    // ── Mapper (Entity → Response DTO) ────────────────────────
    private EmployeeResponse toResponse(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .department(e.getDepartment())
                .designation(e.getDesignation())
                .salary(e.getSalary())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
