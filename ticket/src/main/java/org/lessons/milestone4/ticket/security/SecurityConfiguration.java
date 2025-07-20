package org.lessons.milestone4.ticket.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/tickets/create", "/tickets/*/edit").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/tickets/**", "/tickets/*/delete").hasAuthority("ADMIN")                
                .requestMatchers("/tickets", "/tickets/*").hasAnyAuthority("OPERATORE", "ADMIN")
                .requestMatchers("/**").permitAll()
            )
                .formLogin(Customizer.withDefaults());
                return http.build();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider() { 
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    DatabaseUserDetailsService userDetailsService() {
        return new DatabaseUserDetailsService();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();                                                                    
    }

}