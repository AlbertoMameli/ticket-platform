package org.lessons.milestone4.ticket.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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


    @Lob
    @NotBlank(message = "La descrizione/nome prodotto non può essere vuota!")
    private String nomeProdotto;

    @NotNull(message = "Inserisci la data di creazione!")
    @PastOrPresent(message = "La data di creazione non può essere nel futuro")
    private LocalDateTime dataCreazione;

    @NotBlank(message = "Inserire uno stato tra quelli a disposizione.")
    private String stato;

    @OneToMany(mappedBy = "ticket")
    private List<Nota> note;

    @ManyToOne
    @JoinColumn(name = "operatore_id", nullable = false)
    @NotNull(message = "Il ticket deve essere assegnato a un operatore")
    private User operatore;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    @NotNull(message = "Il ticket deve avere una categoria")
    private Categoria categoria;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public List<Nota> getNote() {
        return note;
    }

    public void setNote(List<Nota> note) {
        this.note = note;
    }

    public User getOperatore() {
        return operatore;
    }

    public void setOperatore(User operatore) {
        this.operatore = operatore;
    }

    public Categoria getCategoria() {
        return this.categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getNomeProdotto() {
        return this.nomeProdotto;
    }

    public void setNomeProdotto(String nomeProdotto) {
        this.nomeProdotto = nomeProdotto;
    }

}