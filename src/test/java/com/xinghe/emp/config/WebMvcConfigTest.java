package com.xinghe.emp.config;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebMvcConfigTest {

    @Test
    void jsonConverterSerializesUtf8IsoDatesAndDecimalAmounts() throws Exception {
        var builder = HttpMessageConverters.forServer();
        new WebMvcConfig(new MockEnvironment()).configureMessageConverters(builder);
        var converter = StreamSupport.stream(builder.build().spliterator(), false)
                .filter(JacksonJsonHttpMessageConverter.class::isInstance)
                .map(JacksonJsonHttpMessageConverter.class::cast)
                .findFirst()
                .orElseThrow();
        var output = new MockHttpOutputMessage();
        var payload = new ProbePayload("测试员工", LocalDate.of(2024, 2, 29), new BigDecimal("12345.67"));

        converter.write(payload, MediaType.APPLICATION_JSON, output);

        assertEquals(MediaType.APPLICATION_JSON, output.getHeaders().getContentType());
        String json = output.getBodyAsString(StandardCharsets.UTF_8);
        assertTrue(json.contains("\"name\":\"测试员工\""), json);
        assertTrue(json.contains("\"hireDate\":\"2024-02-29\""), json);
        assertTrue(json.contains("\"salary\":12345.67"), json);
    }

    record ProbePayload(String name, LocalDate hireDate, BigDecimal salary) {
    }
}
