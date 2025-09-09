package edu.upc.dmag.signinginterface;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class ValidatorController {
    @GetMapping({"/validator"})
    public String uploadForm(Model model, @AuthenticationPrincipal Jwt principal) {
        //model.addAttribute("message", principal.getTokenValue());
        return "validate_contract.html";
    }

}
