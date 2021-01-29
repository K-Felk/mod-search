package org.folio.search.controller;

import static org.awaitility.Awaitility.await;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.folio.search.support.base.ApiEndpoints.languageConfig;
import static org.folio.search.utils.SearchConverterUtils.getMapValueByPath;
import static org.folio.search.utils.SearchUtils.INSTANCE_RESOURCE;
import static org.folio.search.utils.SearchUtils.getElasticsearchIndexName;
import static org.folio.search.utils.TestConstants.INVENTORY_INSTANCE_TOPIC;
import static org.folio.search.utils.TestConstants.TENANT_ID;
import static org.folio.search.utils.TestUtils.eventBody;
import static org.folio.search.utils.TestUtils.parseResponse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.folio.search.domain.dto.LanguageConfig;
import org.folio.search.domain.dto.LanguageConfigs;
import org.folio.search.sample.InstanceBuilder;
import org.folio.search.support.base.BaseIntegrationTest;
import org.folio.search.utils.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

@IntegrationTest
class ConfigControllerIT extends BaseIntegrationTest {
  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired
  private RestHighLevelClient elasticsearchClient;

  @BeforeEach
  void removeConfigs() {
    parseResponse(doGet(languageConfig()), LanguageConfigs.class)
      .getLanguageConfigs()
      .forEach(config -> doDelete(languageConfig() + "/{code}", config.getCode()));
  }

  @Test
  void canCreateLanguageConfig() throws Exception {
    final String languageCode = "eng";

    doPost(languageConfig(), new LanguageConfig().code(languageCode));

    doGet(languageConfig())
      .andExpect(jsonPath("totalRecords", is(1)))
      .andExpect(jsonPath("languageConfigs[0].code", is(languageCode)));
  }

  @Test
  void cannotAddLanguageIfNoAnalyzer() throws Exception {
    attemptPost(languageConfig(), new LanguageConfig().code("ukr"))
      .andExpect(status().is(422))
      .andExpect(jsonPath("errors[0].parameters.key", is("code")))
      .andExpect(jsonPath("errors[0].parameters.value", is("ukr")))
      .andExpect(jsonPath("errors[0].message",
        is("Language has no analyzer available")));
  }

  @Test
  void canRemoveLanguageConfig() throws Exception {
    final LanguageConfig language = new LanguageConfig().code("fre");

    doPost(languageConfig(), language);

    doDelete(languageConfig() + "/fre")
      .andExpect(status().isNoContent());
  }

  @Test
  void shouldUseConfiguredLanguagesDuringMapping() {
    final List<String> languageCodes = List.of("eng", "rus");
    for (String languageCode : languageCodes) {
      doPost(languageConfig(), new LanguageConfig().code(languageCode));
    }

    var newInstance = InstanceBuilder.builder()
      .languages(languageCodes)
      .title("This is title")
      .build();

    kafkaTemplate.send(INVENTORY_INSTANCE_TOPIC, newInstance.getId().toString(),
      eventBody(INSTANCE_RESOURCE, newInstance));

    final var indexedInstance = getIndexedInstanceById(newInstance.getId().toString());

    assertThat(getMapValueByPath("title.src", indexedInstance), is(newInstance.getTitle()));
    assertThat(getMapValueByPath("title.eng", indexedInstance), is(newInstance.getTitle()));
    assertThat(getMapValueByPath("title.rus", indexedInstance), is(newInstance.getTitle()));
  }

  @SneakyThrows
  private Map<String, Object> getIndexedInstanceById(String id) {
    final var searchRequest = new SearchRequest()
      .routing(TENANT_ID)
      .source(new SearchSourceBuilder().query(matchQuery("id", id)))
      .indices(getElasticsearchIndexName(INSTANCE_RESOURCE, TENANT_ID));

    await().until(() -> elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT)
      .getHits().getTotalHits().value > 0);

    return elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT).getHits()
      .getAt(0).getSourceAsMap();
  }
}
