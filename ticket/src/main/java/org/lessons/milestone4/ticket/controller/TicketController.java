package org.lessons.milestone4.ticket.controller;

// Import necessari per la validazione, le entità, i repository e la sicurezza
import jakarta.validation.Valid;
import org.lessons.milestone4.ticket.model.*;
import org.lessons.milestone4.ticket.repository.*;
import org.lessons.milestone4.ticket.security.DatabaseUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller per la gestione delle operazioni sui Ticket.
 * Gestisce la creazione, visualizzazione, modifica ed eliminazione dei ticket,
 * con controlli di sicurezza basati sui ruoli e sulla proprietà.
 */
@Controller
@RequestMapping("/tickets")
public class TicketController {

    // Iniezione delle dipendenze tramite @Autowired
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotaRepository notaRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private StatoRepository statoRepository;

    /**
     * Mostra la lista dei ticket.
     * Per gli ADMIN: tutti i ticket.
     * Per gli OPERATORI: solo i ticket a loro assegnati.
     * Supporta la ricerca per titolo.
     */
    @GetMapping
    public String index(Model model, Authentication authentication,
            @RequestParam(name = "q", required = false) String keyword) {
        User utenteLoggato = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        List<Ticket> tickets;

        if (keyword != null && !keyword.isEmpty()) {
            if (isAdmin) {
                tickets = ticketRepository.findByTitoloContainingIgnoreCase(keyword);
            } else {
                tickets = ticketRepository.findByOperatoreIdAndTitoloContainingIgnoreCase(utenteLoggato.getId(),
                        keyword);
            }
        } else {
            if (isAdmin) {
                tickets = ticketRepository.findAll();
            } else {
                tickets = ticketRepository.findByOperatoreId(utenteLoggato.getId());
            }
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("keyword", keyword);
        return "tickets/index";
    }

    /**
     * Mostra i dettagli di un singolo ticket.
     */
    @GetMapping("/{id}")
    public String show(@PathVariable Integer id, Model model, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        checkUserOwnershipOrAdmin(ticket, authentication);
        model.addAttribute("ticket", ticket);
        model.addAttribute("note", notaRepository.findByTicketId(id));
        model.addAttribute("newNota", new Nota());
        return "tickets/show";
    }

    /**
     * Mostra il form per creare un nuovo ticket.
     */
    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("ticket", new Ticket());
        model.addAttribute("users", getUtentiAssegnabiliDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());
        return "tickets/create";
    }

    /**
     * Salva un nuovo ticket nel database.
     */
    @PostMapping("/create")
    public String store(
            // Rimuoviamo @Valid da qui per prendere il controllo della validazione
            @ModelAttribute("ticket") Ticket formTicket,
            BindingResult bindingResult,
            @RequestParam(name = "categoriaId", required = false) Integer categoriaId,
            @RequestParam(name = "operatoreId", required = false) Integer operatoreId,
            Model model) {

        if (formTicket.getTitolo() == null || formTicket.getTitolo().trim().isEmpty()) {
            bindingResult.addError(new FieldError("ticket", "titolo", "Il titolo è obbligatorio"));
        }
        if (formTicket.getDescrizione() == null || formTicket.getDescrizione().trim().isEmpty()) {
            bindingResult.addError(new FieldError("ticket", "descrizione", "La descrizione è obbligatoria"));
        }
        if (categoriaId == null) {
            bindingResult.addError(new FieldError("ticket", "categoria", "La categoria è obbligatoria"));
        }
        if (operatoreId == null) {
            bindingResult.addError(new FieldError("ticket", "operatore", "L'operatore è obbligatorio"));
        }

        // Se uno qualsiasi dei controlli sopra è fallito...
        if (bindingResult.hasErrors()) {
            // ... ricarichiamo il form, ripopolando i menu a tendina.
            model.addAttribute("users", getUtentiAssegnabiliDisponibili());
            model.addAttribute("categorie", categoriaRepository.findAll());
            return "tickets/create";
        }

        // Se la validazione è passata, procediamo a costruire e salvare l'oggetto
        // completo.
        formTicket.setCategoria(categoriaRepository.findById(categoriaId).orElseThrow());
        formTicket.setOperatore(userRepository.findById(operatoreId).orElseThrow());
        formTicket.setDataCreazione(LocalDateTime.now());
        formTicket.setStato(statoRepository.findByValore("Da fare").orElseThrow());

        ticketRepository.save(formTicket);

        return "redirect:/tickets";
    }

