package edu.upc.dmag.signinginterface;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SigningInterfaceController {
    @GetMapping({"/{project}/signer/"})
    public String uploadForm(@PathVariable String project, Model model) {
        model.addAttribute("project", project);
        return "sign_contract.html";
    }

}
