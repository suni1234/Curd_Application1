package com.example.employee.integration;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LEVEL 3 — Integration Test (Full flow end-to-end)
 *
 * What runs  : Full Spring context
 *              Controller → Service → Repository → H2 DB
 * Database   : H2 in-memory (MySQL NEVER touched)
 * Spring     : Full context starts up
 * Speed      : Slower (few seconds per test)
 *
 * Purpose: Verify the COMPLETE flow works — HTTP request
 *          goes all the way to DB and back.
 *          After each API call, DB is verified directly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Employee API - Integration Tests (H2 DB)")
class EmployeeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmployeeRepository employeeRepository;

    // ── clean DB before each test ─────────────────────────────
    @BeforeEach
    void cleanDatabase() {
        employeeRepository.deleteAll();
        System.out.println("\n🧹 H2 DB cleaned — starting fresh");
    }

    // ── print DB state after each test ────────────────────────
    @AfterEach
    void printDbState() {
        List<Employee> records = employeeRepository.findAll();
        System.out.println("📋 H2 records after test: " + records.size());
        records.forEach(e -> System.out.println(
                "   → id=" + e.getId()
                + " name=" + e.getFirstName() + " " + e.getLastName()
                + " email=" + e.getEmail()));
    }

    // ── helper to build a request ─────────────────────────────
    private EmployeeRequest buildRequest(String firstName, String email) {
        return EmployeeRequest.builder()
                .firstName(firstName).lastName("Doe")
                .email(email).department("Engineering")
                .designation("Developer")
                .salary(new BigDecimal("75000"))
                .build();
    }

    // ── helper to save directly to H2 ────────────────────────
    private Employee saveToDb(String firstName, String email) {
        return employeeRepository.save(
                Employee.builder()
                        .firstName(firstName).lastName("Doe")
                        .email(email).department("Engineering")
                        .designation("Developer")
                        .salary(new BigDecimal("75000"))
                        .build());
    }

    // ════════════════════════════════════════════════════════
    //  CREATE — POST /api/v1/employees
    // ════════════════════════════════════════════════════════
    @Test
    @Order(1)
    @DisplayName("POST → record saved in H2, MySQL never touched")
    void create_shouldSaveToH2() throws Exception {
        System.out.println("\n🔵 H2 records BEFORE POST: " + employeeRepository.count());

        // 1. Call real POST API
        String responseBody = mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest("John", "john@example.com"))))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andReturn().getResponse().getContentAsString();

        System.out.println("🟢 API Response: " + responseBody);

        // 2. Verify record EXISTS in H2
        List<Employee> inDb = employeeRepository.findAll();
        assertThat(inDb).hasSize(1);
        assertThat(inDb.get(0).getFirstName()).isEqualTo("John");
        assertThat(inDb.get(0).getEmail()).isEqualTo("john@example.com");
        assertThat(inDb.get(0).getId()).isNotNull();
        assertThat(inDb.get(0).getCreatedAt()).isNotNull(); // @PrePersist ran

        System.out.println("✅ H2 record confirmed:");
        System.out.println("   id        = " + inDb.get(0).getId());
        System.out.println("   firstName = " + inDb.get(0).getFirstName());
        System.out.println("   createdAt = " + inDb.get(0).getCreatedAt());
    }

    // ════════════════════════════════════════════════════════
    //  READ ALL — GET /api/v1/employees
    // ════════════════════════════════════════════════════════
    @Test
    @Order(2)
    @DisplayName("GET all → reads 3 pre-saved H2 records")
    void getAll_shouldReturnAllFromH2() throws Exception {
        // Pre-load directly into H2
        saveToDb("Alice", "alice@example.com");
        saveToDb("Bob",   "bob@example.com");
        saveToDb("Carol", "carol@example.com");

        System.out.println("\n🔵 H2 pre-loaded: " + employeeRepository.count() + " records");

        mockMvc.perform(get("/api/v1/employees"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].firstName").value("Alice"));

        System.out.println("✅ GET all returned 3 records from H2");
    }

    // ════════════════════════════════════════════════════════
    //  READ BY ID — GET /api/v1/employees/{id}
    // ════════════════════════════════════════════════════════
    @Test
    @Order(3)
    @DisplayName("GET /{id} → returns correct H2 record")
    void getById_shouldFetchCorrectRecord() throws Exception {
        Employee saved = saveToDb("Dave", "dave@example.com");
        System.out.println("\n🔵 Saved to H2: id=" + saved.getId());

        mockMvc.perform(get("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.firstName").value("Dave"))
                .andExpect(jsonPath("$.email").value("dave@example.com"));

        System.out.println("✅ GET /" + saved.getId() + " returned correct H2 record");
    }

    @Test
    @Order(4)
    @DisplayName("GET /{id} → 404 when record not in H2")
    void getById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/employees/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Employee not found with id: 9999"));

        System.out.println("✅ 404 returned for missing H2 record");
    }

    // ════════════════════════════════════════════════════════
    //  UPDATE — PUT /api/v1/employees/{id}
    // ════════════════════════════════════════════════════════
    @Test
    @Order(5)
    @DisplayName("PUT → H2 record updated, old values replaced")
    void update_shouldModifyH2Record() throws Exception {
        Employee saved = saveToDb("Eve", "eve@example.com");
        System.out.println("\n🔵 H2 BEFORE update:");
        System.out.println("   firstName  = " + saved.getFirstName());
        System.out.println("   department = " + saved.getDepartment());

        EmployeeRequest updateReq = EmployeeRequest.builder()
                .firstName("Eva").lastName("Smith")
                .email("eva@example.com").department("Product")
                .designation("Lead Developer").salary(new BigDecimal("95000"))
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Eva"))
                .andExpect(jsonPath("$.department").value("Product"));

        // Verify H2 record changed
        Employee updated = employeeRepository.findById(saved.getId()).orElseThrow();
        System.out.println("🟢 H2 AFTER update:");
        System.out.println("   firstName  = " + updated.getFirstName());
        System.out.println("   department = " + updated.getDepartment());
        System.out.println("   updatedAt  = " + updated.getUpdatedAt());

        assertThat(updated.getFirstName()).isEqualTo("Eva");
        assertThat(updated.getDepartment()).isEqualTo("Product");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
    }

    // ════════════════════════════════════════════════════════
    //  DELETE — DELETE /api/v1/employees/{id}
    // ════════════════════════════════════════════════════════
    @Test
    @Order(6)
    @DisplayName("DELETE → record removed from H2, GET returns 404")
    void delete_shouldRemoveFromH2() throws Exception {
        Employee saved = saveToDb("Frank", "frank@example.com");
        System.out.println("\n🔵 H2 BEFORE delete: exists="
                + employeeRepository.existsById(saved.getId()));

        // Delete via API
        mockMvc.perform(delete("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        // Verify record GONE from H2
        boolean exists = employeeRepository.existsById(saved.getId());
        System.out.println("🟢 H2 AFTER delete: exists=" + exists);
        assertThat(exists).isFalse();
        assertThat(employeeRepository.count()).isZero();

        // GET should now return 404
        mockMvc.perform(get("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isNotFound());

        System.out.println("✅ 404 confirmed after delete");
    }

    // ════════════════════════════════════════════════════════
    //  FULL CRUD LIFECYCLE in one test
    // ════════════════════════════════════════════════════════
    @Test
    @Order(7)
    @DisplayName("Full CRUD lifecycle → Create → Read → Update → Delete in H2")
    void fullCrudLifecycle() throws Exception {
        System.out.println("\n══════════ FULL CRUD LIFECYCLE ══════════");

        // 1. CREATE
        String postRes = mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest("Grace", "grace@example.com"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readValue(postRes, EmployeeResponse.class).getId();
        assertThat(employeeRepository.count()).isEqualTo(1);
        System.out.println("1️⃣  CREATE → H2 record id=" + id);

        // 2. READ
        mockMvc.perform(get("/api/v1/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Grace"));
        System.out.println("2️⃣  READ   → H2 record found");

        // 3. UPDATE
        EmployeeRequest updateReq = buildRequest("Grace Updated", "grace@example.com");
        mockMvc.perform(put("/api/v1/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Grace Updated"));
        assertThat(employeeRepository.findById(id).get().getFirstName())
                .isEqualTo("Grace Updated");
        System.out.println("3️⃣  UPDATE → H2 record modified");

        // 4. DELETE
        mockMvc.perform(delete("/api/v1/employees/{id}", id))
                .andExpect(status().isNoContent());
        assertThat(employeeRepository.existsById(id)).isFalse();
        System.out.println("4️⃣  DELETE → H2 record removed");

        System.out.println("✅ Full lifecycle passed — MySQL never touched");
        System.out.println("══════════════════════════════════════════");
    }
}
