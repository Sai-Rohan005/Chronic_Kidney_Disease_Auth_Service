package Database.schema;

import java.time.LocalDate;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String role = "Patient";

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

    private LocalDate dob;

    @NotBlank
    private String phone;

    @NotBlank
    private String password;

    private Instant createdAt = Instant.now();

    public User() {}

}