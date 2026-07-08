package com.example.employee.unit;

import com.example.employee.controller.EmployeeController;
import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.GlobalExceptionHandler;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LEVEL 2 — Controller Unit Test
 *
 * What runs  : EmployeeController + GlobalExceptionHandler (real)
 * What is fake: EmployeeService (MockBean)
 * Database   : NONE
 * Spring     : Web layer only (slim context)
 * Speed      : Fast
 *
 * Purpose: Test HTTP layer — routes, status codes,
 *          request validation, JSON response shape.
 */
@WebMvcTest(controllers = {EmployeeController.class, GlobalExceptionHandler.class})
@DisplayName("EmployeeController - Unit Tests")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private EmployeeResponse sampleResponse;
    private EmployeeRequest  sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = EmployeeResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .department("Engineering")
                .designation("Developer")
                .salary(new BigDecimal("75000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = EmployeeRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .department("Engineering")
                .designation("Developer")
                .salary(new BigDecimal("75000"))
                .build();
    }

    // ════════════════════════════════════════════════
    //  CREATE
    // ════════════════════════════════════════════════
    @Test
    @DisplayName("POST /employees → 201 Created with response body")
    void create_shouldReturn201() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.department").value("Engineering"));

        verify(employeeService, times(1)).createEmployee(any(EmployeeRequest.class));
    }

    @Test
    @DisplayName("POST /employees with blank firstName → 400 Bad Request")
    void create_blankFirstName_shouldReturn400() throws Exception {
        EmployeeRequest bad = EmployeeRequest.builder()
                .firstName("")                    // blank — @NotBlank fails
                .lastName("Doe")
                .email("john@example.com")
                .department("Engineering")
                .designation("Developer")
                .salary(new BigDecimal("75000"))
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.firstName").value("First name is required"));

        verify(employeeService, never()).createEmployee(any());
    }

    @Test
    @DisplayName("POST /employees with invalid email → 400 Bad Request")
    void create_invalidEmail_shouldReturn400() throws Exception {
        EmployeeRequest bad = EmployeeRequest.builder()
                .firstName("John").lastName("Doe")
                .email("not-a-valid-email")        // @Email fails
                .department("Engineering").designation("Developer")
                .salary(new BigDecimal("75000"))
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(employeeService, never()).createEmployee(any());
    }

    // ════════════════════════════════════════════════
    //  READ ALL
    // ════════════════════════════════════════════════
    @Test
    @DisplayName("GET /employees → 200 with list of employees")
    void getAll_shouldReturn200WithList() throws Exception {
        EmployeeResponse emp2 = EmployeeResponse.builder()
                .id(2L).firstName("Jane").lastName("Smith")
                .email("jane@example.com").department("HR")
                .designation("Manager").salary(new BigDecimal("90000"))
                .build();

        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse, emp2));

        mockMvc.perform(get("/api/v1/employees"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].firstName").value("Jane"));
    }

    @Test
    @DisplayName("GET /employees → 200 with empty list")
    void getAll_noData_shouldReturnEmptyList() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ════════════════════════════════════════════════
    //  READ BY ID
    // ════════════════════════════════════════════════
    @Test
    @DisplayName("GET /employees/{id} → 200 when found")
    void getById_found_shouldReturn200() throws Exception {
        when(employeeService.getEmployeeById(1L))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("GET /employees/{id} → 404 when not found")
    void getById_notFound_shouldReturn404() throws Exception {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(get("/api/v1/employees/99"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Employee not found with id: 99"));
    }

    // ════════════════════════════════════════════════
    //  UPDATE
    // ════════════════════════════════════════════════
    @Test
    @DisplayName("PUT /employees/{id} → 200 with updated data")
    void update_shouldReturn200() throws Exception {
        EmployeeResponse updated = EmployeeResponse.builder()
                .id(1L).firstName("John Updated").lastName("Doe")
                .email("john.doe@example.com").department("Architecture")
                .designation("Senior Developer").salary(new BigDecimal("95000"))
                .build();

        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John Updated"))
                .andExpect(jsonPath("$.department").value("Architecture"));
    }

    @Test
    @DisplayName("PUT /employees/{id} → 404 when not found")
    void update_notFound_shouldReturn404() throws Exception {
        when(employeeService.updateEmployee(eq(99L), any()))
                .thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(put("/api/v1/employees/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isNotFound());
    }

    // ════════════════════════════════════════════════
    //  DELETE
    // ════════════════════════════════════════════════
    @Test
    @DisplayName("DELETE /employees/{id} → 204 No Content")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isNoContent());

        verify(employeeService, times(1)).deleteEmployee(1L);
    }

    @Test
    @DisplayName("DELETE /employees/{id} → 404 when not found")
    void delete_notFound_shouldReturn404() throws Exception {
        doThrow(new EmployeeNotFoundException(99L))
                .when(employeeService).deleteEmployee(99L);

        mockMvc.perform(delete("/api/v1/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Employee not found with id: 99"));
    }
}
