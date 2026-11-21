package controllers;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import services.Authentication;
import services.JwtTokenService;
import services.UserService;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class AuthenticationErrorContractTest {
    @Test void interceptorRejectionUsesCommonErrorShape() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(Config.class);
            context.refresh();
            webAppContextSetup(context)
                    .build().perform(post("/auth/change-password").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Authentication required or token expired"))
                    .andExpect(jsonPath("$.path").value("/auth/change-password"));
        }
    }
    @Configuration
    @ComponentScan("services.web")
    @org.springframework.web.servlet.config.annotation.EnableWebMvc
    static class Config implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
        @Override public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
            registry.addInterceptor(new JwtAuthenticationInterceptor(mock(JwtTokenService.class)));
        }
        @Bean AuthController controller() { return new AuthController(mock(Authentication.class), mock(UserService.class)); }
    }
}
