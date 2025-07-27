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
    //il metodo che ho usato nel ticketcontroller eseguiva il controllo una volta entrato.. invece qua mi blocca prima di entrare..
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
        // La logica per trovare l'utente è identica a quella del metodo 'show'.
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());

        if (userOpt.isEmpty()) {
            return "redirect:/logout";
        }

        // Passo l'utente trovato al model, così il form sarà pre-compilato con i suoi
        // dati.
        model.addAttribute("utente", userOpt.get());

        return "users/edit"; // Mostro la pagina di modifica.
    }

    /**
     * Questo metodo riceve i dati dal form e salva le modifiche al profilo.
     */
    @PostMapping("/edit")
    public String update(@ModelAttribute("utente") User formUtente, Authentication authentication,
            RedirectAttributes redirectAttributes) {

        // Devo recuperare la versione "reale" dell'utente dal DB. Non mi fido
        // dell'oggetto che arriva dal form per motivi di sicurezza.

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            // Anche qui, se l'utente non esiste, lo mando al logout.
            return "redirect:/logout";
        }
        User userDaSalvare = userOpt.get(); // Questo è l'utente che modificherò e salverò.

        // Ho una regola importante: un utente non può mettersi 'Non disponibile'
        // se ha ancora dei ticket aperti. Devo controllarlo.

        // La condizione si attiva solo se l'utente sta cercando di impostarsi come non
        // disponibile.
        if (!formUtente.isDisponibile()) {
            // Conto quanti ticket ha l'utente il cui stato NON è 'COMPLETATO'.
            Integer ticketAperti = ticketRepository.countByOperatoreIdAndStato_ValoreNot(userDaSalvare.getId(),
                    "COMPLETATO");

            // Se il numero di ticket aperti è maggiore di zero...
            if (ticketAperti > 0) {
                // ...non posso procedere. Devo avvisare l'utente.
                // Uso 'RedirectAttributes' per passare un messaggio di errore alla pagina
                // a cui sto reindirizzando. Questo messaggio "sopravvive" al redirect.
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Non puoi impostare lo stato su 'Non disponibile' perché hai " + ticketAperti
                                + " ticket non completati.");

                // Reindirizzo l'utente di nuovo alla pagina di modifica.
                return "redirect:/users/edit";
            }
        }

        // --- PARTE 3: Aggiornamento e salvataggio ---
        // Se tutti i controlli sono passati, aggiorno i dati dell'utente con
        // quelli che ho ricevuto dal form.

        userDaSalvare.setNome(formUtente.getNome());
        userDaSalvare.setDisponibile(formUtente.isDisponibile());

        // Salvo l'utente aggiornato nel database.
        userRepository.save(userDaSalvare);

        // Invio un messaggio di successo per confermare che l'operazione è andata a
        // buon fine.
        redirectAttributes.addFlashAttribute("successMessage", "Profilo aggiornato con successo!");

        // Reindirizzo l'utente alla pagina di modifica, dove vedrà il messaggio di
        // successo.
        return "redirect:/users/edit";
    }
}