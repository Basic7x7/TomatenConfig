package dev.tomaten.config;

/**
 * A {@link RuntimeException} thrown when an error occurs while loading or processing configuration data.
 *
 * @version 2025-02-16
 * @since 1.0
 */
public class ConfigError extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * Constructs a new {@link ConfigError} with the specified detail message and no cause.
	 * @param message The detail message.
	 */
	public ConfigError(String message) {
		super(message);
	}
	
	/**
	 * Constructs a new {@link ConfigError} with the specified detail message and cause.
	 * @param message The detail message.
	 * @param cause The cause. May be null.
	 */
	public ConfigError(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * Constructs a new {@link ConfigError} with the specified cause.
	 * @param cause The cause. May be null.
	 */
	public ConfigError(Throwable cause) {
		super(cause);
	}
}
