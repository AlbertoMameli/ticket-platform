package org.lessons.milestone4.ticket.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Il titolo del ticket non può essere vuoto!")
    private String titolo;

    @NotBlank(message = "Il nome del prodotto non può essere vuoto!")
    private String nomeProdotto;

    @NotNull(message = "Inserisci la data di creazione!")
    @PastOrPresent(message = "La data di creazione non può essere nel futuro")
    private LocalDateTime dataCreazione;

    @NotNull(message = "Lo stato non può essere vuoto!")
    private String stato;

    @OneToMany(mappedBy = "ticket")
    private List<Nota> note;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Inserisci un operatore")
    private User user;


    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    

    public List<Nota> getNote() {
        return this.note;
    }

    public void setNote(List<Nota> note) {
        this.note = note;
    }

    public String getNomeProdotto() {
        return this.nomeProdotto;
    }

    public void setNomeProdotto(String nomeProdotto) {
        this.nomeProdotto = nomeProdotto;
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

    public LocalDateTime getDataCreazione() {
        return this.dataCreazione;
    }

    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public String getStato() {
        return this.stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

}