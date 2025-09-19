package org.lessons.milestone4.ticket.controller;

import org.lessons.milestone4.ticket.model.Categoria;
import org.lessons.milestone4.ticket.model.Nota;
import org.lessons.milestone4.ticket.model.Role;
import org.lessons.milestone4.ticket.model.Stato;
import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.CategoriaRepository;
import org.lessons.milestone4.ticket.repository.NotaRepository;
import org.lessons.milestone4.ticket.repository.StatoRepository;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.UserRepository;
import org.lessons.milestone4.ticket.security.DatabaseUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/tickets")
public class TicketController {
// ! field injection
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

    @GetMapping
    public String index(Model model, Authentication authentication,
            @RequestParam(name = "q", required = false) String keyword ) {
        Optional<User> optionalUser = userRepository.findByEmail(authentication.getName());
        User utenteLoggato;
        if (optionalUser.isPresent()) {
            utenteLoggato = optionalUser.get();
        } else {

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utente non trovato!");
        }

        boolean isAdmin = false;
        for (GrantedAuthority ruolo : authentication.getAuthorities()) {
            if (ruolo.getAuthority().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }

        List<Ticket> tickets;
//se sono admin e in base alla barra di ricerca 
        if (isAdmin) {
            if (keyword != null && !keyword.isEmpty()) {
                tickets = ticketRepository.findByTitoloContainingIgnoreCase(keyword);
            } else {
                tickets = ticketRepository.findAll();
            }
        } else {//non  sono admin alloraa
            if (keyword != null && !keyword.isEmpty()) {
                tickets = ticketRepository.findByOperatoreIdAndTitoloContainingIgnoreCase(utenteLoggato.getId(), keyword);
            } else {
                tickets = ticketRepository.findByOperatoreId(utenteLoggato.getId());
            }
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("keyword", keyword);
        model.addAttribute("stati", statoRepository.findAll());

        return "tickets/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Integer id, Model model, Authentication authentication) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket non trovato!");
        } // assegno alla lista dei tickets il ticket..
        Ticket ticket = optionalTicket.get();

        operatoreOAdmin(ticket, authentication);

        // Passo tutti i dati necessari alla pagina di dettaglio.
        model.addAttribute("ticket", ticket);
        model.addAttribute("note", notaRepository.findByTicketId(id));
        model.addAttribute("newNota", new Nota()); // Un oggetto Nota vuoto per il form di aggiunta.
        model.addAttribute("stati", statoRepository.findAll());

        return "tickets/show";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("ticket", new Ticket()); // Un ticket vuoto per il form.
        // menù select
        model.addAttribute("users", getUtentiAssegnabiliDisponibili()); 
        model.addAttribute("categorie", categoriaRepository.findAll()); 
        return "tickets/create";
    }

    @PostMapping("/create")
    public String store(@Valid @ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult,
            @RequestParam(name = "categoriaId", required = false) Integer categoriaId,
            @RequestParam(name = "operatoreId", required = false) Integer operatoreId, Model model) {

        // Controllo se ci sono operatori disponibili
        if (!ciSonoOperatoriDisponibili()) {
            bindingResult.reject("noOperatori", "Non ci sono operatori disponibili al momento.");
        }

        // Validazioni manuali
        if (categoriaId == null)
            bindingResult.addError(new FieldError("ticket", "categoria", "La categoria è obbligatoria"));
        if (operatoreId == null)
            bindingResult.addError(new FieldError("ticket", "operatore", "L'operatore è obbligatorio"));

        // Se ci sono errori, ricarica i dati e torna alla form
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", getUtentiAssegnabiliDisponibili());
            model.addAttribute("categorie", categoriaRepository.findAll());
            model.addAttribute("categoriaId", categoriaId);
            model.addAttribute("operatoreId", operatoreId);
            return "tickets/create";
        }

        Optional<Categoria> optionalCategoria = categoriaRepository.findById(categoriaId);
        Optional<User> optionalOperatore = userRepository.findById(operatoreId);
        Optional<Stato> optionalStato = statoRepository.findByValore("Da fare");

        if (optionalCategoria.isEmpty() || optionalOperatore.isEmpty() || optionalStato.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dati non validi per la creazione del ticket.");
        }

        formTicket.setCategoria(optionalCategoria.get());
        formTicket.setOperatore(optionalOperatore.get());
        formTicket.setDataCreazione(LocalDateTime.now());
        formTicket.setStato(optionalStato.get());

