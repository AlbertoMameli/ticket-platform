package org.lessons.milestone4.ticket.controller;


import org.lessons.milestone4.ticket.model.Nota;
import org.lessons.milestone4.ticket.repository.NotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/note")
public class NotaController {

    @Autowired
    NotaRepository notaRepository;



    @PostMapping
    public String store(@Valid @ModelAttribute("nota") Nota formNota,BindingResult bindingResult, Model model){
        if (bindingResult.hasErrors()){
            model.addAttribute("nota", formNota);
            return "note/create";
        }
        notaRepository.save(formNota);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model){
        model.addAttribute("nota", notaRepository.findById(id).get());
        return "note/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @Valid @ModelAttribute("nota") Nota formNota, BindingResult bindingResult, Model model){

        if (bindingResult.hasErrors()){
            model.addAttribute("nota", formNota);
            return "nota/edit";
        }

        notaRepository.save(formNota);
        return "redirect:/tickets";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id,Model model){
        notaRepository.deleteById(id);
        return "redirect:/tickets/{id}";
    }


    
}