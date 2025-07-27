package org.lessons.milestone4.ticket.controller;

import org.lessons.milestone4.ticket.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {
    
    @GetMapping
    public String homepage(Model model){
        model.addAttribute("user", new User());
        return "pages/home";
    }
}
