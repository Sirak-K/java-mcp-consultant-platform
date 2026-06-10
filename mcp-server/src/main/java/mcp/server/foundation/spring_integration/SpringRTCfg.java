package mcp.server.foundation.spring_integration;

import mcp.server.foundation.rpc.RPCCapaDscr;
import mcp.server.foundation.rpc.RPCJsonEntry;
import mcp.server.foundation.rpc.RPCJsonSeria;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringRTCfg {

  // =========================================================
  // RPC codec
  // =========================================================

  @Bean
  public RPCJsonSeria rpcJsonSeria() {
    return new RPCJsonSeria();
  }

  @Bean
  public RPCJsonEntry rpcJsonEntrypoint(
      RPCJsonSeria serializer) {

    return new RPCJsonEntry(serializer);
  }

  // =========================================================
  // RPC descriptor
  // =========================================================

  @Bean
  public RPCCapaDscr rpcCapaDscr() {
    return new RPCCapaDscr();
  }
}
