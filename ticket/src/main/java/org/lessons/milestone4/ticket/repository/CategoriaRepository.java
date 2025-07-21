package org.lessons.milestone4.ticket.repository;

import org.lessons.milestone4.ticket.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    
}