package org.lessons.milestone4.ticket.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // questa non è una classe normale, ma una classe di configurazione..
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                // REGOLE SPECIFICHE
                .requestMatchers(HttpMethod.POST, "/tickets/*/editStato").hasAnyAuthority("ADMIN", "OPERATORE")
                .requestMatchers(HttpMethod.POST, "/tickets/*/note").hasAnyAuthority("ADMIN", "OPERATORE")

                // L'ADMIN può accedere alle pagine di creazione e modifica
                .requestMatchers("/tickets/create", "/tickets/*/edit").hasAuthority("ADMIN")

                .requestMatchers(HttpMethod.POST, "/tickets/create", "/tickets/*/edit", "/tickets/*/delete")
                .hasAuthority("ADMIN")

                // REGOLE GENERALI
                .requestMatchers("/tickets", "/tickets/*", "/users/**", "/note/**").authenticated()// basta essersiautenticato per vedere queste pagine
                .requestMatchers("/**").permitAll())
                //relativo al login
                .formLogin(form -> form
                        .loginPage("/") // login i trova  si trova alla homepage.
                        .loginProcessingUrl("/login")
                        .failureUrl("/?error=true") // se il login fallisce, torna alla homepage con un parametro di errore.
                        .permitAll() 
                )
                .logout(logout -> logout.logoutSuccessUrl("/"))// dopo il logout torniamo alla homepage
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
        return new DatabaseUserDetailsService();//recuperiamo l'utente
    }

    @Bean
    PasswordEncoder passwordEncoder() {//codifico la password
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}