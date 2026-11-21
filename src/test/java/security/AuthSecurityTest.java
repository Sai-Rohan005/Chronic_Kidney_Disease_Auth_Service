package security;
import controllers.AuthController;
import Database.UserRepository;
import com.ckd.auth.JwtTokenService;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import services.Authentication;
import services.UserService;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class AuthSecurityTest extends SecurityTestSupport {
    @Override protected Class<?> config() { return Config.class; }
    @Override protected String protectedPath() { return "/auth/getUserByToken"; }
    @Override protected boolean authService() { return true; }
    @Test void emailLookupCannotEnumerateOtherUsers() throws Exception {
        for (String path : java.util.List.of("/auth/getUser", "/auth/getRole")) {
            mvc.perform(post(path).contentType("application/json").content("{\"email\":\"other@example.invalid\"}")
                    .header("Authorization", bearer("owner@example.invalid", "Patient"))).andExpect(status().isForbidden());
        }
        verify(context.getBean(UserRepository.class), never()).findUser("other@example.invalid");
    }
    @Test void doctorDirectoryIsNotPublic() throws Exception {
        mvc.perform(get("/auth/getAllDoctors")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/getAllDoctors").header("Authorization", bearer("owner@example.invalid", "Patient"))).andExpect(status().isOk());
    }
    @Test void passwordChangeUsesPrincipalAndIgnoresBodyEmail() throws Exception {
        mvc.perform(post("/auth/change-password").contentType("application/json")
                .content("{\"email\":\"other@example.invalid\",\"currentPassword\":\"invalid-password\",\"newPassword\":\"a-long-new-password\"}")
                .header("Authorization", bearer("owner@example.invalid", "Patient"))).andExpect(status().isUnauthorized());
        verify(context.getBean(UserRepository.class)).findUser("owner@example.invalid");
        verify(context.getBean(UserRepository.class), never()).findUser("other@example.invalid");
    }
    @Configuration static class Config {
        @Bean UserRepository repository() {
            var repo = mock(UserRepository.class);
            when(repo.findUser("owner@example.invalid")).thenReturn(new Document("email", "owner@example.invalid")); return repo;
        }
        @Bean Authentication authentication(UserRepository repo, JwtTokenService tokens) { return new Authentication(repo, tokens); }
        @Bean UserService users(UserRepository repo) { return new UserService(repo); }
        @Bean AuthController controller(Authentication authentication, UserService users) { return new AuthController(authentication, users); }
    }
}
