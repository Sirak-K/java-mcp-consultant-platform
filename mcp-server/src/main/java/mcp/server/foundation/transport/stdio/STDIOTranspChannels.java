package mcp.server.foundation.transport.stdio;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Objects;

public record STDIOTranspChannels(
    InputStream inputStream,
    PrintStream outputStream) {

  public STDIOTranspChannels {
    inputStream = Objects.requireNonNull(inputStream, "inputStream");
    outputStream = Objects.requireNonNull(outputStream, "outputStream");
  }
}
