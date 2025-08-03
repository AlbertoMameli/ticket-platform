package org.lessons.milestone4.ticket.controller;

import org.lessons.milestone4.ticket.model.Nota;
import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.NotaRepository;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;


@Controller
public class NotaController {

    // Mi preparo i miei "attrezzi" per parlare con il database.
    @Autowired
    private NotaRepository notaRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/tickets/{ticketId}/note")
    public String store(@PathVariable Integer ticketId, @RequestParam("testo") String testoNota,
            Authentication authentication, RedirectAttributes redirectAttributes) {

        //  Trovo il ticket a cui associare la nota 
        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);
        if (optionalTicket.isEmpty()) {
            // Se il ticket non esiste, non posso aggiungere una nota. Blocco tutto.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket non trovato");
        }
        Ticket ticket = optionalTicket.get();

        if (testoNota == null || testoNota.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("erroreNota", "Il testo della nota non può essere vuoto.");
            // Reindirizzo l'utente alla stessa pagina da cui è venuto.
            return "redirect:/tickets/" + ticketId;
        }

        // Se la validazione è andata a buon fine, creo il nuovo oggetto Nota.
        Nota nuovaNota = new Nota();
        nuovaNota.setTesto(testoNota); // Imposto il testo.

        // Devo trovare chi è l'autore della nota.
        Optional<User> optionalAutore = userRepository.findByEmail(authentication.getName());
        if (optionalAutore.isEmpty()) {
            // Non dovrebbe mai succedere se l'utente è loggato, ma è un controllo di
            // sicurezza.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autore non valido");
        }
        User autore = optionalAutore.get();

        // Collego tutti i pezzi: la nota al suo ticket e al suo autore.
        nuovaNota.setTicket(ticket);
        nuovaNota.setAutore(autore);
        nuovaNota.setDataCreazione(LocalDateTime.now()); // La data la imposto io dal server.

        // Salvo la nota nel database.
        notaRepository.save(nuovaNota);

        return "redirect:/tickets/" + ticketId;
    }

    @GetMapping("/note/{id}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        // Cerco la nota da modificare nel database.
        Optional<Nota> optionalNota = notaRepository.findById(id);
        if (optionalNota.isEmpty()) {
            // Se non la trovo, lancio un errore 404.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }
        Nota nota = optionalNota.get();

        // Passo la nota al model, così il form sarà pre-compilato.
        model.addAttribute("nota", nota);
        return "note/edit"; // Mostro la pagina di modifica.
    }

    @PostMapping("/note/{id}") 
    public String update(@PathVariable Integer id, @ModelAttribute("nota") Nota formNota,
            BindingResult bindingResult) {

        // Cerco la versione "reale" della nota nel database.
        Optional<Nota> optionalNota = notaRepository.findById(id);
        if (optionalNota.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota da aggiornare non trovata");
        }
        Nota notaDaAggiornare = optionalNota.get();

        // Faccio la validazione del testo.
        if (formNota.getTesto() == null || formNota.getTesto().trim().isEmpty()) {
            // Se il testo è vuoto, aggiungo un errore al BindingResult.
            bindingResult.addError(new FieldError("nota", "testo", "Il testo non può essere vuoto"));
        }

        // Se ci sono errori...
        if (bindingResult.hasErrors()) {
            formNota.setTicket(notaDaAggiornare.getTicket());
            return "note/edit"; // Ritorno alla pagina di modifica per mostrare l'errore.
        }

        // Se è tutto OK, aggiorno solo il testo della nota che ho recuperato dal DB.
        notaDaAggiornare.setTesto(formNota.getTesto());

        // Salvo le modifiche.
        notaRepository.save(notaDaAggiornare);

        // Reindirizzo l'utente alla pagina del ticket a cui appartiene la nota.
        return "redirect:/tickets/" + notaDaAggiornare.getTicket().getId();
    }

    @PostMapping("/note/{id}/delete")
    public String delete(@PathVariable Integer id) {

        Optional<Nota> optionalNota = notaRepository.findById(id);
        if (optionalNota.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota da cancellare non trovata");
        }
        Nota nota = optionalNota.get();

        Integer ticketId = nota.getTicket().getId();

        // Cancello la nota.
        notaRepository.deleteById(id);

        // Reindirizzo alla pagina del ticket.
        return "redirect:/tickets/" + ticketId;
    }
}