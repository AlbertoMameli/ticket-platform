package org.lessons.milestone4.ticket.controller;

import java.util.List;
import java.util.Optional;

import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
public class UserController {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public UserController(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

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

    @GetMapping("/edit")
    public String edit(Model model, Authentication authentication) {
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }
        model.addAttribute("utente", userOpt.get());
        return "users/edit";
    }

    @PostMapping("/edit")
    public String update(
            @ModelAttribute("utente") User formUtente,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }
        User userDaSalvare = userOpt.get();

        // 2. Se l'operatore vuole mettersi NON disponibile, fai il controllo
        // Usiamo il nuovo campo booleano 'isDisponibile'
        if (!formUtente.isDisponibile()) {

            Integer ticketAperti = ticketRepository.countByOperatoreIdAndStato_ValoreNot(userDaSalvare.getId(),
                    "COMPLETATO");

            if (ticketAperti > 0) {
                // 4. Se ci sono ticket aperti, non salvare e manda un messaggio di errore
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Non puoi impostare lo stato su 'Non disponibile' perché hai " + ticketAperti
                                + " ticket non completati.");
                return "redirect:/user/edit";
            }
        }

        // 5. Se i controlli passano, aggiorna i dati dell'utente e salva
        userDaSalvare.setNome(formUtente.getNome());
        userDaSalvare.setDisponibile(formUtente.isDisponibile());
        userRepository.save(userDaSalvare);

        // Manda un messaggio di successo
        redirectAttributes.addFlashAttribute("successMessage", "Profilo aggiornato con successo!");
        return "redirect:/user/edit";
    }
}