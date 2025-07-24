package org.lessons.milestone4.ticket.controller;

import org.lessons.milestone4.ticket.model.Nota;
import org.lessons.milestone4.ticket.model.Ticket;
import org.lessons.milestone4.ticket.model.User;
import org.lessons.milestone4.ticket.repository.NotaRepository;
import org.lessons.milestone4.ticket.repository.TicketRepository;
import org.lessons.milestone4.ticket.repository.UserRepository;
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

@Controller
public class NotaController {

    @Autowired
    private NotaRepository notaRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/tickets/{ticketId}/note")
    public String store(@PathVariable Integer ticketId, @RequestParam("testo") String testoNota,
            Authentication authentication, Model model) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket non trovato"));

        if (testoNota == null || testoNota.trim().isEmpty()) {
            model.addAttribute("ticket", ticket);
            model.addAttribute("note", notaRepository.findByTicketId(ticketId));
            model.addAttribute("newNota", new Nota());
            model.addAttribute("erroreNota", "Il testo della nota non può essere vuoto.");
            return "tickets/show";
        }

        Nota nuovaNota = new Nota();
        nuovaNota.setTesto(testoNota);
        User autore = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        nuovaNota.setTicket(ticket);
        nuovaNota.setAutore(autore);
        nuovaNota.setDataCreazione(LocalDateTime.now());
        notaRepository.save(nuovaNota);
        return "redirect:/tickets/" + ticketId;
    }

    @GetMapping("/note/{id}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        Nota nota = notaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("nota", nota);
        return "note/edit";
    }

    @PostMapping("/note/{id}")
    public String update(@PathVariable Integer id, @ModelAttribute("nota") Nota formNota,
            BindingResult bindingResult, Model model) {
        Nota notaDaAggiornare = notaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (formNota.getTesto() == null || formNota.getTesto().trim().isEmpty()) {
            bindingResult.addError(new FieldError("nota", "testo", "Il testo non può essere vuoto"));
        }
        if (bindingResult.hasErrors()) {
            formNota.setTicket(notaDaAggiornare.getTicket());
            return "note/edit";
        }
        notaDaAggiornare.setTesto(formNota.getTesto());
        notaRepository.save(notaDaAggiornare);
        return "redirect:/tickets/" + notaDaAggiornare.getTicket().getId();
    }

    @PostMapping("/note/{id}/delete")
    public String delete(@PathVariable Integer id) {
        Nota nota = notaRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Integer ticketId = nota.getTicket().getId();
        notaRepository.deleteById(id);
        return "redirect:/tickets/" + ticketId;
    }
}