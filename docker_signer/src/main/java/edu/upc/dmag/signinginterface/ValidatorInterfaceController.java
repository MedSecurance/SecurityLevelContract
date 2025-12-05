package edu.upc.dmag.signinginterface;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ValidatorInterfaceController {
    @GetMapping({"/{project}/validator/", "/{project}/validator"})
    public String handle(@PathVariable String project, Model model) {
        model.addAttribute("project", project);
        return "validate_contract.html";
    }

}
