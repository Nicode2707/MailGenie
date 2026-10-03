package com.email.writer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class CampaignTrackingController {

    private final CampaignVariantRepository variantRepository;
    
    // 1x1 transparent GIF base64
    private static final byte[] PIXEL_BYTES = java.util.Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    @GetMapping("/open/{variantId}")
    public ResponseEntity<byte[]> trackOpen(@PathVariable Long variantId) {
        Optional<CampaignVariant> variantOpt = variantRepository.findById(variantId);
        if (variantOpt.isPresent()) {
            CampaignVariant variant = variantOpt.get();
            variant.setOpenCount(variant.getOpenCount() + 1);
            variantRepository.save(variant);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_GIF);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return new ResponseEntity<>(PIXEL_BYTES, headers, HttpStatus.OK);
    }

    @GetMapping("/click/{variantId}")
    public ResponseEntity<Void> trackClick(@PathVariable Long variantId, @RequestParam(name = "url", required = true) String targetUrl) {
        Optional<CampaignVariant> variantOpt = variantRepository.findById(variantId);
        if (variantOpt.isPresent()) {
            CampaignVariant variant = variantOpt.get();
            variant.setClickCount(variant.getClickCount() + 1);
            variantRepository.save(variant);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
