package com.papamxzhet.filmio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Никнейм должен содержать не менее 3 и не более 50 символов")
    @Column(unique = true)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Неверный формат почты")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Пароль должен содержать не менее 6 символов")
    private String password;

    private String avatarUrl;

}
