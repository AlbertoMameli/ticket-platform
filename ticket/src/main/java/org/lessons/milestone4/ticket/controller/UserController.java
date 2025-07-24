package org.lessons.milestone4.ticket.controller;

import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/users") // <-- CORRETTO: Plurale
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TicketRepository ticketRepository;

    /**
     * Mostra la lista di tutti gli utenti. Accessibile solo all'ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')") // Sicurezza a livello di metodo
    public String index(Model model) {
        model.addAttribute("userList", userRepository.findAll());
        return "users/index";
    }

    /**
     * Mostra la pagina del profilo dell'utente loggato.
     */
    @GetMapping("/show")
    public String show(Model model, Authentication authentication) {
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }
        User utente = userOpt.get();
        model.addAttribute("utente", utente);
        List<Ticket> ticketsAssegnati = ticketRepository.findByOperatoreId(utente.getId());
        model.addAttribute("tickets", ticketsAssegnati);
        return "users/show";
    }

    /**
     * Mostra il form per modificare il proprio profilo.
     */
    @GetMapping("/edit")
    public String edit(Model model, Authentication authentication) {
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }
        model.addAttribute("utente", userOpt.get());
        return "users/edit";
    }

    /**
     * Salva le modifiche al profilo.
     */
    @PostMapping("/edit")
    public String update(@ModelAttribute("utente") User formUtente, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User userDaSalvare = userRepository.findByEmail(authentication.getName()).orElseThrow();

        if (!formUtente.isDisponibile()) {
            Integer ticketAperti = ticketRepository.countByOperatoreIdAndStato_ValoreNot(userDaSalvare.getId(),
                    "COMPLETATO");
            if (ticketAperti > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Non puoi impostare lo stato su 'Non disponibile' perché hai " + ticketAperti
                                + " ticket non completati.");
                return "redirect:/users/edit"; // <-- CORRETTO: Plurale
            }
        }

        userDaSalvare.setNome(formUtente.getNome());
        userDaSalvare.setDisponibile(formUtente.isDisponibile());
        userRepository.save(userDaSalvare);
        redirectAttributes.addFlashAttribute("successMessage", "Profilo aggiornato con successo!");
        return "redirect:/users/edit"; // <-- CORRETTO: Plurale
    }
}