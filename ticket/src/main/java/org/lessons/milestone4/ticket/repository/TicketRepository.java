package org.lessons.milestone4.ticket.repository;

import java.util.Optional;

import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Optional<User> findByUserId(Integer id);
}
