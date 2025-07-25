package org.lessons.milestone4.ticket.api;


import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
public class TicketRestController {

    @Autowired
    private TicketRepository ticketRepository;

    // Metodo INDEX 
    @GetMapping
    public ResponseEntity<List<Ticket>> list(
            @RequestParam Optional<String> stato,
            @RequestParam Optional<Integer> categoriaId) {

        List<Ticket> tickets;

        if (stato.isPresent() && !stato.get().isEmpty()) {
            tickets = ticketRepository.findByStato_Valore(stato.get());
        } else if (categoriaId.isPresent()) {
            tickets = ticketRepository.findByCategoriaId(categoriaId.get());
        } else {
            tickets = ticketRepository.findAll();
        }

        if (tickets.isEmpty()) {
            // Se la lista è vuota, ritorna 204 No Content
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            // Altrimenti, ritorna la lista con 200 OK
            return new ResponseEntity<>(tickets, HttpStatus.OK);
        }
    }

    // Metodo SHOW (per un singolo ticket)
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getById(@PathVariable Integer id) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isPresent()) {
            // Se il ticket esiste, ritorna il ticket con 200 OK
            return new ResponseEntity<>(ticketOpt.get(), HttpStatus.OK);
        } else {
            // Altrimenti, ritorna 404 Not Found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
