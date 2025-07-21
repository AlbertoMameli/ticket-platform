package org.lessons.milestone4.ticket.repository;

import org.lessons.milestone4.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    List<Ticket> findByTitoloContainingIgnoreCase(String titolo);

    List<Ticket> findByOperatoreId(Integer operatoreId);

    List<Ticket> findByStato(String stato);

    List<Ticket> findByCategoriaId(Integer categoriaId);

    
    
    Integer countByOperatoreIdAndStatoNot(Integer operatoreId, String stato);

}