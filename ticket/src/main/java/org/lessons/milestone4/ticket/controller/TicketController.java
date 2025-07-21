package org.lessons.milestone4.ticket.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lessons.milestone4.ticket.model.Nota;
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

    @GetMapping
    public String index(Model model, Authentication authentication) {
        Optional<User> userOptional = userRepository.findByEmail(authentication.getName());
        if (userOptional.isEmpty()) {
            // Gestisci il caso in cui l'utente non viene trovato, magari con un errore
            return "redirect:/logout";
        }
        User utenteLoggato = userOptional.get();

        // Controlla se l'utente ha il ruolo di ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        List<Ticket> tickets;
        if (isAdmin) {
            // Se è ADMIN, prende tutti i ticket
            tickets = ticketRepository.findAll();
        } else {
            // Se è OPERATORE, usa il metodo efficiente per trovare solo i suoi ticket
            tickets = ticketRepository.findByOperatoreId(utenteLoggato.getId());
        }

        model.addAttribute("tickets", tickets);
        return "tickets/index";
    }

    @GetMapping("/{id}")
    public String show(Model model, @PathVariable Integer id) {
        // Trova il ticket, gestendo il caso in cui non esista
        Optional<Ticket> ticketOptional = ticketRepository.findById(id);
        if (ticketOptional.isEmpty()) {
            // Se il ticket non esiste mostra pagina errore da creare dopo
            return "error/404";
        }

        model.addAttribute("ticket", ticketOptional.get());
        // Carica solo le note di QUESTO ticket
        model.addAttribute("note", notaRepository.findByTicketId(id));
        return "tickets/show";
    }

    @GetMapping("/create")
    public String create(Model model) {
        Ticket ticket = new Ticket();
        ticket.setDataCreazione(LocalDateTime.now());

        model.addAttribute("ticket", ticket);
        // Chiama il nostro nuovo metodo!
        model.addAttribute("users", getOperatoriDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());
        // Aggiungi anche categorie e stati se ti servono nel form
        // model.addAttribute("categorie", categoriaRepository.findAll());
        return "tickets/create";
    }

    @PostMapping
    public String store(@Valid @ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", getOperatoriDisponibili());
            return "tickets/create";
        }
        ticketRepository.save(formTicket);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/edit")
    public String edit(Model model, @PathVariable Integer id) {
        model.addAttribute("ticket", ticketRepository.findById(id).get());
        // Chiama lo stesso metodo! Niente più codice duplicato.
        model.addAttribute("users", getOperatoriDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());
        return "tickets/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @Valid @ModelAttribute("ticket") Ticket formTicket,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {

            model.addAttribute("users", getOperatoriDisponibili());
            return "tickets/edit";
        }
        ticketRepository.save(formTicket);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/editStato")
    public String editStato(Model model, @PathVariable Integer id) {
        model.addAttribute("ticket", ticketRepository.findById(id).get());
        model.addAttribute("users", userRepository.findAll());
        return "tickets/editStato";
    }

    @PostMapping("/{id}/editStato")
    public String updateStato(@PathVariable Integer id, @Valid @ModelAttribute("ticket") Ticket formTicket,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userRepository.findAll());
            return "tickets/edit";
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
            return "error/404"; // O un'altra pagina di errore
        }
        Ticket ticket = ticketOptional.get();

        Nota nota = new Nota();
        nota.setTicket(ticket);

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        userOpt.ifPresent(nota::setAutore);

        model.addAttribute("ticket", ticket);
        model.addAttribute("nota", nota);
        return "note/create";
    }

    private List<User> getOperatoriDisponibili() {
        List<User> operatoriDisponibili = new ArrayList<>();
        List<User> tuttiGliUtenti = userRepository.findAll();

        for (User utente : tuttiGliUtenti) {
            // Usiamo il booleano 'isDisponibile'
            boolean isOperatore = utente.getRoles().stream().anyMatch(role -> role.getNome().equals("OPERATORE"));
            if (isOperatore && utente.isDisponibile()) {
                operatoriDisponibili.add(utente);
            }
        }
        return operatoriDisponibili;
    }

}
