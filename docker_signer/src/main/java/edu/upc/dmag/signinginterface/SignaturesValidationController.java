package edu.upc.dmag.signinginterface;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/{project}/validator")
@RequiredArgsConstructor
public class SignaturesValidationController {
    private final Signer signer;

    @PostMapping("/upload")
    public String verifyUploadedSignatures(
        @RequestParam("filetoverify") MultipartFile file,
        @PathVariable String project,
        Model model,
        HttpServletRequest request
    ) throws IOException {
        model.addAttribute("project", project);
        var tmpFile = Utils.createTempFile("upload-", ".tmp", request);
        file.transferTo(tmpFile);

        Boolean signatureValid = signer.validate(project, tmpFile);
        if (signatureValid == null) {
            model.addAttribute("hasSignatures", false);
        } else {
            model.addAttribute("hasSignatures", true);
            model.addAttribute("signatureValid", signatureValid);
        }
        return "validate_contract.html";
    }
}
