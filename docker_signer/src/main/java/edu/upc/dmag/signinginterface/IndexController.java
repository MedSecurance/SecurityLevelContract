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
        var uploadedFiles = uploadService.getListOfFiles(project);
        if (false) {
        //if (!uploadedFiles.isEmpty()) {
            Map<KnownDocuments, Boolean> files = new LinkedHashMap<>();
            for (KnownDocuments knownDocuments : KnownDocuments.values()) {
                files.put(knownDocuments, false);
            }
            for(S3Object s3Object: uploadedFiles) {
                try {
                    files.put(KnownDocuments.valueOf(s3Object.key().replace(project + "/", "")), Boolean.TRUE);
                }catch (Exception ignore) {}
            }

            model.addAttribute(
                "files",
                files.entrySet().toArray(
                        java.util.Map.Entry[]::new
                )
            );

            model.addAttribute(
                "has_files",
                Boolean.TRUE
            );
            model.addAttribute(
                "organizations",
                projectsContractStatus.getOrganizationsForProject(project).toArray(String[]::new)
            );
            model.addAttribute(
                "documents_to_status",
                projectsContractStatus.getDocumentsStatusForProject(project)
            );

            return "index_known_elements.html";
        } else {
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

            return "index.html";
        }
    }

}
