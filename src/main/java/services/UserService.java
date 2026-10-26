package services;

import Database.UserRepository;
import org.bson.Document;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Lazy
public class UserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;

    public UserService(@Lazy UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResult getRole(String rawEmail) {
        String email = normalizeEmail(rawEmail);
//        if (!validEmail(email)) {
//            return failure(400, "A valid email is required");
//        }

        String role = userRepository.findRole(email);
        if (role == null) {
            return failure(404, "User not found");
        }

        return new UserResult(200, Map.of("email", email, "role", role));
    }

    public UserResult getUser(String rawEmail) {
        String email = normalizeEmail(rawEmail);
//        if (!validEmail(email)) {
//            return failure(400, "A valid email is required");
//        }

        Document user = userRepository.findUser(email);
        if (user == null) {
            return failure(404, "User not found");
        }

        return new UserResult(200, Map.of("user", safeUser(user)));
    }



    public UserResult getAllDoctors() {
        List<Map<String, Object>> doctors = new ArrayList<>();
        for (Document doctor : userRepository.getDoctors()) {
            doctors.add(safeUser(doctor));
        }
        return new UserResult(200, Map.of("doctors", doctors));
    }

    public static Map<String, Object> safeUser(Document user) {
        Map<String, Object> safe = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : user.entrySet()) {
            if (!entry.getKey().equals("password")
                    && !entry.getKey().equals("passwordHash")) {
                safe.put(entry.getKey(), entry.getValue());
            }
        }
        return safe;
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean validEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static UserResult failure(int status, String message) {
        return new UserResult(status, Map.of("message", message));
    }

    public record UserResult(int status, Map<String, Object> body) {}
}
