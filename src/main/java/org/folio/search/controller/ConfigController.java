package org.folio.search.controller;

import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.search.domain.dto.LanguageConfig;
import org.folio.search.domain.dto.LanguageConfigs;
import org.folio.search.rest.resource.ConfigApi;
import org.folio.search.service.LanguageConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/search/")
public class ConfigController implements ConfigApi {
  private final LanguageConfigService languageConfigService;

  @Override
  public ResponseEntity<LanguageConfig> createLanguageConfig(@Valid LanguageConfig languageConfig) {
    log.info("Attempting to save language config {}", languageConfig);
    return ok(languageConfigService.create(languageConfig));
  }

  @Override
  public ResponseEntity<Void> deleteLanguageConfig(@Pattern(
    regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$") String id) {

    log.info("Attempting to remove language config {}", id);
    languageConfigService.delete(id);
    return noContent().build();
  }

  @Override
  public ResponseEntity<LanguageConfigs> getAllLanguageConfigs() {
    return ok(languageConfigService.getAll());
  }
}
