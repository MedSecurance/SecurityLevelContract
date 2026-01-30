package edu.upc.dmag.signinginterface;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Controller
@RequestMapping("/{project}/validator")
@RequiredArgsConstructor
@Slf4j
public class SignaturesValidationController {
    private final Signer signer;

    @PostMapping("/upload")
    public String verifyUploadedSignatures(
        @RequestParam("filetoverify") MultipartFile file,
        @PathVariable String project,
        Model model,
        HttpServletRequest request
    ) throws IOException, NoSuchAlgorithmException {
        model.addAttribute("project", project);
        var tmpFile = Utils.createTempFile("upload-", ".tmp", request);
        file.transferTo(tmpFile);

        Boolean signatureValid = signer.validate(project, tmpFile, request);
        if (signatureValid == null) {
            model.addAttribute("hasSignatures", false);
        } else {
            model.addAttribute("hasSignatures", true);
            model.addAttribute("signatureValid", signatureValid);
        }
        return "validate_contract.html";
    }

    @PostMapping("/extractEvidence")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> extractEvidence(
            @RequestParam("filetoverify") MultipartFile file,
            @PathVariable String project,
            Model model,
            HttpServletRequest request
    ) throws IOException, NoSuchAlgorithmException {
        var tmpFile = Utils.createTempFile("upload-", ".tmp", request);
        file.transferTo(tmpFile);
        Boolean signatureValid = signer.validate(project, tmpFile, request);
        log.info("Signature validity: {}", signatureValid);
        if (signatureValid != null && signatureValid) {
            var evidences = signer.extractEvidence(tmpFile);
            log.info("Extracted evidences: {}", evidences);
            return ResponseEntity.ok(evidences);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", true));
        }
    }
}
