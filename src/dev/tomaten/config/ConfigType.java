package dev.tomaten.config;

/**
 * The type of a configuration file.
 * This is used to determine which parser should be used when reading a configuration.
 */
public enum ConfigType {
	
	/**
	 * Automatically detect the config type based on the file extension of the configuration file.
	 * This type cannot be used if the input is not read from a file.
	 */
	AUTO_DETECT,
	
	/**
	 * Uses the JSON parser to parse the configuration.
	 * The parser will be able to read non-strict JSON.
	 */
	JSON,
	
	/**
	 * Uses the TOML parser to parse the configuration.
	 */
	TOML
}
