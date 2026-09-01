package com.mediflow.user.repository;

import com.mediflow.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    //→ email দিয়ে existing user খুঁজবে
    Optional<User> findByEmail(String email);

    //→ email already registered কিনা check করবে
    boolean existsByEmail(String email);
}