        ticketRepository.save(formTicket);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model, Authentication authentication) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket da modificare non trovato!");
        }
        Ticket ticket = optionalTicket.get();

        operatoreOAdmin(ticket, authentication); // Solito controllo di sicurezza.

        model.addAttribute("ticket", ticket); // Passo il ticket da modificare.
        model.addAttribute("users",  getUtentiAssegnabiliDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());

        return "tickets/edit";
    }

    // Metodo che riceve i dati dal form di modifica e aggiorna il ticket.
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id, @Valid @ModelAttribute("ticket") Ticket formTicket,
            BindingResult bindingResult,
            @RequestParam(name = "categoriaId", required = false) Integer categoriaId,
            @RequestParam(name = "operatoreId", required = false) Integer operatoreId,
            Authentication authentication, Model model) {

        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket da aggiornare non trovato!");
        }
        Ticket ticketToUpdate = optionalTicket.get();

        operatoreOAdmin(ticketToUpdate, authentication);

        // Validazione
        if (formTicket.getDescrizione().trim().isEmpty())
            bindingResult.addError(new FieldError("ticket", "descrizione", "La descrizione è obbligatoria"));

        if (!ciSonoOperatoriDisponibili()) {
            bindingResult.reject("operatoreIndisponibile", "Nessun operatore disponibile per l'assegnazione.");
            model.addAttribute("users", getUtentiAssegnabiliDisponibili());
            model.addAttribute("categorie", categoriaRepository.findAll());
            return "tickets/edit";
        }
        if (bindingResult.hasErrors()) {
            formTicket.setCategoria(ticketToUpdate.getCategoria());
            formTicket.setOperatore(ticketToUpdate.getOperatore());
            formTicket.setStato(ticketToUpdate.getStato()); // Mantieni lo stato originale
            model.addAttribute("users", getUtentiAssegnabiliDisponibili());
            model.addAttribute("categorie", categoriaRepository.findAll());
            return "tickets/edit";
        }

        // Recupero le entità
        Optional<Categoria> optionalCategoria = categoriaRepository.findById(categoriaId);
        Optional<User> optionalOperatore = userRepository.findById(operatoreId);

        if (optionalCategoria.isEmpty() || optionalOperatore.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dati non validi per l'aggiornamento.");
        }

        // Mantieni lo stato attuale (non modificarlo)
        Stato statoOriginale = ticketToUpdate.getStato();

        // Aggiorna i dati modificabili
        ticketToUpdate.setTitolo(formTicket.getTitolo());
        ticketToUpdate.setDescrizione(formTicket.getDescrizione());
        ticketToUpdate.setCategoria(optionalCategoria.get());
        ticketToUpdate.setOperatore(optionalOperatore.get());
        ticketToUpdate.setStato(statoOriginale); // Stato invariato

        ticketRepository.save(ticketToUpdate);

        return "redirect:/tickets";
    }

    @PostMapping("/{id}/editStato")
    public String updateStato(@PathVariable Integer id, @RequestParam("statoId") Integer statoId,
            Authentication authentication) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket non trovato");
        }
        Ticket ticketToUpdate = optionalTicket.get();

        operatoreOAdmin(ticketToUpdate, authentication);

        Optional<Stato> optionalStato = statoRepository.findById(statoId);
        if (optionalStato.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stato non valido.");
        }

        ticketToUpdate.setStato(optionalStato.get());
        ticketRepository.save(ticketToUpdate);

        return "redirect:/tickets";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, Authentication authentication) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket da eliminare non trovato!");
        }
        Ticket ticket = optionalTicket.get();

        operatoreOAdmin(ticket, authentication);

        ticketRepository.delete(ticket);

        return "redirect:/tickets";
    }

    // --- METODI DI SUPPORTO ---
//1
    private boolean ciSonoOperatoriDisponibili() {
        return getUtentiAssegnabiliDisponibili().size() > 0;
    }
//2
    private void operatoreOAdmin(Ticket ticket, Authentication authentication) {
        DatabaseUserDetails userDetails = (DatabaseUserDetails) authentication.getPrincipal();
        boolean isAdmin = false;
        for (GrantedAuthority ruolo : authentication.getAuthorities()) {
            if (ruolo.getAuthority().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }
        if (!isAdmin && !ticket.getOperatore().getId().equals(userDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accesso negato");
        }
    }
//3
    private List<User> getUtentiAssegnabiliDisponibili() {
        List<User> utentiDisponibili = new ArrayList<>();
        for (User utente : userRepository.findAll()) {
            boolean haRuoloAssegnabile = false;
            for (Role ruolo : utente.getRoles()) {
                if (ruolo.getNome().equals("OPERATORE") || ruolo.getNome().equals("ADMIN")) {
                    haRuoloAssegnabile = true;
                    break; // Trovato un ruolo valido, esco dal ciclo interno.
                }
            }
            if (haRuoloAssegnabile && utente.isDisponibile()) {
                // ...lo aggiungo alla lista che restituirò.
                utentiDisponibili.add(utente);
            }
        }
        return utentiDisponibili;
    }


}
