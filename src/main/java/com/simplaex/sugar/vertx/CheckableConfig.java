package com.simplaex.sugar.vertx;

public interface CheckableConfig {

  class InvalidConfigException extends Exception {
    public InvalidConfigException(
      final String configurationKey,
      final Object actualConfigurationValue,
      final String errorMessage
    ) {
      super(
        String.format("Invalid configuration item \"%s\" with value \"%s\"", configurationKey, actualConfigurationValue),
        new IllegalArgumentException(errorMessage)
      );
    }

    public InvalidConfigException(
      final String configurationKey,
      final Object actualConfigurationValue,
      final Exception cause
    ) {
      super(
        String.format("Invalid configuration item \"%s\" with value \"%s\"", configurationKey, actualConfigurationValue),
        cause
      );
    }
  }

  void check() throws InvalidConfigException;

}
