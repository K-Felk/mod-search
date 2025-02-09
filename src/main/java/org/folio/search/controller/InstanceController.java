package org.folio.search.controller;

import static org.folio.search.model.service.CqlResourceIdsRequest.INSTANCE_ID_PATH;
import static org.folio.search.utils.SearchUtils.INSTANCE_RESOURCE;

import lombok.RequiredArgsConstructor;
import org.folio.search.domain.dto.Instance;
import org.folio.search.domain.dto.InstanceSearchResult;
import org.folio.search.model.service.CqlResourceIdsRequest;
import org.folio.search.model.service.CqlSearchRequest;
import org.folio.search.rest.resource.InstancesApi;
import org.folio.search.service.ResourceIdsStreamHelper;
import org.folio.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller with set of endpoints for manipulating with Elasticsearch search API.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class InstanceController implements InstancesApi {

  private final SearchService searchService;
  private final ResourceIdsStreamHelper resourceIdsStreamHelper;

  @Override
  public ResponseEntity<InstanceSearchResult> searchInstances(String tenantId, String query, Integer limit,
                                                              Integer offset, Boolean expandAll) {
    var searchRequest = CqlSearchRequest.of(Instance.class, tenantId, query, limit, offset, expandAll);
    var result = searchService.search(searchRequest);
    return ResponseEntity.ok(new InstanceSearchResult()
      .instances(result.getRecords())
      .totalRecords(result.getTotalRecords()));
  }

  @Override
  public ResponseEntity<Void> getInstanceIds(String query, String tenantId, String contentType) {
    var request = CqlResourceIdsRequest.of(INSTANCE_RESOURCE, tenantId, query, INSTANCE_ID_PATH);
    return resourceIdsStreamHelper.streamResourceIds(request, contentType);
  }
}
