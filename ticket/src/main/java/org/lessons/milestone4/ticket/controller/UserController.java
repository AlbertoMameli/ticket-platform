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

    // Come per il TicketController, mi faccio "iniettare" da Spring i repository
    // che mi servono.
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TicketRepository ticketRepository;

    @GetMapping
    // Invece di un controllo manuale, uso questa annotazione di Spring Security.
    // Dice: "Solo un utente che ha l'autorità 'ADMIN' può eseguire questo metodo".
    // È un modo più diretto e dichiarativo per gestire la sicurezza.
    // il metodo che ho usato nel ticketcontroller eseguiva il controllo una volta
    // entrato.. invece qua mi blocca prima di entrare..
    @PreAuthorize("hasAuthority('ADMIN')")
    public String index(Model model) {
        // Prendo tutti gli utenti dal database e li passo alla vista.
        model.addAttribute("userList", userRepository.findAll());
        return "users/index"; // Mostro la pagina 'index.html' dentro la cartella 'users'.
    }

    /**
     * Questo metodo mostra la pagina del profilo personale dell'utente che ha fatto
     * il login.
     */
    @GetMapping("/show")
    public String show(Model model, Authentication authentication) {
        // Devo recuperare i dati dell'utente loggato.
        // Uso il suo nome (l'email) per cercarlo nel database.
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());

        // Controllo se l'utente è stato trovato.
        if (userOpt.isEmpty()) {
            // Se non lo trovo, qualcosa è andato storto. La cosa più sicura
            // è reindirizzarlo alla pagina di logout per fargli rifare il login.
            return "redirect:/logout";
        }

        // Se l'utente è stato trovato, lo estraggo dall'Optional.
        User utente = userOpt.get();

        // Ora cerco tutti i ticket che sono stati assegnati a questo utente.
        List<Ticket> ticketsAssegnati = ticketRepository.findByOperatoreId(utente.getId());

        // Passo tutti i dati alla pagina HTML.
        model.addAttribute("utente", utente);
        model.addAttribute("tickets", ticketsAssegnati);

        return "users/show"; // Mostro la pagina del profilo.
    }

    /**
     * Questo metodo mostra il form per modificare il proprio profilo.
     */
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

    /**
     * Questo metodo riceve i dati dal form e salva le modifiche al profilo.
     */
    @PostMapping("/edit")
    public String update(@ModelAttribute("utente") User formUtente, Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }

        User userDaSalvare = userOpt.get();

        // Se sta cercando di diventare "non disponibile"...
        if (!formUtente.isDisponibile()) {
            // Controllo se ha almeno un ticket in stato "Da fare" o "In corso"
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
