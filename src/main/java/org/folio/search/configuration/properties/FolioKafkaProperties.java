package org.folio.search.configuration.properties;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application properties for kafka message consumer.
 */
@Data
@ConfigurationProperties("application.kafka")
public class FolioKafkaProperties {

  /**
   * Map with settings for application kafka listeners.
   */
  private Map<String, KafkaListenerProperties> listener;

  /**
   * Specifies time to wait before reattempting delivery.
   */
  private long retryIntervalMs;

  /**
   * How many delivery attempts to perform when message failed.
   */
  private long retryDeliveryAttempts;

  /**
   * Contains set of settings for specific kafka listener.
   */
  @Data
  public static class KafkaListenerProperties {

    /**
     * List of topic to listen.
     */
    private String topics;

    /**
     * Number of concurrent consumers in service.
     */
    private String concurrency;

    /**
     * The group id.
     */
    private String groupId;
  }
}
