package security;

import com.ckd.auth.SecurityAutoConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.*;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

public abstract class SecurityTestSupport {
    protected AnnotationConfigWebApplicationContext context;
    protected MockMvc mvc;
    private final byte[] secret = new byte[32];
    private final Map<String, String> active = new ConcurrentHashMap<>();
    protected abstract Class<?> config();
    protected abstract String protectedPath();
    protected boolean authService() { return false; }
    @BeforeEach @SuppressWarnings("unchecked") void startSecurity() {
        new SecureRandom().nextBytes(secret);
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MockPropertySource()
                .withProperty("app.jwt.secret", Base64.getEncoder().encodeToString(secret))
                .withProperty("ckd.security.auth-service", String.valueOf(authService())));
        context.register(SharedConfig.class, config());
        context.refresh();
        StringRedisTemplate redis = context.getBean(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> active.get(call.getArgument(0)));
        mvc = webAppContextSetup(context).addFilters(context.getBeansOfType(Filter.class).values().toArray(Filter[]::new)).build();
    }
    @AfterEach void stopSecurity() { context.close(); org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    protected String bearer(String email, String role) { return bearer(email, role, Instant.now().plusSeconds(60), true); }
    private String bearer(String email, String role, Instant expiry, boolean valid) {
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder().subject(email).claim("role", role).id(jti)
                .issuedAt(Date.from(Instant.now().minusSeconds(120))).expiration(Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(secret)).compact();
        if (valid) active.put("auth:token:" + jti, token);
        return "Bearer " + token;
    }
    @Test void noTokenReturns401() throws Exception {
        mvc.perform(get(protectedPath())).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }
    @Test void expiredTokenReturns401() throws Exception {
        mvc.perform(get(protectedPath()).header("Authorization", bearer("owner@example.invalid", "Patient", Instant.now().minusSeconds(60), true)))
                .andExpect(status().isUnauthorized());
    }
    @Test void revokedTokenReturns401() throws Exception {
        mvc.perform(get(protectedPath()).header("Authorization", bearer("owner@example.invalid", "Patient", Instant.now().plusSeconds(60), false)))
                .andExpect(status().isUnauthorized());
    }
    @Configuration @EnableWebMvc @Import(SecurityAutoConfiguration.class) @ComponentScan("services.web")
    static class SharedConfig {
        @Bean StringRedisTemplate stringRedisTemplate() { return mock(StringRedisTemplate.class); }
    }
}
