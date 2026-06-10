package mcp.server.domain.reference_data.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import mcp.server.domain.reference_data.web.DomainStaticReferenceDataDto.StaticReferenceDataView;
import mcp.server.domain.reference_data.application.DomainStaticReferenceDataService;

import java.util.Objects;

@RestController
public final class ReferenceDataApiCtrl {

  private final DomainStaticReferenceDataService referenceDataService;

  public ReferenceDataApiCtrl(DomainStaticReferenceDataService referenceDataService) {
    this.referenceDataService = Objects.requireNonNull(referenceDataService, "referenceDataService");
  }

  @GetMapping("/api/public/reference-data")
  public StaticReferenceDataView referenceData() {
    return referenceDataService.referenceData();
  }
}