    /**
     * Mostra il form per modificare un ticket esistente.
     */
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        checkUserOwnershipOrAdmin(ticket, authentication);
        model.addAttribute("ticket", ticket);
        model.addAttribute("users", getUtentiAssegnabiliDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());
        return "tickets/edit";
    }

    /**
     * Aggiorna un ticket esistente nel database.
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id, @Valid @ModelAttribute("ticket") Ticket formTicket,
            BindingResult bindingResult,
            @RequestParam("categoriaId") Integer categoriaId,
            @RequestParam("operatoreId") Integer operatoreId, Authentication authentication) {
        Ticket ticketToUpdate = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        checkUserOwnershipOrAdmin(ticketToUpdate, authentication);
        if (bindingResult.hasErrors()) {
            return "tickets/edit";
        }
        ticketToUpdate.setTitolo(formTicket.getTitolo());
        ticketToUpdate.setDescrizione(formTicket.getDescrizione());
        ticketToUpdate.setCategoria(categoriaRepository.findById(categoriaId).orElseThrow());
        ticketToUpdate.setOperatore(userRepository.findById(operatoreId).orElseThrow());
        ticketRepository.save(ticketToUpdate);
        return "redirect:/tickets";
    }

    /**
     * Mostra il form per modificare solo lo stato di un ticket.
     */
    @GetMapping("/{id}/editStato")
    public String editStato(@PathVariable Integer id, Model model, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        checkUserOwnershipOrAdmin(ticket, authentication);
        model.addAttribute("ticket", ticket);
        model.addAttribute("stati", statoRepository.findAll());
        return "tickets/editStato";
    }

    /**
     * Aggiorna solo lo stato di un ticket esistente.
     */
    @PostMapping("/{id}/editStato")
    public String updateStato(@PathVariable Integer id, @RequestParam("statoId") Integer statoId,
            Authentication authentication) {
        Ticket ticketToUpdate = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        checkUserOwnershipOrAdmin(ticketToUpdate, authentication);
        ticketToUpdate.setStato(statoRepository.findById(statoId).orElseThrow());
        ticketRepository.save(ticketToUpdate);
        return "redirect:/tickets/" + ticketToUpdate.getId();
    }

    /**
     * Elimina un ticket dal database.
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        checkUserOwnershipOrAdmin(ticket, authentication);
        ticketRepository.delete(ticket);
        return "redirect:/tickets";
    }

    // METODI PRIVATI DI SUPPORTO //

    /**
     * Metodo di sicurezza per verificare che l'utente loggato sia ADMIN o il
     * proprietario del ticket.
     */
    private void checkUserOwnershipOrAdmin(Ticket ticket, Authentication authentication) {
        DatabaseUserDetails userDetails = (DatabaseUserDetails) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin && !ticket.getOperatore().getId().equals(userDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accesso negato");
        }
    }

    /**
     * Restituisce una lista di utenti (Operatori/Admin) che sono attualmente
     * disponibili.
     */
    private List<User> getUtentiAssegnabiliDisponibili() {
        List<User> utentiDisponibili = new ArrayList<>();
        List<User> tuttiGliUtenti = userRepository.findAll();
        for (User utente : tuttiGliUtenti) {
            boolean haRuoloAssegnabile = utente.getRoles().stream()
                    .anyMatch(role -> role.getNome().equals("OPERATORE") || role.getNome().equals("ADMIN"));
            if (haRuoloAssegnabile && utente.isDisponibile()) {
                utentiDisponibili.add(utente);
            }
        }
        return utentiDisponibili;
    }
}