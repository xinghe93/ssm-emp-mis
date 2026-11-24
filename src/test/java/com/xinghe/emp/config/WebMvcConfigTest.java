package com.xinghe.emp.config;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class WebMvcConfigTest {

    @Test
    void mvcSerializesUtf8IsoDatesAndDecimalAmounts() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(WebMvcConfig.class, JsonProbe.class);
            context.refresh();

            var response = MockMvcBuilders.webAppContextSetup(context).build()
                    .perform(get("/serialization-probe"))
                    .andReturn().getResponse();

            assertEquals(200, response.getStatus());
            assertEquals("application/json", response.getContentType());
            String json = response.getContentAsString(StandardCharsets.UTF_8);
            assertTrue(json.contains("\"name\":\"测试员工\""), json);
            assertTrue(json.contains("\"hireDate\":\"2024-02-29\""), json);
            assertTrue(json.contains("\"salary\":12345.67"), json);
        }
    }

    @RestController
    static class JsonProbe {
        @GetMapping("/serialization-probe")
        public ProbePayload getSample() {
            return new ProbePayload("测试员工", LocalDate.of(2024, 2, 29), new BigDecimal("12345.67"));
        }
    }

    record ProbePayload(String name, LocalDate hireDate, BigDecimal salary) {
    }
}
