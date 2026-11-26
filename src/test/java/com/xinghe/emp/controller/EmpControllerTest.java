package com.xinghe.emp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinghe.emp.common.BusinessException;
import com.xinghe.emp.config.WebMvcConfig;
import com.xinghe.emp.dto.EmpCreateRequest;
import com.xinghe.emp.dto.EmpQueryRequest;
import com.xinghe.emp.dto.EmpUpdateRequest;
import com.xinghe.emp.entity.Emp;
import com.xinghe.emp.service.EmpService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class EmpControllerTest {

    private static final String EMPLOYEE_JSON = """
            {"empName":"张三","gender":1,"dept":"研发部","post":"工程师","salary":12345.67,"hireDate":"2024-02-29"}
            """;
    private static final BigDecimal SALARY = new BigDecimal("12345.67");
    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 2, 29);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String LOGIN_JSON = """
            {"username":"mvc-test","password":"mvc-test-password"}
            """;
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";
    private static AnnotationConfigWebApplicationContext context;
    private static RecordingEmpService service;
    private static MockMvc mvc;

    @BeforeAll
    static void startMvc() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("mvcTestAuth", Map.of(
                "AUTH_USERNAME", "mvc-test", "AUTH_PASSWORD", "mvc-test-password",
                "CORS_ALLOWED_ORIGINS", "http://127.0.0.1:5173, " + FRONTEND_ORIGIN)));
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
                .session(session()).contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE_JSON))
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
        var data = ok(mvc.perform(put("/emps").session(session()).contentType(MediaType.APPLICATION_JSON).content("""
                {"empId":42,"empName":"张三","gender":1,"dept":"研发部","post":"主管","salary":12345.67,"hireDate":"2024-02-29","status":0}
                """)));
        assertEquals(new EmpUpdateRequest(42L, "张三", 1, "研发部", "主管", SALARY, HIRE_DATE, 0), service.updated);
        assertEquals(0, data.get("status").asInt());
    }

    @Test
    void readsEmployeeByPathId() throws Exception {
        var data = ok(mvc.perform(get("/emps/42").session(session())));
        assertEquals(42L, service.readId);
        assertEquals(42, data.get("empId").asInt());
    }

    @Test
    void deletesOneEmployeeByPathId() throws Exception {
        var data = ok(mvc.perform(delete("/emps/42").session(session())));
        assertEquals(42L, service.deletedId);
        assertEquals(1, data.asInt());
    }

    @Test
    void rejectsBatchIdsInQueryParameters() throws Exception {
        var authorized = session();
        error(mvc.perform(delete("/emps").session(authorized).param("ids", "9,42")), 400);
        error(mvc.perform(delete("/emps").session(authorized).param("ids", "42")
                .contentType(MediaType.APPLICATION_JSON).content("[43]")), 400);
    }

    @Test
    void batchDeletesIdsFromJsonArray() throws Exception {
        var data = ok(mvc.perform(delete("/emps").session(session())
                .contentType(MediaType.APPLICATION_JSON).content("[8,43]")));
        assertEquals(List.of(8L, 43L), service.deletedIds);
        assertEquals(2, data.asInt());
    }

    @Test
    void rejectsInvalidBatchArrays() throws Exception {
        var authorized = session();
        for (String body : List.of("null", "[]", "[null]", "[0]", "[1.5]", "[\"42\"]",
                "[9223372036854775808]", "{\"ids\":[42]}")) {
            error(mvc.perform(delete("/emps").session(authorized)
                    .contentType(MediaType.APPLICATION_JSON).content(body)), 400);
        }
    }

    @Test
    void bindsCombinedFiltersAndReturnsOnlyPublicPageFields() throws Exception {
        var data = ok(mvc.perform(get("/emps").session(session()).param("name", "张").param("gender", "1").param("dept", "研发部")
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
        var data = ok(mvc.perform(get("/emps").session(session())));
        assertEquals(new EmpQueryRequest(null, null, null, null, null), service.queried);
        assertEquals(1, data.get("page").asInt());
        assertEquals(10, data.get("size").asInt());
    }

    @Test
    void rejectsMissingBatchBodyAndUnconvertiblePage() throws Exception {
        var authorized = session();
        error(mvc.perform(delete("/emps").session(authorized)), 400);
        error(mvc.perform(get("/emps").session(authorized).param("page", "invalid")), 400);
    }

    @Test
    void sessionIsRequiredRotatedOnLoginAndInvalidatedOnLogout() throws Exception {
        var anonymous = mvc.perform(get("/emps"));
        error(anonymous, 401);
        assertNull(anonymous.andReturn().getRequest().getSession(false));
        error(mvc.perform(delete("/emps/42")), 401);

        var incorrect = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_JSON.replace("mvc-test-password", "incorrect-password")));
        error(incorrect, 401);
        assertNull(incorrect.andReturn().getRequest().getSession(false));

        var existing = new MockHttpSession(context.getServletContext());
        String originalId = existing.getId();
        ok(mvc.perform(post("/auth/login").session(existing).contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON)));
        assertNotEquals(originalId, existing.getId());
        assertEquals(1800, existing.getMaxInactiveInterval());
        ok(mvc.perform(get("/emps/42").session(existing)));
        ok(mvc.perform(post("/auth/logout").session(existing)));
        assertTrue(existing.isInvalid());
        error(mvc.perform(get("/emps/42")), 401);
    }

    @Test
    void validatesBodiesPathParametersAndPagination() throws Exception {
        var authorized = session();
        String invalid = EMPLOYEE_JSON.replace("张三", " ").replace("12345.67", "12.345")
                .replace("2024-02-29", LocalDate.now().plusDays(2).toString());
        String message = error(mvc.perform(post("/emps").session(authorized)
                .contentType(MediaType.APPLICATION_JSON).content(invalid)), 400);
        assertTrue(message.contains("empName") && message.contains("salary") && message.contains("hireDate"), message);
        error(mvc.perform(post("/emps").session(authorized).contentType(MediaType.APPLICATION_JSON).content("{")), 400);
        error(mvc.perform(post("/emps").session(authorized).contentType(MediaType.APPLICATION_JSON)
                .content(EMPLOYEE_JSON.replace("\"gender\":1", "\"gender\":1.5"))), 400);
        error(mvc.perform(get("/emps/0").session(authorized)), 400);
        error(mvc.perform(get("/emps").session(authorized).param("size", "101")), 400);
    }

    @Test
    void wrapsBusinessAndMvcErrorsWithoutLeakingInternalDetails() throws Exception {
        var authorized = session();
        assertEquals("员工不存在", error(mvc.perform(get("/emps/404").session(authorized)), 404));
        error(mvc.perform(get("/unknown")), 404);
        var unsupportedMethod = mvc.perform(post("/emps/42").session(authorized));
        error(unsupportedMethod, 405);
        String allow = unsupportedMethod.andReturn().getResponse().getHeader(HttpHeaders.ALLOW);
        assertTrue(allow != null && allow.contains("GET") && allow.contains("DELETE"), "405 应保留 Allow 响应头");
        error(mvc.perform(post("/emps").session(authorized).contentType(MediaType.TEXT_PLAIN).content("text")), 415);
        error(mvc.perform(get("/emps/409").session(authorized)), 409);
        assertEquals("服务器内部错误", error(mvc.perform(get("/emps/500").session(authorized)), 500));
    }

    @Test
    void unauthenticatedWritesAreRejectedBeforeBodyAndIdValidation() throws Exception {
        error(mvc.perform(post("/emps").contentType(MediaType.APPLICATION_JSON).content("{")), 401);
        error(mvc.perform(put("/emps").contentType(MediaType.APPLICATION_JSON).content("{}")), 401);
        error(mvc.perform(delete("/emps")), 401);
    }

    @Test
    void allowsLoginAndBatchDeletePreflightsWithoutCreatingSession() throws Exception {
        var endpoints = Map.of("/auth/login", "POST", "/emps", "DELETE");
        for (var endpoint : endpoints.entrySet()) {
            var result = mvc.perform(options(endpoint.getKey())
                    .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, endpoint.getValue())
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type")).andReturn();

            assertEquals(200, result.getResponse().getStatus());
            assertEquals(FRONTEND_ORIGIN, result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
            assertEquals("true", result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
            assertNull(result.getRequest().getSession(false));
        }
    }

    @Test
    void permitsActualCorsRequestsFromAllowedOriginsAndStillRequiresLogin() throws Exception {
        var anonymous = mvc.perform(get("/emps/42").header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN));
        error(anonymous, 401);
        assertEquals(FRONTEND_ORIGIN, anonymous.andReturn().getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        var authorized = session();
        var allowed = mvc.perform(get("/emps/42").session(authorized).header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN));
        assertEquals(42, ok(allowed).get("empId").asInt());
        assertEquals(FRONTEND_ORIGIN, allowed.andReturn().getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        var disallowed = mvc.perform(get("/emps/42").session(authorized)
                .header(HttpHeaders.ORIGIN, "https://unlisted.example.test")).andReturn().getResponse();
        assertEquals(403, disallowed.getStatus());
        assertNull(disallowed.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void servesClasspathStaticResourcesWithoutLogin() throws Exception {
        var result = mvc.perform(get("/ssm-emp-mis/static/probe.txt").contextPath("/ssm-emp-mis")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertEquals("static resource probe", result.getResponse().getContentAsString(StandardCharsets.UTF_8).strip());
        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void loginFailsClosedWhenCredentialsAreMissing() throws Exception {
        var sources = context.getEnvironment().getPropertySources();
        sources.addFirst(new MapPropertySource("missingAuthTest", Map.of("AUTH_PASSWORD", "")));
        try {
            var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON));
            error(login, 503);
            assertNull(login.andReturn().getRequest().getSession(false));
        } finally {
            sources.remove("missingAuthTest");
        }
    }

    private static MockHttpSession session() throws Exception {
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON));
        ok(login);
        return (MockHttpSession) login.andReturn().getRequest().getSession(false);
    }

    private static String error(ResultActions result, int status) throws Exception {
        var response = result.andReturn().getResponse();
        assertEquals(status, response.getStatus());
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(response.getContentType())));
        var body = JSON.readTree(response.getContentAsByteArray());
        assertEquals(Set.of("code", "message", "data"), fieldNames(body));
        assertEquals(status, body.get("code").asInt());
        assertTrue(body.get("data").isNull());
        return body.get("message").asString();
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
            if (id == 404L) {
                throw new BusinessException(BusinessException.Code.EMPLOYEE_NOT_FOUND, "员工不存在");
            }
            if (id == 409L) {
                throw new DataIntegrityViolationException("private JDBC constraint details");
            }
            if (id == 500L) {
                throw new IllegalStateException("private internal details");
            }
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
