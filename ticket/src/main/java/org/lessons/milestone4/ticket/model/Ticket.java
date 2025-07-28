package org.lessons.milestone4.ticket.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
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
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Il titolo del ticket non può essere vuoto!")
    @Size(max = 50)
    private String titolo;

    @Lob
    @Size(min = 10, max = 300)
    @NotBlank(message = "La descrizione non può essere vuota!")
    private String descrizione;

    
    @PastOrPresent(message = "La data di creazione non può essere nel futuro")
    private LocalDateTime dataCreazione;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Nota> note;

    @ManyToOne
    @JoinColumn(name = "operatore_id", nullable = false)
    @JsonBackReference
    private User operatore;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    @JsonBackReference
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "stato_id", nullable = false)
    private Stato stato;

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

    public String getDescrizione() {
        return this.descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Stato getStato() {
        return this.stato;
    }

    public void setStato(Stato stato) {
        this.stato = stato;
    }

}