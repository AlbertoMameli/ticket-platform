package org.lessons.milestone4.ticket.controller;

import org.lessons.milestone4.ticket.model.Nota;
import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.NotaRepository;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Optional;

// Rimuoviamo @RequestMapping("/note") perché gestiremo rotte diverse
@Controller
public class NotaController {

    // Usiamo sempre constructor injection per le dipendenze
    private final NotaRepository notaRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public NotaController(
            NotaRepository notaRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository) {
        this.notaRepository = notaRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    // La creazione di una nota dipende da un ticket, quindi l'URL riflette questo
    @PostMapping("/tickets/{ticketId}/note")
    public String store(
            @PathVariable Integer ticketId,
            @Valid @ModelAttribute("nota") Nota formNota,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {

        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket non trovato");
        }
        Ticket ticket = ticketOpt.get();

        if (bindingResult.hasErrors()) {
            model.addAttribute("ticket", ticket);
            // AGGIUNTA: Ripopola la lista delle note per la vista
            model.addAttribute("note", notaRepository.findByTicketId(ticketId));
            return "tickets/show"; // Ora la pagina ha tutti i dati di cui ha bisogno
        }

        // 2. Trova l'utente che sta scrivendo la nota (l'autore)
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utente non autorizzato");
        }

        // 3. Imposta le relazioni e i dati mancanti
        formNota.setTicket(ticket);
        formNota.setAutore(userOpt.get());
        formNota.setDataCreazione(LocalDateTime.now());

        // 4. Salva la nota
        notaRepository.save(formNota);

        // 5. Reindirizza alla pagina del ticket
        return "redirect:/tickets/" + ticketId;
    }

    @GetMapping("/note/{id}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        Optional<Nota> notaOpt = notaRepository.findById(id);
        if (notaOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }
        model.addAttribute("nota", notaOpt.get());
        return "note/edit";
    }

    @PostMapping("/note/{id}")
    public String update(
            @PathVariable Integer id,
            @Valid @ModelAttribute("nota") Nota formNota,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "note/edit";
        }

        Optional<Nota> notaOpt = notaRepository.findById(id);
        if (notaOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }
        Nota notaDaSalvare = notaOpt.get();

        // Aggiorna solo i campi che l'utente può modificare
        notaDaSalvare.setTesto(formNota.getTesto());

        notaRepository.save(notaDaSalvare);

        // Reindirizza alla pagina del ticket a cui la nota appartiene
        return "redirect:/tickets/" + notaDaSalvare.getTicket().getId();
    }

    @PostMapping("/note/{id}/delete")
    public String delete(@PathVariable Integer id) {
        Optional<Nota> notaOpt = notaRepository.findById(id);
        if (notaOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }

        Integer ticketId = notaOpt.get().getTicket().getId();

        notaRepository.deleteById(id);

        return "redirect:/tickets/" + ticketId;
    }
}