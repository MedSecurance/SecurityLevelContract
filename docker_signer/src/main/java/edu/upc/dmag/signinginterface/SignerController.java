package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;

@Controller
@RequestMapping("/{project}/signer")
@RequiredArgsConstructor
@Slf4j
public class SignerController {
    private final Signer signer;

    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadFile(
        @PathVariable String project,
        @RequestParam("to_sign") MultipartFile file,
        Model model
    ) {
        model.addAttribute("project", project);
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            // Read original file content
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.debug("about to sign document");
            String modifiedContent = signer.sign(project, content);
            log.debug("document is signed");
            log.info("User '{}' signed document '{}'", retrieveUsernameFromSecurityContext(), file.getOriginalFilename());
            return Utils.generateAnswer(modifiedContent, "signed_" + file.getOriginalFilename());

        } catch (IOException | ParserConfigurationException | SAXException | TransformerException | XMLStreamException |
                 CertificateException e) {
            log.error("failed to sign document", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String retrieveUsernameFromSecurityContext() {
        return ((DefaultOidcUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserInfo().getClaim("preferred_username");
    }
}
