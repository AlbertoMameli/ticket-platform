package org.lessons.milestone4.ticket.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lessons.milestone4.ticket.model.Nota;
import org.lessons.milestone4.ticket.model.Role;
import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.NotaRepository;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

    @GetMapping
    public String index(Model model, Authentication authentication) {
        Optional<User> utenteLoggato = userRepository.findByEmail(authentication.getName());
        List<Ticket> ticketPerUtente = new ArrayList<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ADMIN")) {
                model.addAttribute("tickets", ticketRepository.findAll());
            } else if (authority.getAuthority().equals("OPERATORE")) {
                for (Ticket singleTicket : ticketRepository.findAll()) {
                    if (singleTicket.getUser().equals(utenteLoggato.get())) {
                        ticketPerUtente.add(singleTicket);
                    }
                }
                model.addAttribute("tickets", ticketPerUtente);
            }
        }

        return "tickets/index";
    }

    @GetMapping("/{id}")
    public String show(Model model, @PathVariable Integer id) {
        model.addAttribute("ticket", ticketRepository.findById(id).get());
        model.addAttribute("note", notaRepository.findAll());
        return "tickets/show";
    }

    @GetMapping("/create")
    public String create(Model model) {
        Ticket ticket = new Ticket();
        List<User> utentiAttivi = new ArrayList<>();
        boolean isOperatore = false;
        // Verifico se l'utente è un operatore
        for (User singelUser : userRepository.findAll()) {
            for (Role ruolo : singelUser.getRoles()) {
                if (ruolo.getNome().equals("OPERATORE")) {
                    isOperatore = true;
                    break;
                }
            }
            // Verifico se l'utente è attivo
            // In caso di esito positivo per entrambe lo aggiungo alla lista da mostrare
            // nella creazione ticket
            if (singelUser.getStatoPersonale().equals("Attivo") && isOperatore) {
                utentiAttivi.add(singelUser);
            }
        }
        ticket.setDataCreazione(LocalDateTime.now());
        model.addAttribute("ticket", ticket);
        model.addAttribute("users", utentiAttivi);
        return "tickets/create";

    }

    @PostMapping
    public String store(@Valid @ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userRepository.findAll());
            return "tickets/create";
        }
        ticketRepository.save(formTicket);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/edit")
    public String edit(Model model, @PathVariable Integer id) {
        List<User> utentiAttivi = new ArrayList<>();
        boolean isOperatore = false;
        // Verifico se l'utente è un operatore
        for (User singelUser : userRepository.findAll()) {
            for (Role ruolo : singelUser.getRoles()) {
                if (ruolo.getNome().equals("OPERATORE")) {
                    isOperatore = true;
                    break;
                }
            }
            // Verifico se l'utente è attivo
            // In caso di esito positivo per entrambe lo aggiungo alla lista da mostrare
            // nella creazione ticket
            if (singelUser.getStatoPersonale().equals("Attivo") && isOperatore) {
                utentiAttivi.add(singelUser);
            }
        }
        model.addAttribute("ticket", ticketRepository.findById(id).get());
        model.addAttribute("users", utentiAttivi);
        return "tickets/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @Valid @ModelAttribute("ticket") Ticket formTicket,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userRepository.findAll());
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
    public String nota(@PathVariable Integer id, Model model) {
        Nota nota = new Nota();
        nota.setTicket(ticketRepository.findById(id).get());
        model.addAttribute("ticket", ticketRepository.findById(id).get());
        model.addAttribute("nota", nota);
        return "note/create";
    }

}
