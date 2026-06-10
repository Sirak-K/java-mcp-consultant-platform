package mcp.server.foundation.spring_integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringCustomerDataCfg {

  @Bean
  public CustomerDataModeRTPolicy customerDataModeRTPolicy(
      @Value("${mcp.customer-data.mode}") String activeMode,
      @Value("${spring.flyway.locations:classpath:db/migration/postgresql}") String activeFlywayLocations,
      @Value("${spring.jpa.hibernate.ddl-auto:validate}") String activeSchemaValidationMode) {

    return new CustomerDataModeRTPolicy(
        activeMode,
        activeFlywayLocations,
        activeSchemaValidationMode);
  }
}
