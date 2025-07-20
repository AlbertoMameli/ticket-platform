package org.lessons.milestone4.ticket.security;

import java.util.Optional;

import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService  {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            return new DatabaseUserDetails(user.get());
        } else {
            throw new UsernameNotFoundException("Username not found");
        }
    }
}
