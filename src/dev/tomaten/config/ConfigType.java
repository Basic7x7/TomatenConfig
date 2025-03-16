package dev.tomaten.config;

import java.util.Locale;

import de.tomatengames.util.ArrayUtil;

/**
 * The type of a configuration file.
 * This is used to determine which parser should be used when reading a configuration.
 */
public enum ConfigType {
	
	/**
	 * Automatically detect the config type based on the file extension of the configuration file.
	 * This type cannot be used if the input is not read from a file.
	 */
	AUTO_DETECT(),
	
	/**
	 * Uses the TOML parser to parse the configuration.
	 */
	TOML("toml"),
	
	/**
	 * Uses the JSON parser to parse the configuration.
	 * The parser will be able to read non-strict JSON.
	 */
	JSON("json", "js");
	
	
	private final String[] extensions;
	
	private ConfigType(String... extensions) {
		this.extensions = extensions;
	}
	
	/**
	 * Returns the file extensions that are associated with this config type.
	 * @return The file extensions. Not null.
	 */
	public String[] getExtensions() {
		return extensions;
	}
	
	
	/**
	 * Returns the config type that corresponds to the given file extension.
	 * @param extension The file extension. May be null.
	 * @return The config type that corresponds to the given file extension.
	 * Null if the file extension is not recognized.
	 */
	public static ConfigType fromExtension(String extension) {
		if (extension == null) {
			return null;
		}
		extension = extension.toLowerCase(Locale.ROOT);
		for (ConfigType type : ConfigType.values()) {
			if (ArrayUtil.containsEqual(type.getExtensions(), extension)) {
				return type;
			}
		}
		return null;
	}
	
	/**
	 * Returns the config type that corresponds to the extension of the given file name.
	 * @param fileName The file name. May be null.
	 * @return The config type that corresponds to the extension of the given file name.
	 * Null if the extension is not recognized.
	 */
	public static ConfigType fromFileName(String fileName) {
		if (fileName == null) {
			return null;
		}
		fileName = fileName.toLowerCase(Locale.ROOT);
		for (ConfigType type : ConfigType.values()) {
			for (String ext : type.getExtensions()) {
				if (fileName.endsWith("." + ext)) {
					return type;
				}
			}
		}
		return null;
	}
	
}
