package org.lessons.milestone4.ticket.controller.api;

import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.Categoria;
import org.lessons.milestone4.ticket.model.Stato;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.CategoriaRepository;
import org.lessons.milestone4.ticket.repository.StatoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController // Indico che questa classe espone API REST
@RequestMapping("/api/tickets") // Tutti gli endpoint partiranno con /api/tickets
public class TicketRestController {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private StatoRepository statoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    // -------------------------------
    // REST API 1: Elenco di tutti i ticket
    // -------------------------------
    @GetMapping
    public ResponseEntity<List<Ticket>> index() {
        List<Ticket> tickets = ticketRepository.findAll();

        // Se non ci sono ticket, restituisco NO_CONTENT (204)
        if (tickets.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // Altrimenti ritorno la lista con OK (200)
        return new ResponseEntity<>(tickets, HttpStatus.OK);
    }

    // -------------------------------
    // REST API 2: Singolo ticket per ID
    // -------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> show(@PathVariable Integer id) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);

        // Se non esiste un ticket con quell'ID, ritorno NOT_FOUND (404)
        if (ticketOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Altrimenti ritorno il ticket trovato
        return new ResponseEntity<>(ticketOpt.get(), HttpStatus.OK);
    }

    // -------------------------------
    // REST API 3: Ticket per categoria (ID categoria)
    // -------------------------------
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<List<Ticket>> getByCategoria(@PathVariable Integer categoriaId) {
        // Verifico che la categoria esista
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(categoriaId);

        if (categoriaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<Ticket> tickets = ticketRepository.findByCategoriaId(categoriaId);

        if (tickets.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(tickets, HttpStatus.OK);
    }

    // -------------------------------
    // REST API 4: Ticket per stato (ID stato)
    // -------------------------------
    @GetMapping("/stato/id/{id}")
    public ResponseEntity<List<Ticket>> getByStatoId(@PathVariable Integer id) {
        // Verifico che esista uno stato con quell’ID
        Optional<Stato> statoOpt = statoRepository.findById(id);

        if (statoOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<Ticket> tickets = ticketRepository.findByStatoId(id);

        if (tickets.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(tickets, HttpStatus.OK);
    }
}
