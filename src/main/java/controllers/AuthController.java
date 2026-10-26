package controllers;

import services.Authentication;
import services.Authentication.AuthResult;
import services.UserService;
import services.UserService.UserResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final Authentication authentication;
    private final UserService userService;

    public AuthController(Authentication authentication, UserService userService) {
        this.authentication = authentication;
        this.userService = userService;
    }

    @PostMapping("/getRole")
    public ResponseEntity<Map<String, Object>> getRole(
            @RequestBody Map<String, Object> body
    ) {
        String email=(String) body.get("email");
        return response(userService.getRole(email));
    }
    @GetMapping("/getRoleByToken")
    public ResponseEntity<Map<String, Object>> getRoleByToken(
            @RequestHeader("Authorization") String Token
    ){
        return response(authentication.getRoleByToken(Token));
    }
    @PostMapping ("/getUser")
    public ResponseEntity<Map<String, Object>> getUser(
            @RequestBody Map<String, Object> body
    ) {
        String email=(String) body.get("email");
        return response(userService.getUser(email));
    }
    @GetMapping("/getUserByToken")
    public ResponseEntity<Map<String, Object>> getUserByToken(
            @RequestHeader("Authorization") String token
    ) {
        return response(authentication.getUserByToken(token));
    }

    @GetMapping("/getAllDoctors")
    public ResponseEntity<Map<String, Object>> getAllDoctors() {
        return response(userService.getAllDoctors());
    }

    @GetMapping("/getUserAndRole")
    public ResponseEntity<Map<String,Object>> getUserAndRole(
            @RequestHeader("Authorization") String token
    ){
        return response(authentication.getUserAndRole(token));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, Object> body
    ) {
        return response(authentication.login(body));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, Object> body
    ) {
        return response(authentication.register(body));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        String authenticatedEmail = (String) request.getAttribute(
                JwtAuthenticationInterceptor.AUTHENTICATED_EMAIL_ATTRIBUTE);
        return response(authentication.changePassword(body, authenticatedEmail));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return response(authentication.logout(authorizationHeader));
    }

    private static ResponseEntity<Map<String, Object>> response(AuthResult result) {
        return ResponseEntity.status(result.status()).body(result.body());
    }

    private static ResponseEntity<Map<String, Object>> response(UserResult result) {
        return ResponseEntity.status(result.status()).body(result.body());
    }
}