package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;

@RequiredArgsConstructor
@Controller
@Slf4j
public class IndexController {
    private final ProjectsContractStatus projectsContractStatus;

    @SneakyThrows
    @GetMapping({"/", "/index.html"})
    public String handle(Model model) {

        model.addAttribute("projects", projectsContractStatus.getProjectNames());
        log.debug("Rendering index with projects: {}", projectsContractStatus.getProjectNames());
        return "index.html";
    }

}
