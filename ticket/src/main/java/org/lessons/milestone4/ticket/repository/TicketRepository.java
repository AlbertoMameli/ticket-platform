// org/lessons/milestone4/ticket/repository/TicketRepository.java

package org.lessons.milestone4.ticket.repository;

import org.lessons.milestone4.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    List<Ticket> findByOperatoreId(Integer operatoreId);

    //  Cerca tramite il campo 'valore' dell'entità Stato
    List<Ticket> findByStato_Valore(String statoValore);

    List<Ticket> findByCategoriaId(Integer categoriaId);

    // Conta tramite il campo 'valore' dell'entità Stato
    Integer countByOperatoreIdAndStato_ValoreNot(Integer operatoreId, String statoValore);

    List<Ticket> findByOperatoreIdAndTitoloContainingIgnoreCase(Integer operatoreId, String keyword);

    List<Ticket> findByTitoloContainingIgnoreCase(String keyword);
}