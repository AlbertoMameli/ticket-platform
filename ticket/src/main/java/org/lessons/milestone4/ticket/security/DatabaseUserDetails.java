package org.lessons.milestone4.ticket.security;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.lessons.milestone4.ticket.model.Role;
import org.lessons.milestone4.ticket.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class DatabaseUserDetails implements UserDetails {
    private final Integer id;
    private final String username;//email
    private final String password;
    private final String nome;
    private final Set<GrantedAuthority> authorities;
//in questo costruttore prendiamo come oggetto User e inizializzziamo i campi
    public DatabaseUserDetails(User user){
        this.id = user.getId();
        this.username = user.getEmail();
        this.password = user.getPassword();
        this.nome = user.getNome();

        this.authorities = new HashSet<>();
        for (Role role : user.getRoles() ){
            //prendiamo la lista dei ruoli e andiamo a trasformarlo in un  nuovo oggetto di spring SimpleGrantedAuthority
            this.authorities.add(new SimpleGrantedAuthority(role.getNome()));
        }
    }
 
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    public Integer getId() {
        return this.id;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public String getNome() {
        return this.nome;
    }


    
}