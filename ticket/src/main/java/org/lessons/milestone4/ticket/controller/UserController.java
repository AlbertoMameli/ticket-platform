package org.lessons.milestone4.ticket.controller;

import java.util.Optional;

import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserController {

    private final TicketRepository ticketRepository;

    @Autowired
    UserRepository userRepository;

    UserController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/show")
    public String show(Model model, Authentication authentication) {
        model.addAttribute("utente", userRepository.findByEmail(authentication.getName()).get());
        return "users/show";
    }

    @GetMapping("/edit")
    public String stato(Model model, Authentication authentication) {
        model.addAttribute("utente", userRepository.findByEmail(authentication.getName()).get());

        return "users/edit";
    }

    @PostMapping("/edit")
    public String update(@ModelAttribute("utente") User userForm, Authentication authentication) {
        Optional<User> user = userRepository.findByEmail(authentication.getName());
        int nTicketNonCompleti = 0;
        if (user.get().getStatoPersonale().equals("Attivo")) {
            for (Ticket singleTicket : ticketRepository.findAll()) {
                if (singleTicket.getUser().equals(user.get()) && !singleTicket.getStato().equals("Completato")) {
                    nTicketNonCompleti = +1;

                }

            }
            if (nTicketNonCompleti == 0) {
                userRepository.save(userForm);
                return "redirect:/user/edit";
            }

            return "pages/home";
        }

        userRepository.save(userForm);
        return "redirect:/user/edit";
    }

}