package com.example.employee.unit;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.service.EmployeeService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LEVEL 1 — Service Unit Test
 *
 * What runs  : EmployeeService (real)
 * What is fake: EmployeeRepository (Mockito mock)
 * Database   : NONE — not even H2
 * Spring     : NOT started
 * Speed      : Milliseconds
 *
 * Purpose: Test business logic in complete isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService - Unit Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    // ── shared test data ─────────────────────────────────────
    private Employee   sampleEmployee;
    private EmployeeRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleEmployee = Employee.builder()
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
    @DisplayName("createEmployee → should save and return response")
    void createEmployee_shouldSaveAndReturn() {
        // Arrange: when save() is called, return sampleEmployee
        when(employeeRepository.save(any(Employee.class)))
                .thenReturn(sampleEmployee);

        // Act
        EmployeeResponse response = employeeService.createEmployee(sampleRequest);

        // Assert response fields
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");

        // Assert save() was called exactly once with correct data
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(captor.getValue().getDepartment()).isEqualTo("Engineering");
    }

    // ════════════════════════════════════════════════
    //  READ ALL
    // ════════════════════════════════════════════════
    @Test
    @DisplayName("getAllEmployees → should return list of responses")
    void getAllEmployees_shouldReturnList() {
        Employee emp2 = Employee.builder()
                .id(2L).firstName("Jane").lastName("Smith")
                .email("jane@example.com").department("HR")
                .designation("Manager").salary(new BigDecimal("90000"))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(employeeRepository.findAll())
                .thenReturn(List.of(sampleEmployee, emp2));

        List<EmployeeResponse> result = employeeService.getAllEmployees();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EmployeeResponse::getEmail)
                .containsExactlyInAnyOrder(
                        "john.doe@example.com", "jane@example.com");

        verify(employeeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllEmployees → should return empty list when no records")
    void getAllEmployees_noRecords_shouldReturnEmptyList() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        List<EmployeeResponse> result = employeeService.getAllEmployees();

        assertThat(result).isEmpty();
    }

    // ════════════════════════════════════════════════
    //  READ BY ID
    // ════════════════════════════════════════════════
    @Test
    @DisplayName("getEmployeeById → should return employee when found")
    void getEmployeeById_found_shouldReturn() {
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(sampleEmployee));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("John");
        verify(employeeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getEmployeeById → should throw EmployeeNotFoundException when missing")
    void getEmployeeById_notFound_shouldThrow() {
        when(employeeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");

        verify(employeeRepository, times(1)).findById(99L);
    }

    // ════════════════════════════════════════════════
    //  UPDATE
    // ════════════════════════════════════════════════
    @Test
    @DisplayName("updateEmployee → should update fields and save")
    void updateEmployee_found_shouldUpdate() {
        EmployeeRequest updateReq = EmployeeRequest.builder()
                .firstName("John Updated")
                .lastName("Doe")
                .email("john.updated@example.com")
                .department("Architecture")
                .designation("Senior Developer")
                .salary(new BigDecimal("95000"))
                .build();

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(sampleEmployee));
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = employeeService.updateEmployee(1L, updateReq);

        assertThat(response.getFirstName()).isEqualTo("John Updated");
        assertThat(response.getDepartment()).isEqualTo("Architecture");
        assertThat(response.getSalary()).isEqualByComparingTo("95000");

        verify(employeeRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("updateEmployee → should throw when employee not found")
    void updateEmployee_notFound_shouldThrow() {
        when(employeeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(99L, sampleRequest))
                .isInstanceOf(EmployeeNotFoundException.class);

        // Save should never be called if employee not found
        verify(employeeRepository, never()).save(any());
    }

    // ════════════════════════════════════════════════
    //  DELETE
    // ════════════════════════════════════════════════
    @Test
    @DisplayName("deleteEmployee → should delete when employee exists")
    void deleteEmployee_exists_shouldDelete() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(1L);

        employeeService.deleteEmployee(1L);

        verify(employeeRepository, times(1)).existsById(1L);
        verify(employeeRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteEmployee → should throw when employee not found")
    void deleteEmployee_notFound_shouldThrow() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");

        // deleteById should never be called if not found
        verify(employeeRepository, never()).deleteById(anyLong());
    }
}
