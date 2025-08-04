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
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TicketRepository ticketRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')") //aop non è un annotazione di mvc, si tratta di programmazione orientati agli aspetti 
    public String index(Model model) {
        model.addAttribute("userList", userRepository.findAll());
        return "users/index"; 
    }

    @GetMapping("/show")
    public String show(Model model, Authentication authentication) {
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());

        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }
        User utente = userOpt.get();

        // Ora cerco tutti i ticket che sono stati assegnati a questo utente.
        List<Ticket> ticketsAssegnati = ticketRepository.findByOperatoreId(utente.getId());

        // Passo tutti i dati alla pagina HTML.
        model.addAttribute("utente", utente);
        model.addAttribute("tickets", ticketsAssegnati);

        return "users/show"; // Mostro la pagina del profilo.
    }

    @GetMapping("/edit")
    public String edit(Model model, Authentication authentication) {
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());

        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }

        // Prendiamo l'utente dall'Optional
        User utente = userOpt.get();

        model.addAttribute("utente", utente);

        // Controlla se l'utente ha ticket non completati USANDO L'ID CORRETTO
        Integer ticketAperti = ticketRepository.countByOperatoreIdAndStato_ValoreNot(utente.getId(), "COMPLETATO");

        // Passa un booleano al template
        model.addAttribute("haTicketAperti", ticketAperti > 0);

        return "users/edit";
    }

    @PostMapping("/edit")
    public String update(@ModelAttribute("utente") User formUtente, Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }

        User userDaSalvare = userOpt.get();

        if (!formUtente.isDisponibile()) {
            List<String> statiAttivi = List.of("Da fare", "In corso");
            int ticketAttivi = ticketRepository.countByOperatoreIdAndStato_ValoreIn(userDaSalvare.getId(), statiAttivi);

            if (ticketAttivi > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Non puoi impostare lo stato su 'Non disponibile' perché hai " + ticketAttivi
                                + " ticket attivi ('Da fare' o 'In corso').");
                return "redirect:/users/edit";
            }
        }

        // Aggiorno i dati
        userDaSalvare.setNome(formUtente.getNome());
        userDaSalvare.setDisponibile(formUtente.isDisponibile());
        userRepository.save(userDaSalvare);

        redirectAttributes.addFlashAttribute("successMessage", "Profilo aggiornato con successo!");
        return "redirect:/users/edit";
    }
}
