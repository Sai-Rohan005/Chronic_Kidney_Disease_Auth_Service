package web;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class ApiErrorContractTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        mvc = webAppContextSetup(context).build();
    }
    @AfterEach void close() { context.close(); }

    @Test void serviceErrorUsesCommonShape() throws Exception {
        var result = mvc.perform(get("/probe/rejected"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Already exists"))
                .andExpect(jsonPath("$.path").value("/probe/rejected"))
                .andExpect(jsonPath("$.timestamp", not(emptyString())))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        String timestamp = com.jayway.jsonpath.JsonPath.read(json, "$.timestamp");
        assertDoesNotThrow(() -> Instant.parse(timestamp));
    }
    @Test void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(post("/probe/body").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/probe/body"));
    }
    @Test void missingHeaderIsBadRequest() throws Exception {
        mvc.perform(get("/probe/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/probe/header"));
    }
    @Test void methodNotAllowedKeepsStatusAndAllowHeader() throws Exception {
        mvc.perform(post("/probe/header"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));
    }
    @Test void explicitStatusIsPreserved() throws Exception {
        mvc.perform(get("/probe/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
    @Test void unexpectedErrorDoesNotExposeInternalDetails() throws Exception {
        mvc.perform(get("/probe/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("private database details"))));
    }
    @Test void successfulResponsesAreUnchanged() throws Exception {
        mvc.perform(post("/probe/body").contentType("application/json").content("{\"ok\":true}"))
                .andExpect(status().isOk()).andExpect(content().json("{\"ok\":true}"));
    }
    @Configuration
    @ComponentScan("services.web")
    @org.springframework.web.servlet.config.annotation.EnableWebMvc
    static class Config {
        @Bean ProbeController probeController() { return new ProbeController(); }
    }
    @RestController
    @RequestMapping("/probe")
    static class ProbeController {
        @GetMapping("/rejected") ResponseEntity<Map<String, Object>> rejected() {
            return ResponseEntity.status(409).body(Map.of("message", "Already exists"));
        }
        @PostMapping("/body") Map<String, Object> body(@RequestBody Map<String, Object> body) { return body; }
        @GetMapping("/header") Map<String, Object> header(@RequestHeader("Authorization") String token) {
            return Map.of("ok", true);
        }
        @GetMapping("/unauthorized") void unauthorized() {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        @GetMapping("/crash") void crash() { throw new IllegalStateException("private database details"); }
    }
}
