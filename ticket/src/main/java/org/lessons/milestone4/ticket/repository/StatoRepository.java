package org.lessons.milestone4.ticket.repository;


import java.util.Optional;

import org.lessons.milestone4.ticket.model.Stato;
import org.springframework.data.jpa.repository.JpaRepository;





public interface StatoRepository extends JpaRepository <Stato, Integer> {
     Optional<Stato> findByValore(String valore);
     
}