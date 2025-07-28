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
@EnableWebSecurity // sto dicendo che queste istruzioni sono di Spring
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception { // gestisce le autorizzazioni
        http.authorizeHttpRequests(requests -> requests
                // REGOLE SPECIFICHE
                .requestMatchers(HttpMethod.POST, "/tickets/*/editStato").hasAnyAuthority("ADMIN", "OPERATORE")
                .requestMatchers(HttpMethod.POST, "/tickets/*/note").hasAnyAuthority("ADMIN", "OPERATORE")

                // L'ADMIN può accedere alle pagine di creazione e modifica
                .requestMatchers("/tickets/create", "/tickets/*/edit").hasAuthority("ADMIN")

                .requestMatchers(HttpMethod.POST, "/tickets/create", "/tickets/*/edit", "/tickets/*/delete")
                .hasAuthority("ADMIN")

                // REGOLE GENERALI
                .requestMatchers("/tickets", "/tickets/*", "/users/**", "/note/**").authenticated()// basta essersi
                                                                                                   // autenticato per
                                                                                                   // vedere queste
                                                                                                   // pagine
                .requestMatchers("/**").permitAll())// queste possono essere accessibili a tutti
                // ... dentro il tuo SecurityFilterChain per il web

                .formLogin(form -> form
                        .loginPage("/") // dice a Spring che il tuo  si trova alla homepage.
                        .loginProcessingUrl("/login")//usrname e password ti arrivano tramite url /login
                        .failureUrl("/?error=true") // se il login fallisce, torna alla homepage con
                                                    // un parametro di errore.
                        .permitAll() // Permetti a tutti di vedere la pagina di login.
                )

                // ... resto della configurazione (.logout(), etc.)
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
    PasswordEncoder passwordEncoder() {// codifica le password e non le mostra in chiaro
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}