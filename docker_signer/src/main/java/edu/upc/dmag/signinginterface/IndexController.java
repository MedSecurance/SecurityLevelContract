package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@Slf4j
public class IndexController {
    private final MinioService uploadService;
    private final ProjectsContractStatus projectsContractStatus;

    @Value("${INSTANCE_ROLE:}")
    private String instanceRole;

    @SneakyThrows
    @GetMapping({"/upload-form", "/", "/index.html"})
    public String uploadForm(Model model, @AuthenticationPrincipal Jwt principal) {
        model.addAttribute("instanceRole", instanceRole);
        String project = "test";

        if ("provider".equalsIgnoreCase(instanceRole)) {
            var uploadedFiles = uploadService.getListOfFiles(project);

            model.addAttribute(
                    "has_files",
                    Boolean.TRUE
            );
            log.error("Organizations: {}", projectsContractStatus.getOrganizationsForProject(project));
            model.addAttribute(
                    "organizations",
                    projectsContractStatus.getOrganizationsForProject(project).toArray(String[]::new)
            );
            log.error("Documents to status: {}", projectsContractStatus.getDocumentsStatusForProject(project));
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
        return "index.html";
    }

}
