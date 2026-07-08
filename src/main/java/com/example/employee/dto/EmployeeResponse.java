package com.example.employee.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {
    private Long          id;
    private String        firstName;
    private String        lastName;
    private String        email;
    private String        department;
    private String        designation;
    private BigDecimal    salary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
