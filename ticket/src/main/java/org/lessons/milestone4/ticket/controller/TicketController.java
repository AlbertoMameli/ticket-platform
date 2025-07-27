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

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Optional;

@Controller//dto dicendo.. heyy! da ora in poi lavoro le richieste che arrivano dal web e in base a ciò che mi chiedi ti restituisco la pagina html..
@RequestMapping("/tickets") //dice a Spring : voglio mappare, cioè collegare, le richieste web a questa classe..
public class TicketController {
 //------dependency injenction
    // Qui mi preparo tutti i Repository che mi servono per parlare col database.
    // Grazie a @Autowired, Spring me li fornirà già pronti all'uso.
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

    // Questo è il metodo principale, quello che mostra la lista di tutti i ticket.
    @GetMapping
    public String index(Model model/*serve per passare gli oggetti java alla pagina HTML*/, Authentication authentication/*spring security.. la nostra autenticazione */, @RequestParam(name = "q", required = false) String keyword) {
        // Devo recuperare l'utente dal DB usando l'email che trovo nell'oggetto
        // Authentication.
        Optional<User> optionalUser = userRepository.findByEmail(authentication.getName());//assegno a optionalUser i dati che trovo inbase alla email.
        User utenteLoggato;//dichiaro una variabile vuota..
        if (optionalUser.isPresent()) {//se i dati inserirti sono presenti..
            utenteLoggato = optionalUser.get();//allora lo assegno alla  variabile
        } else {
            // Se non lo trovo,cè un errore e quindi blocco tutto.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utente non trovato!");
        }

        // Ora controllo se l'utente è un ADMIN. Lo faccio con un ciclo for per
        // chiarezza.
        boolean isAdmin = false;
        for (GrantedAuthority ruolo : authentication.getAuthorities()) {//cicliamop ogni ruolo che può avere l'utente...
            if (ruolo.getAuthority().equals("ADMIN")) {//se il ruolo che troviamo è admin 
                isAdmin = true;
                break; // Ho trovato il ruolo, inutile continuare a ciclare.
            }
        }

    //parte relativa ai tickets..
        List<Ticket> tickets;

        if (isAdmin) {
            // Se è un admin, può vedere tutti i ticket.
            if (keyword != null && !keyword.isEmpty()) {
                // Se ha cercato qualcosa, filtro per titolo.
                tickets = ticketRepository.findByTitoloContainingIgnoreCase(keyword);
            } else {
                // Altrimenti, li prendo tutti.
                tickets = ticketRepository.findAll();
            }
        } else {
            // Se non è un admin, vede solo i ticket assegnati a lui.
            if (keyword != null && !keyword.isEmpty()) {
                // Se ha cercato qualcosa, filtro tra i suoi ticket.
                tickets = ticketRepository.findByOperatoreIdAndTitoloContainingIgnoreCase(utenteLoggato.getId(),
                        keyword);
            } else {
                // Altrimenti, prendo tutti i suoi ticket.
                tickets = ticketRepository.findByOperatoreId(utenteLoggato.getId());
            }
        }

        // Aggiungo la lista di ticket al model, così la pagina può mostrarla.
        model.addAttribute("tickets", tickets);
        model.addAttribute("keyword", keyword); // Passo anche la keyword, per mostrarla nel campo di ricerca.
        model.addAttribute("stati", statoRepository.findAll()); // E la lista degli stati per i mini-form.

        return "tickets/index"; // Dico a Spring di mostrare la pagina index.html
    }

    // Metodo per vedere il dettaglio di un singolo ticket.
    @GetMapping("/{id}")
    public String show(@PathVariable Integer id, Model model, Authentication authentication) {
        // Cerco il ticket per ID. Se non lo trovo, gestisco l'errore.
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket non trovato!");
        }//assegno alla liosta dei tickets il ticket..
        Ticket ticket = optionalTicket.get();

        // Controllo se l'utente ha il permesso di vedere questo ticket.
        operatoreOAdmin(ticket, authentication);

        // Passo tutti i dati necessari alla pagina di dettaglio.
        model.addAttribute("ticket", ticket);
        model.addAttribute("note", notaRepository.findByTicketId(id));
        model.addAttribute("newNota", new Nota()); // Un oggetto Nota vuoto per il form di aggiunta.
        model.addAttribute("stati", statoRepository.findAll());

