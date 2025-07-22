package org.lessons.milestone4.ticket.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lessons.milestone4.ticket.model.Nota;
import org.lessons.milestone4.ticket.model.Role;
import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.CategoriaRepository;
import org.lessons.milestone4.ticket.repository.NotaRepository;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    NotaRepository notaRepository;

    @Autowired
    CategoriaRepository categoriaRepository;

    // Metodo che ritorna una lista di utenti a cui è possibile assegnare un ticket.

    private List<User> getUtentiAssegnabiliDisponibili() {
        List<User> utentiDisponibili = new ArrayList<>();
        List<User> tuttiGliUtenti = userRepository.findAll();

        // Ciclo esterno: itera su ogni utente del database
        for (User utente : tuttiGliUtenti) {

            // Controlliamo i ruoli dell'utente con un ciclo 'for' annidato
            boolean haRuoloAssegnabile = false;
            for (Role ruolo : utente.getRoles()) {
                if (ruolo.getNome().equals("OPERATORE") || ruolo.getNome().equals("ADMIN")) {
                    haRuoloAssegnabile = true;
                    break;
                }
            }

            // Se l'utente ha il ruolo giusto ED è anche disponibile, lo aggiungiamo
            if (haRuoloAssegnabile && utente.isDisponibile()) {
                utentiDisponibili.add(utente);
            }
        }
        return utentiDisponibili;
    }

    /**
     * Metodo che popola il Model con dati comuni a più form (create, edit, etc.).
     * Viene eseguito prima di ogni richiesta a questo controller, riducendo il
     * codice duplicato.
     */
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("users", getUtentiAssegnabiliDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());
    }

    @GetMapping
    public String index(
            Model model,
            Authentication authentication,
            @RequestParam(name = "q", required = false) String keyword) {
        Optional<User> userOptional = userRepository.findByEmail(authentication.getName());
        if (userOptional.isEmpty()) {
            return "redirect:/logout";
        }
        User utenteLoggato = userOptional.get();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        List<Ticket> tickets;

        // Se l'utente ha inserito una parola chiave per la ricerca...
        if (keyword != null && !keyword.isEmpty()) {
            if (isAdmin) {
                // L'admin cerca su tutti i ticket
                tickets = ticketRepository.findByTitoloContainingIgnoreCase(keyword);
            } else {
                // L'operatore cerca solo tra i propri ticket
                tickets = ticketRepository.findByOperatoreIdAndTitoloContainingIgnoreCase(utenteLoggato.getId(),
                        keyword);
            }
        } else {
            // Altrimenti (nessuna ricerca), carica la lista normale
            if (isAdmin) {
                tickets = ticketRepository.findAll();
            } else {
                tickets = ticketRepository.findByOperatoreId(utenteLoggato.getId());
            }
        }

        model.addAttribute("tickets", tickets);
        // Passiamo la keyword alla view per riempire il campo di ricerca
        model.addAttribute("keyword", keyword);

        return "tickets/index";
    }

    @GetMapping("/{id}")
    public String show(Model model, @PathVariable Integer id) {
        Optional<Ticket> ticketOptional = ticketRepository.findById(id);
        if (ticketOptional.isEmpty()) {
            return "error/404";
        }

        model.addAttribute("ticket", ticketOptional.get());
        model.addAttribute("note", notaRepository.findByTicketId(id));
        return "tickets/show";
    }

    @GetMapping("/create")
    public String create(Model model) {
        Ticket ticket = new Ticket();
        ticket.setDataCreazione(LocalDateTime.now());
        model.addAttribute("ticket", ticket);
        return "tickets/create";
    }

    @PostMapping
    public String store(@Valid @ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // Non serve ripopolare il model, lo fa già addCommonAttributes
            return "tickets/create";
        }
        // creoil mio ticket con il valore di defoult da fare
        formTicket.setStato("Da fare");
        ticketRepository.save(formTicket);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/edit")
    public String edit(Model model, @PathVariable Integer id) {
        model.addAttribute("ticket", ticketRepository.findById(id).orElseThrow());
        // Le liste 'users' e 'categorie' sono già aggiunte da addCommonAttributes
        return "tickets/edit";
    }

    @PostMapping("/{id}")
    public String update(@Valid @ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // Non serve ripopolare il model, lo fa già addCommonAttributes
            return "tickets/edit";
        }
        ticketRepository.save(formTicket);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/editStato")
    public String editStato(Model model, @PathVariable Integer id) {
        model.addAttribute("ticket", ticketRepository.findById(id).orElseThrow());
        return "tickets/editStato";
    }

    @PostMapping("/{id}/editStato")
    public String updateStato(@Valid @ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "tickets/editStato";
        }
        ticketRepository.save(formTicket);
        return "redirect:/tickets/{id}";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id) {
        ticketRepository.deleteById(id);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/nota")
    public String nota(@PathVariable Integer id, Model model, Authentication authentication) {
        Optional<Ticket> ticketOptional = ticketRepository.findById(id);
        if (ticketOptional.isEmpty()) {
            return "error/404";
        }
        Ticket ticket = ticketOptional.get();

        Nota nota = new Nota();
        nota.setTicket(ticket);

        userRepository.findByEmail(authentication.getName()).ifPresent(nota::setAutore);

        model.addAttribute("ticket", ticket);
        model.addAttribute("nota", nota);
        return "note/create";
    }
}