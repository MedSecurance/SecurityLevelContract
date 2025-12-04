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
public class ProjectIndexController {
    private final MinioService uploadService;
    private final ProjectsContractStatus projectsContractStatus;

    @Value("${INSTANCE_ROLE:}")
    private String instanceRole;

    @SneakyThrows
    @GetMapping({"/{project}", "/{project}/", "/{project}/index.html"})
    public String handle(@PathVariable String project, Model model) {
        model.addAttribute("instanceRole", instanceRole);
        model.addAttribute("project", project);

        if ("provider".equalsIgnoreCase(instanceRole)) {
            var uploadedFiles = uploadService.getListOfFiles(project);

            model.addAttribute(
                    "has_files",
                    !uploadedFiles.isEmpty()
            );
            log.debug("Organizations: {}", projectsContractStatus.getOrganizationsForProject(project));
            model.addAttribute(
                    "organizations",
                    projectsContractStatus.getOrganizationsForProject(project).toArray(String[]::new)
            );
            log.debug("Documents to status: {}", projectsContractStatus.getDocumentsStatusForProject(project));
            model.addAttribute(
                    "documents_to_status",
                    projectsContractStatus.getDocumentsStatusForProject(project)
            );
        } else {
            model.addAttribute(
                    "has_files",
                    Boolean.FALSE
            );
        }
        return "project_index.html";
    }

}
