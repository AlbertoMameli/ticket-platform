package org.lessons.milestone4.ticket.controller;

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

@Controller
@RequestMapping("/tickets")
public class TicketController {

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
    public String index(Model model, Authentication authentication, @RequestParam(name = "q", required = false) String keyword) {
        User utenteLoggato = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        List<Ticket> tickets;

        if (keyword != null && !keyword.isEmpty()) {
            tickets = isAdmin ? ticketRepository.findByTitoloContainingIgnoreCase(keyword)
                    : ticketRepository.findByOperatoreIdAndTitoloContainingIgnoreCase(utenteLoggato.getId(), keyword);
        } else {
            tickets = isAdmin ? ticketRepository.findAll() : ticketRepository.findByOperatoreId(utenteLoggato.getId());
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("keyword", keyword);
        model.addAttribute("stati", statoRepository.findAll()); // Passa la lista degli stati per i mini-form
        return "tickets/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Integer id, Model model, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        operatoreOAdmin(ticket, authentication);
        model.addAttribute("ticket", ticket);
        model.addAttribute("note", notaRepository.findByTicketId(id));
        model.addAttribute("newNota", new Nota());
        model.addAttribute("stati", statoRepository.findAll()); // Passa la lista degli stati per i mini-form
        return "tickets/show";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("ticket", new Ticket());
        model.addAttribute("users", getUtentiAssegnabiliDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());
        return "tickets/create";
    }

    @PostMapping("/create")
    public String store(@ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult,
            @RequestParam(name = "categoriaId", required = false) Integer categoriaId,
            @RequestParam(name = "operatoreId", required = false) Integer operatoreId, Model model) {
        // Validazione manuale
        if (formTicket.getTitolo().trim().isEmpty())
            bindingResult.addError(new FieldError("ticket", "titolo", "Il titolo è obbligatorio"));
        if (formTicket.getDescrizione().trim().isEmpty())
            bindingResult.addError(new FieldError("ticket", "descrizione", "La descrizione è obbligatoria"));
        if (categoriaId == null)
            bindingResult.addError(new FieldError("ticket", "categoria", "La categoria è obbligatoria"));
        if (operatoreId == null)
            bindingResult.addError(new FieldError("ticket", "operatore", "L'operatore è obbligatorio"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", getUtentiAssegnabiliDisponibili());
            model.addAttribute("categorie", categoriaRepository.findAll());
            return "tickets/create";
        }

        formTicket.setCategoria(categoriaRepository.findById(categoriaId).orElseThrow());
        formTicket.setOperatore(userRepository.findById(operatoreId).orElseThrow());
        formTicket.setDataCreazione(LocalDateTime.now());
        formTicket.setStato(statoRepository.findByValore("Da fare").orElseThrow());
        ticketRepository.save(formTicket);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        operatoreOAdmin(ticket, authentication);
        model.addAttribute("ticket", ticket);
        model.addAttribute("users", getUtentiAssegnabiliDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());
        return "tickets/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id, @ModelAttribute("ticket") Ticket formTicket,
            BindingResult bindingResult,
            @RequestParam("categoriaId") Integer categoriaId,
            @RequestParam("operatoreId") Integer operatoreId, Authentication authentication, Model model) {
        Ticket ticketToUpdate = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        operatoreOAdmin(ticketToUpdate, authentication);

        if (formTicket.getTitolo().trim().isEmpty())
            bindingResult.addError(new FieldError("ticket", "titolo", "Il titolo è obbligatorio"));
        if (formTicket.getDescrizione().trim().isEmpty())
            bindingResult.addError(new FieldError("ticket", "descrizione", "La descrizione è obbligatoria"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", getUtentiAssegnabiliDisponibili());
            model.addAttribute("categorie", categoriaRepository.findAll());
            return "tickets/edit";
        }

        ticketToUpdate.setTitolo(formTicket.getTitolo());
        ticketToUpdate.setDescrizione(formTicket.getDescrizione());
        ticketToUpdate.setCategoria(categoriaRepository.findById(categoriaId).orElseThrow());
        ticketToUpdate.setOperatore(userRepository.findById(operatoreId).orElseThrow());
        ticketRepository.save(ticketToUpdate);
        return "redirect:/tickets";
    }

    @PostMapping("/{id}/editStato")
    public String updateStato(@PathVariable Integer id, @RequestParam("statoId") Integer statoId,
            Authentication authentication) {
        Ticket ticketToUpdate = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        operatoreOAdmin(ticketToUpdate, authentication);
        ticketToUpdate.setStato(statoRepository.findById(statoId).orElseThrow());
        ticketRepository.save(ticketToUpdate);
        return "redirect:/tickets"; // Redirect alla lista per un feedback visivo immediato
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        operatoreOAdmin(ticket, authentication);
        ticketRepository.delete(ticket);
        return "redirect:/tickets";
    }

    private void operatoreOAdmin (Ticket ticket, Authentication authentication) {
        DatabaseUserDetails userDetails = (DatabaseUserDetails) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin && !ticket.getOperatore().getId().equals(userDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accesso negato");
        }
    }

    private List<User> getUtentiAssegnabiliDisponibili() {
        List<User> utentiDisponibili = new ArrayList<>();
        for (User utente : userRepository.findAll()) {
            boolean haRuoloAssegnabile = utente.getRoles().stream()
                    .anyMatch(role -> role.getNome().equals("OPERATORE") || role.getNome().equals("ADMIN"));
            if (haRuoloAssegnabile && utente.isDisponibile()) {
                utentiDisponibili.add(utente);
            }
        }
        return utentiDisponibili;
    }
}