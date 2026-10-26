package services;

import Database.UserRepository;
import com.mongodb.MongoWriteException;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

import static services.UserService.safeUser;

@Service
@Lazy
public class Authentication {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public Authentication(
            @Lazy UserRepository userRepository,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    public AuthResult login(Map<String, Object> body) {
        String email = normalizeEmail(body.get("email"));
        String password = value(body.get("password"));

        if (!validEmail(email) || password == null || password.isBlank()) {
            return failure(400, "A valid email and password are required");
        }

        Document user = userRepository.findUser(email);
        if (user == null || !passwordMatches(password, user.getString("passwordHash"))) {
            return failure(401, "Invalid email or password");
        }

        Map<String, Object> userDetails = new LinkedHashMap<>();
        userDetails.put("name", user.getString("name"));
        userDetails.put("email", user.getString("email"));
        userDetails.put("role", user.getString("role"));
        JwtTokenService.IssuedToken issuedToken = jwtTokenService.issue(
                email, user.getString("role"));

        return new AuthResult(200, Map.of(
                "message", "Login successful",
                "token", issuedToken.value(),
                "tokenType", "Bearer",
                "expiresAt", issuedToken.expiresAt().toString(),
                "user", userDetails
        ));
    }

    public AuthResult register(Map<String, Object> body) {
        String name = value(body.get("name"));
        String email = normalizeEmail(body.get("email"));
        String password = value(body.get("password"));

        if (name == null || name.isBlank()
                || !validEmail(email)
                || password == null || password.isBlank()) {
            return failure(400, "Name, a valid email, and password are required");
        }

        if (userRepository.findUser(email) != null) {
            return failure(409, "An account with this email already exists");
        }

        try {
            userRepository.createUser(name.trim(), email, password);
        } catch (MongoWriteException exception) {
            if (exception.getError().getCode() == 11000) {
                return failure(409, "An account with this email already exists");
            }
            throw exception;
        }

        return new AuthResult(201, Map.of("message", "Registration successful"));
    }

    public AuthResult getRoleByToken(String authorizationHeader) {

        String token = JwtTokenService.bearerToken(authorizationHeader);

        if (token == null) {
            return failure(404,"Token Expired");
        }

        Optional<JwtTokenService.AuthenticatedUser> user =
                jwtTokenService.authenticate(token);

        if (user.isEmpty()) {
            return failure(404,"Invalid or expired token");
        }

        String role = user.get().role();
        String email = user.get().email();
        Map<String,Object> mp=new HashMap<>();
        mp.put("email",email);
        mp.put("role",role);

        return new AuthResult(200 ,mp);
    }

    public UserService.UserResult getUserByToken(String authorizationHeader){
        String token = JwtTokenService.bearerToken(authorizationHeader);

        if (token == null) {
            return UserService.failure(404,"Token Expired");
        }

        Optional<JwtTokenService.AuthenticatedUser> user =
                jwtTokenService.authenticate(token);

        if (user.isEmpty()) {
            return UserService.failure(404,"Invalid or expired token");
        }

        String email = user.get().email();
        Document doc = userRepository.findUser(email);
        if (doc == null) {
            return UserService.failure(404, "User not found");
        }

        return new UserService.UserResult(200, Map.of("user", safeUser(doc)));
    }

    public AuthResult changePassword(
            Map<String, Object> body,
            String authenticatedEmail
    ) {
        String email = normalizeEmail(authenticatedEmail);
        String currentPassword = value(body.get("currentPassword"));
        String newPassword = value(body.get("newPassword"));

        if (!validEmail(email)
                || currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            return failure(400,
                    "A valid email, currentPassword, and newPassword are required");
        }

        Document user = userRepository.findUser(email);
        if (user == null || !passwordMatches(currentPassword, user.getString("passwordHash"))) {
            return failure(401, "Invalid email or current password");
        }

        if (currentPassword.equals(newPassword)) {
            return failure(400, "The new password must differ from the current password");
        }

        if (!userRepository.updatePassword(email, newPassword)) {
            return failure(401, "Invalid email or current password");
        }

        return new AuthResult(200, Map.of("message", "Password changed successfully"));
    }

    public AuthResult getUserAndRole(String authorizationHeader){
        String token = JwtTokenService.bearerToken(authorizationHeader);

        if (token == null) {
            return failure(404,"Token Expired");
        }

        Optional<JwtTokenService.AuthenticatedUser> user =
                jwtTokenService.authenticate(token);

        if (user.isEmpty()) {
            return failure(404,"Invalid or expired token");
        }

        String role = user.get().role();
        String email = user.get().email();
        Map<String,Object> mp=new HashMap<>();
        mp.put("email",email);
        mp.put("role",role);

        return new AuthResult(200 ,mp);
    }

    public AuthResult logout(String authorizationHeader) {
        String token = JwtTokenService.bearerToken(authorizationHeader);
        if (token == null || !jwtTokenService.revoke(token)) {
            return failure(401, "Invalid or expired token");
        }
        return new AuthResult(200, Map.of("message", "Logout successful"));
    }

    private static boolean passwordMatches(String password, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String value(Object value) {
        return value instanceof String string ? string : null;
    }

    private static String normalizeEmail(Object value) {
        String email = value(value);
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean validEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private static AuthResult failure(int status, String message) {
        return new AuthResult(status, Map.of("message", message));
    }

    public record AuthResult(int status, Map<String, Object> body) {}
}
