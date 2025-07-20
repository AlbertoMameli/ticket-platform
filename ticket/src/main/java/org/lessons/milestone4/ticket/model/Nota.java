package org.lessons.milestone4.ticket.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table (name = "note")
public class Nota {
    

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank (message = "Il titolo non può essere vuoto!")
    private String titolo;

    @NotBlank (message = "Il testo della nota non può essere vuoto!")
    private String testoNota;

    @NotNull (message = "La data di creazione non può essere vuota!")
    @FutureOrPresent (message = "La data di creazione non può essere passata")
    private LocalDateTime dataCreazione;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;


    public Ticket getTicket() {
        return this.ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }



    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitolo() {
        return this.titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getTestoNota() {
        return this.testoNota;
    }

    public void setTestoNota(String testoNota) {
        this.testoNota = testoNota;
    }

    public LocalDateTime getDataCreazione() {
        return this.dataCreazione;
    }

    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }



}
