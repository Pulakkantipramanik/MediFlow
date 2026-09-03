package com.mediflow.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // BUSINESS RULE:
// Each user must have a unique email address.
//
// WHY:
// The email is used for login and user identification.
// Two users should not share the same login email.
    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String role;
}