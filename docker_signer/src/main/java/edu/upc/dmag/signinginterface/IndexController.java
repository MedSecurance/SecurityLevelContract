package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@Controller
public class IndexController {
    private final MinioMultipartUploadService uploadService;

    @SneakyThrows
    @GetMapping({"/upload-form", "/", "/index.html"})
    public String uploadForm(Model model, @AuthenticationPrincipal Jwt principal) {
        String project = "test";
        var uploadedFiles = uploadService.getListOfFiles(project);
        if (!uploadedFiles.isEmpty()) {
            Map<KnownDocuments, Boolean> files = new LinkedHashMap<>();
            for (KnownDocuments knownDocuments : KnownDocuments.values()) {
                files.put(knownDocuments, false);
            }
            for(S3Object s3Object: uploadedFiles) {
                files.put(KnownDocuments.valueOf(s3Object.key().replace(project+"/", "")), Boolean.TRUE);
            }

            model.addAttribute(
                "files",
                files.entrySet().toArray(
                        java.util.Map.Entry[]::new
                )
            );

            return "index_known_elements.html";
        } else {
            //model.addAttribute("message", principal.getTokenValue());
            return "index.html";
        }
    }

}
