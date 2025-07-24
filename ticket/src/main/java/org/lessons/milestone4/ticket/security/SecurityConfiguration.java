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
                // REGOLE SPECIFICHE
                .requestMatchers(HttpMethod.POST, "/tickets/*/editStato").hasAnyAuthority("ADMIN", "OPERATORE")
                .requestMatchers(HttpMethod.POST, "/tickets/*/note").hasAnyAuthority("ADMIN","OPERATORE")

                // L'ADMIN può accedere alle pagine di creazione e modifica
                .requestMatchers("/tickets/create", "/tickets/*/edit").hasAuthority("ADMIN")

                .requestMatchers(HttpMethod.POST, "/tickets/create", "/tickets/*/edit", "/tickets/*/delete")
                .hasAuthority("ADMIN")

                // REGOLE GENERALI (rimangono uguali)
                .requestMatchers("/tickets", "/tickets/*", "/users/**", "/note/**").authenticated()
                .requestMatchers("/**").permitAll())
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable());

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