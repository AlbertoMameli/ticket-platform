package org.lessons.milestone4.ticket.repository;

import java.util.Optional;

import org.lessons.milestone4.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    
}