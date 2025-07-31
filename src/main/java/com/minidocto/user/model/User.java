package com.minidocto.user.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import com.minidocto.user.model.Role;

/**
 * Represents a user in the system (doctor or patient).
 * Doctors have role PRO and availability slots.
 */
@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String id;
    private String name;
    private String email;
    private String password;
    private Role role;
    private Integer score;
    private List<String> tokens; // Optional: for refresh tokens
    private String specialty; // for PRO users only
    private List<String> appointments; // for USER only

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
} 