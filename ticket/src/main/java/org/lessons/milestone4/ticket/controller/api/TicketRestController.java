package org.lessons.milestone4.ticket.controller.api;

import org.lessons.milestone4.ticket.model.Ticket;
//import org.lessons.milestone4.ticket.repository.CategoriaRepository;
//import org.lessons.milestone4.ticket.repository.StatoRepository;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
public class TicketRestController {

    // Mi faccio "iniettare" il repository per parlare con il database.
    @Autowired
    private TicketRepository ticketRepository;
   // @Autowired
  //  private StatoRepository statoRepository;
   // @Autowired
 //   private CategoriaRepository categoriaRepository;
    
    /**
     * Metodo per ottenere la lista di tutti i ticket.
     * Accetta filtri opzionali per 'stato' e 'categoriaId'.
     */
    @GetMapping
    public ResponseEntity<List<Ticket>> index(
            @RequestParam Optional<String> stato,
            @RequestParam Optional<Integer> categoriaId) {

        List<Ticket> tickets;

        // Applichiamo i filtri se presenti, altrimenti prendiamo tutti i ticket.
        if (stato.isPresent() && !stato.get().isEmpty()) {
            tickets = ticketRepository.findByStato_Valore(stato.get());
        } else if (categoriaId.isPresent()) {
            tickets = ticketRepository.findByCategoriaId(categoriaId.get());
        } else {
            tickets = ticketRepository.findAll();
        }

        // Se la lista è vuota, restituisco uno stato '204 No Content',
        // che è semanticamente più corretto di una lista vuota con '200 OK'.
        if (tickets.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // Se ci sono risultati, restituisco la lista con '200 OK'.
        return new ResponseEntity<>(tickets, HttpStatus.OK);
    }

    /**
     * Metodo per ottenere i dettagli di un singolo ticket tramite il suo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> get(@PathVariable Integer id) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);

        if (ticketOpt.isPresent()) {
            // Se trovo il ticket, lo restituisco con '200 OK'.
            return new ResponseEntity<>(ticketOpt.get(), HttpStatus.OK);
        } else {
            // Se non lo trovo, restituisco '404 Not Found'.
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Metodo per creare un nuovo ticket.
     * Riceve i dati del ticket in formato JSON nel corpo della richiesta.
     */
    @PostMapping
    public ResponseEntity<Ticket> create(@Valid @RequestBody Ticket ticket) {
        // Salvo il nuovo ticket che ho ricevuto. L'ID verrà generato automaticamente.
        Ticket savedTicket = ticketRepository.save(ticket);
        
        // Restituisco il ticket appena creato con lo stato '201 Created',
        // che è lo standard REST per la creazione di una risorsa.
        return new ResponseEntity<>(savedTicket, HttpStatus.CREATED);
    }

    /**
     * Metodo per aggiornare un ticket esistente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Ticket> update(@Valid @RequestBody Ticket ticket, @PathVariable Integer id) {
        // Prima di tutto, verifico che il ticket che si vuole modificare esista.
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);

        if (ticketOpt.isPresent()) {
            // Se esiste, imposto l'ID sull'oggetto ricevuto per essere sicuro
            // di aggiornare quello giusto e non crearne uno nuovo.
            ticket.setId(id);
            Ticket updatedTicket = ticketRepository.save(ticket);
            
            // Restituisco il ticket aggiornato con '200 OK'.
            return new ResponseEntity<>(updatedTicket, HttpStatus.OK);
        } else {
            // Se non esiste, restituisco '404 Not Found'.
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Metodo per eliminare un ticket.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        // Verifico che il ticket da eliminare esista.
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);

        if (ticketOpt.isPresent()) {
            // Se esiste, lo elimino.
            ticketRepository.delete(ticketOpt.get());
            
            // Restituisco '204 No Content' per confermare l'avvenuta eliminazione.
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            // Se non esiste, restituisco '404 Not Found'.
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}