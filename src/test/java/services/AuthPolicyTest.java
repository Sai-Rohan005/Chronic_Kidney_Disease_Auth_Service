package services;
import Database.UserRepository;
import com.ckd.auth.JwtTokenService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthPolicyTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void weakRegistrationPasswordIsRejectedBeforeDatabaseAccess() {
        var repository = mock(UserRepository.class);
        var authentication = new Authentication(repository, mock(JwtTokenService.class));
        assertEquals(400, authentication.register(Map.of("name", "Patient", "email", "patient@example.invalid", "password", "short")).status());
        verifyNoInteractions(repository);
    }
    @Test void weakReplacementPasswordIsRejectedBeforeDatabaseAccess() {
        var repository = mock(UserRepository.class);
        var authentication = new Authentication(repository, mock(JwtTokenService.class));
        assertEquals(400, authentication.changePassword(Map.of("currentPassword", "old-password", "newPassword", "short"), "patient@example.invalid").status());
        verifyNoInteractions(repository);
    }
    @Test void emailLookupCannotReadAnotherUser() {
        var repository = mock(UserRepository.class);
        var service = new UserService(repository);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("patient@example.invalid", "token",
                List.of(new SimpleGrantedAuthority("ROLE_Patient"))));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.getUser("other@example.invalid")).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.getRole("other@example.invalid")).getStatusCode().value());
        verifyNoInteractions(repository);
    }
    @Test void passwordLengthBoundariesAreEnforced() {
        var repository = mock(UserRepository.class);
        var authentication = new Authentication(repository, mock(JwtTokenService.class));
        assertEquals(201, authentication.register(Map.of("name", "Patient", "email", "patient@example.invalid", "password", "a".repeat(12))).status());
        verify(repository).createUser("Patient", "patient@example.invalid", "a".repeat(12));
        clearInvocations(repository);
        for (String password : java.util.List.of("a".repeat(73), "界".repeat(25))) {
            assertEquals(400, authentication.register(Map.of("name", "Patient", "email", "patient@example.invalid", "password", password)).status());
        }
        verifyNoInteractions(repository);
    }
    @Test void doctorDirectoryReturnsOnlyBookingFields() {
        var repository = mock(UserRepository.class);
        when(repository.getDoctors()).thenReturn(java.util.List.of(new org.bson.Document("email", "doctor@example.invalid")
                .append("name", "Doctor").append("passwordHash", "not-a-real-hash").append("private_notes", "private")));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("patient@example.invalid", "token",
                List.of(new SimpleGrantedAuthority("ROLE_Patient"))));
        var result = new UserService(repository).getAllDoctors();
        assertEquals(java.util.List.of(Map.of("email", "doctor@example.invalid", "name", "Doctor")), result.body().get("doctors"));
    }
}