        return "tickets/show";
    }

    // Metodo che mostra la pagina per creare un nuovo ticket.
    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("ticket", new Ticket()); // Un ticket vuoto per il form.
        model.addAttribute("users", getUtentiAssegnabiliDisponibili()); // La lista di utenti a cui assegnarlo.
        model.addAttribute("categorie", categoriaRepository.findAll()); // E le categorie.
        return "tickets/create";
    }

    // Metodo che riceve i dati del form di creazione e salva il ticket.
    @PostMapping("/create")
    public String store(@ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult,
            @RequestParam(name = "categoriaId", required = false) Integer categoriaId,
            @RequestParam(name = "operatoreId", required = false) Integer operatoreId, Model model) {

        // Faccio una validazione manuale, campo per campo.
        if (formTicket.getTitolo().trim().isEmpty())
            bindingResult.addError(new FieldError("ticket", "titolo", "Il titolo è obbligatorio"));
        if (formTicket.getDescrizione().trim().isEmpty())
            bindingResult.addError(new FieldError("ticket", "descrizione", "La descrizione è obbligatoria"));
        if (categoriaId == null)
            bindingResult.addError(new FieldError("ticket", "categoria", "La categoria è obbligatoria"));
        if (operatoreId == null)
            bindingResult.addError(new FieldError("ticket", "operatore", "L'operatore è obbligatorio"));

        // Se ci sono stati errori di validazione...
        if (bindingResult.hasErrors()) {
            // ...devo ricaricare i dati per i menu a tendina...
            model.addAttribute("users", getUtentiAssegnabiliDisponibili());
            model.addAttribute("categorie", categoriaRepository.findAll());
            // ...e mostrare di nuovo il form di creazione con gli errori.
            return "tickets/create";
        }

        // Se è tutto OK, procedo a salvare il ticket.
        Optional<Categoria> optionalCategoria = categoriaRepository.findById(categoriaId);
        Optional<User> optionalOperatore = userRepository.findById(operatoreId);
        Optional<Stato> optionalStato = statoRepository.findByValore("Da fare");

        // Devo controllare che categoria, operatore e stato esistano prima di salvare.
        if (optionalCategoria.isEmpty() || optionalOperatore.isEmpty() || optionalStato.isEmpty()) {
            // Se qualcosa non esiste, è un errore grave. Per ora lo gestisco così.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dati non validi per la creazione del ticket.");
        }

        formTicket.setCategoria(optionalCategoria.get());
        formTicket.setOperatore(optionalOperatore.get());
        formTicket.setDataCreazione(LocalDateTime.now()); // Imposto io la data di creazione.
        formTicket.setStato(optionalStato.get());

        ticketRepository.save(formTicket); // Salvo il ticket nel database.

        return "redirect:/tickets"; // E reindirizzo l'utente alla lista dei ticket.
    }

    // Metodo per mostrare la pagina di modifica di un ticket esistente.
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model, Authentication authentication) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket da modificare non trovato!");
        }
        Ticket ticket = optionalTicket.get();

        operatoreOAdmin(ticket, authentication); // Solito controllo di sicurezza.

        model.addAttribute("ticket", ticket); // Passo il ticket da modificare.
        model.addAttribute("users", getUtentiAssegnabiliDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());

        return "tickets/edit";
    }

    // Metodo che riceve i dati dal form di modifica e aggiorna il ticket.
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id, @ModelAttribute("ticket") Ticket formTicket,
            BindingResult bindingResult,
            @RequestParam("categoriaId") Integer categoriaId,
            @RequestParam("operatoreId") Integer operatoreId, Authentication authentication, Model model) {

        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket da aggiornare non trovato!");
        }
        Ticket ticketToUpdate = optionalTicket.get();

        operatoreOAdmin(ticketToUpdate, authentication);

        // Validazione
        if (formTicket.getTitolo().trim().isEmpty())
            bindingResult.addError(new FieldError("ticket", "titolo", "Il titolo è obbligatorio"));
        if (formTicket.getDescrizione().trim().isEmpty())
            bindingResult.addError(new FieldError("ticket", "descrizione", "La descrizione è obbligatoria"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", getUtentiAssegnabiliDisponibili());
            model.addAttribute("categorie", categoriaRepository.findAll());
            return "tickets/edit";
        }

        // Recupero le associazioni
        Optional<Categoria> optionalCategoria = categoriaRepository.findById(categoriaId);
        Optional<User> optionalOperatore = userRepository.findById(operatoreId);
        if (optionalCategoria.isEmpty() || optionalOperatore.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dati non validi per l'aggiornamento.");
        }

        // Aggiorno i dati del ticket che ho trovato nel DB.
        ticketToUpdate.setTitolo(formTicket.getTitolo());
        ticketToUpdate.setDescrizione(formTicket.getDescrizione());
        ticketToUpdate.setCategoria(optionalCategoria.get());
        ticketToUpdate.setOperatore(optionalOperatore.get());

        ticketRepository.save(ticketToUpdate); // Salvo le modifiche.

        return "redirect:/tickets";
    }

    // Metodo per aggiornare solo lo stato del ticket.
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

    // Metodo per cancellare un ticket.
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

    // Metodo per controllare se l'utente è l'operatore del ticket o un admin.
    private void operatoreOAdmin(Ticket ticket, Authentication authentication) {
        // Recupero i dettagli specifici del mio utente.
        DatabaseUserDetails userDetails = (DatabaseUserDetails) authentication.getPrincipal();

        // Controllo se è admin 
        boolean isAdmin = false;
        for (GrantedAuthority ruolo : authentication.getAuthorities()) {
            if (ruolo.getAuthority().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }

        // Se non è un admin E non è l'operatore del ticket, allora non può accedere.
        if (!isAdmin && !ticket.getOperatore().getId().equals(userDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accesso negato");
        }
    }

    // Metodo per prendere la lista di utenti che possono essere assegnati a un
    // ticket.
    private List<User> getUtentiAssegnabiliDisponibili() {
        List<User> utentiDisponibili = new ArrayList<>();

        // Scorro tutti gli utenti del database.
        for (User utente : userRepository.findAll()) {
            // Per ogni utente, controllo se ha il ruolo giusto.
            boolean haRuoloAssegnabile = false;
            // Scorro tutti i ruoli di questo utente.
            for (Role ruolo : utente.getRoles()) {
                if (ruolo.getNome().equals("OPERATORE") || ruolo.getNome().equals("ADMIN")) {
                    haRuoloAssegnabile = true;
                    break; // Trovato un ruolo valido, esco dal ciclo interno.
                }
            }

            // Se l'utente ha il ruolo giusto ED è anche disponibile...
            if (haRuoloAssegnabile && utente.isDisponibile()) {
                // ...lo aggiungo alla lista che restituirò.
                utentiDisponibili.add(utente);
            }
        }
        return utentiDisponibili;
    }
}