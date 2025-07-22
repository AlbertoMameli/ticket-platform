package org.lessons.milestone4.ticket.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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

import jakarta.validation.Valid;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    NotaRepository notaRepository;

    @Autowired
    CategoriaRepository categoriaRepository;

    @Autowired
    StatoRepository statoRepository;

    private List<User> getUtentiAssegnabiliDisponibili() {
        List<User> utentiDisponibili = new ArrayList<>();
        List<User> tuttiGliUtenti = userRepository.findAll();

        for (User utente : tuttiGliUtenti) {
            boolean haRuoloAssegnabile = false;
            for (Role ruolo : utente.getRoles()) {
                if (ruolo.getNome().equals("OPERATORE") || ruolo.getNome().equals("ADMIN")) {
                    haRuoloAssegnabile = true;
                    break;
                }
            }

            if (haRuoloAssegnabile && utente.isDisponibile()) {
                utentiDisponibili.add(utente);
            }
        }
        return utentiDisponibili;
    }
    
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("users", getUtentiAssegnabiliDisponibili());
        model.addAttribute("categorie", categoriaRepository.findAll());
        model.addAttribute("stati", statoRepository.findAll());
    }

    @GetMapping
    public String index(
            Model model,
            Authentication authentication,
            @RequestParam(name = "q", required = false) String keyword) {
        Optional<User> userOptional = userRepository.findByEmail(authentication.getName());
        if (userOptional.isEmpty()) {
            return "redirect:/logout";
        }
        User utenteLoggato = userOptional.get();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        List<Ticket> tickets;

        if (keyword != null && !keyword.isEmpty()) {
            if (isAdmin) {
                tickets = ticketRepository.findByTitoloContainingIgnoreCase(keyword);
            } else {
                tickets = ticketRepository.findByOperatoreIdAndTitoloContainingIgnoreCase(utenteLoggato.getId(), keyword);
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

    @GetMapping("/{id}")
    public String show(Model model, @PathVariable Integer id) {
        Optional<Ticket> ticketOptional = ticketRepository.findById(id);
        if (ticketOptional.isEmpty()) {
            return "error/404";
        }

        model.addAttribute("ticket", ticketOptional.get());
        model.addAttribute("note", notaRepository.findByTicketId(id));
        return "tickets/show";
    }

    @GetMapping("/create")
    public String create(Model model) {
        Ticket ticket = new Ticket();
        ticket.setDataCreazione(LocalDateTime.now());
        model.addAttribute("ticket", ticket);
        return "tickets/create";
    }

    @PostMapping
    public String store(
            @ModelAttribute("ticket") Ticket formTicket,
            BindingResult bindingResult,
            @RequestParam(name = "operatore", required = false) Integer operatoreId,
            @RequestParam(name = "categoria", required = false) Integer categoriaId) {

        if (formTicket.getTitolo() == null || formTicket.getTitolo().trim().isEmpty()) {
            bindingResult.addError(new FieldError("ticket", "titolo", "Il titolo del ticket non può essere vuoto!"));
        }
        if (formTicket.getNomeProdotto() == null || formTicket.getNomeProdotto().trim().isEmpty()) {
            bindingResult.addError(new FieldError("ticket", "nomeProdotto", "La descrizione non può essere vuota!"));
        }

        if (operatoreId == null) {
            bindingResult.addError(new FieldError("ticket", "operatore", "Il ticket deve essere assegnato a un operatore"));
        }
        if (categoriaId == null) {
            bindingResult.addError(new FieldError("ticket", "categoria", "Il ticket deve avere una categoria"));
        }

        if (bindingResult.hasErrors()) {
            return "tickets/create";
        }

        Stato statoIniziale = statoRepository.findByValore("Da fare")
                .orElseThrow(() -> new IllegalStateException("Stato 'Da fare' non trovato nel database!"));
        formTicket.setStato(statoIniziale);

        formTicket.setOperatore(userRepository.findById(operatoreId).get());
        formTicket.setCategoria(categoriaRepository.findById(categoriaId).get());

        ticketRepository.save(formTicket);

        return "redirect:/tickets";
    }

    @GetMapping("/{id}/edit")
    public String edit(Model model, @PathVariable Integer id) {
        model.addAttribute("ticket", ticketRepository.findById(id).orElseThrow());
        return "tickets/edit";
    }

    @PostMapping("/{id}")
    public String update(@Valid @ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "tickets/edit";
        }
        ticketRepository.save(formTicket);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/editStato")
    public String editStato(Model model, @PathVariable Integer id) {
        model.addAttribute("ticket", ticketRepository.findById(id).orElseThrow());
        return "tickets/editStato";
    }

    @PostMapping("/{id}/editStato")
    public String updateStato(@Valid @ModelAttribute("ticket") Ticket formTicket, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "tickets/editStato";
        }
        ticketRepository.save(formTicket);
        return "redirect:/tickets/" + formTicket.getId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id) {
        ticketRepository.deleteById(id);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/nota")
    public String nota(@PathVariable Integer id, Model model, Authentication authentication) {
        Optional<Ticket> ticketOptional = ticketRepository.findById(id);
        if (ticketOptional.isEmpty()) {
            return "error/404";
        }
        Ticket ticket = ticketOptional.get();

        Nota nota = new Nota();
        nota.setTicket(ticket);

        userRepository.findByEmail(authentication.getName()).ifPresent(nota::setAutore);

        model.addAttribute("ticket", ticket);
        model.addAttribute("nota", nota);
        return "note/create";
    }
}