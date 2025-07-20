package org.lessons.milestone4.ticket.repository;


import org.lessons.milestone4.ticket.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaRepository extends JpaRepository<Nota, Integer> {
    
}
