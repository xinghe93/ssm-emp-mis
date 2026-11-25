package com.xinghe.emp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinghe.emp.config.WebMvcConfig;
import com.xinghe.emp.dto.EmpCreateRequest;
import com.xinghe.emp.dto.EmpQueryRequest;
import com.xinghe.emp.dto.EmpUpdateRequest;
import com.xinghe.emp.entity.Emp;
import com.xinghe.emp.service.EmpService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class EmpControllerTest {

    private static final String EMPLOYEE_JSON = """
            {"empName":"张三","gender":1,"dept":"研发部","post":"工程师","salary":12345.67,"hireDate":"2024-02-29"}
            """;
    private static final BigDecimal SALARY = new BigDecimal("12345.67");
    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 2, 29);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static AnnotationConfigWebApplicationContext context;
    private static RecordingEmpService service;
    private static MockMvc mvc;

    @BeforeAll
    static void startMvc() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        service = new RecordingEmpService();
        context.register(WebMvcConfig.class);
        context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("contractTestEmpService", service));
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterAll
    static void stopMvc() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void createsEmployeeAndReturnsLocationWithContextPath() throws Exception {
        var response = mvc.perform(post("/ssm-emp-mis/emps").contextPath("/ssm-emp-mis")
                .contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE_JSON))
                .andReturn().getResponse();

        assertEquals(201, response.getStatus());
        assertEquals("/ssm-emp-mis/emps/42", response.getHeader(HttpHeaders.LOCATION));
        assertEquals(new EmpCreateRequest("张三", 1, "研发部", "工程师", SALARY, HIRE_DATE, null), service.created);
        var data = successData(JSON.readTree(response.getContentAsByteArray()));
        assertEquals(42, data.get("empId").asInt());
        assertEquals("张三", data.get("empName").asString());
        assertEquals("2024-02-29", data.get("hireDate").asString());
        assertEquals(0, SALARY.compareTo(data.get("salary").decimalValue()));
    }

    @Test
    void updatesEmployeeUsingBodyIdAndStatus() throws Exception {
        var data = ok(mvc.perform(put("/emps").contentType(MediaType.APPLICATION_JSON).content("""
                {"empId":42,"empName":"张三","gender":1,"dept":"研发部","post":"主管","salary":12345.67,"hireDate":"2024-02-29","status":0}
                """)));
        assertEquals(new EmpUpdateRequest(42L, "张三", 1, "研发部", "主管", SALARY, HIRE_DATE, 0), service.updated);
        assertEquals(0, data.get("status").asInt());
    }

    @Test
    void readsEmployeeByPathId() throws Exception {
        var data = ok(mvc.perform(get("/emps/42")));
        assertEquals(42L, service.readId);
        assertEquals(42, data.get("empId").asInt());
    }

    @Test
    void deletesOneEmployeeByPathId() throws Exception {
        var data = ok(mvc.perform(delete("/emps/42")));
        assertEquals(42L, service.deletedId);
        assertEquals(1, data.asInt());
    }

    @Test
    void batchDeletesIdsFromCommaSeparatedQueryParameter() throws Exception {
        var data = ok(mvc.perform(delete("/emps").param("ids", "9,42")));
        assertEquals(List.of(9L, 42L), service.deletedIds);
        assertEquals(2, data.asInt());
    }

    @Test
    void bindsCombinedFiltersAndReturnsOnlyPublicPageFields() throws Exception {
        var data = ok(mvc.perform(get("/emps").param("name", "张").param("gender", "1").param("dept", "研发部")
                .param("page", "2").param("size", "2")));
        assertEquals(new EmpQueryRequest("张", 1, "研发部", 2, 2), service.queried);
        assertEquals(Set.of("records", "total", "page", "size", "pages"), fieldNames(data));
        assertEquals(2, data.get("page").asInt());
        assertEquals(2, data.get("size").asInt());
        assertEquals(3, data.get("total").asInt());
        assertEquals(2, data.get("pages").asInt());
        assertEquals(1, data.get("records").size());
    }

    @Test
    void leavesPaginationDefaultsToService() throws Exception {
        var data = ok(mvc.perform(get("/emps")));
        assertEquals(new EmpQueryRequest(null, null, null, null, null), service.queried);
        assertEquals(1, data.get("page").asInt());
        assertEquals(10, data.get("size").asInt());
    }

    @Test
    void rejectsMissingBatchIdsAndUnconvertibleParameters() throws Exception {
        assertEquals(400, mvc.perform(delete("/emps")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(delete("/emps").param("ids", "42,invalid")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/emps").param("page", "invalid")).andReturn().getResponse().getStatus());
    }

    private static JsonNode ok(ResultActions result) throws Exception {
        var response = result.andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(response.getContentType())));
        return successData(JSON.readTree(response.getContentAsByteArray()));
    }

    private static JsonNode successData(JsonNode response) {
        assertEquals(Set.of("code", "message", "data"), fieldNames(response));
        assertEquals(0, response.get("code").asInt());
        assertEquals("成功", response.get("message").asString());
        return response.get("data");
    }

    private static Set<String> fieldNames(JsonNode node) {
        return node.properties().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    static class RecordingEmpService implements EmpService {
        private EmpCreateRequest created;
        private EmpUpdateRequest updated;
        private Long readId;
        private Long deletedId;
        private List<Long> deletedIds;
        private EmpQueryRequest queried;

        @Override
        public Emp create(EmpCreateRequest request) {
            created = request;
            return employee(1);
        }

        @Override
        public Emp update(EmpUpdateRequest request) {
            updated = request;
            return employee(request.status());
        }

        @Override
        public int deleteById(Long id) {
            deletedId = id;
            return 1;
        }

        @Override
        public int deleteBatch(List<Long> ids) {
            deletedIds = List.copyOf(ids);
            return ids.size();
        }

        @Override
        public Emp getById(Long id) {
            readId = id;
            return employee(1);
        }

        @Override
        public IPage<Emp> findPage(EmpQueryRequest request) {
            queried = request;
            long current = request.page() == null ? 1 : request.page();
            long size = request.size() == null ? 10 : request.size();
            return new Page<Emp>(current, size, 3).setRecords(List.of(employee(1)));
        }

        private static Emp employee(int status) {
            var employee = new Emp();
            employee.setEmpId(42L);
            employee.setEmpName("张三");
            employee.setHireDate(HIRE_DATE);
            employee.setSalary(SALARY);
            employee.setStatus(status);
            return employee;
        }
    }
}
