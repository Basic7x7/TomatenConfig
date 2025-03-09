package dev.tomaten.config;

import static de.tomatengames.util.RequirementUtil.requireNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import de.tomatengames.lib.compiler.CompilerException;

/**
 * The main class of the TomatenConfig project.
 * <p>
 * This class provides methods to load configurations from files and other sources.
 * 
 * @version 2025-03-04 last modified
 * @version 2025-02-15 created
 * @since 1.0
 */
public class TomatenConfig {
	
	// Static class
	private TomatenConfig() {
	}
	
	/**
	 * Returns the currently running version of TomatenConfig.
	 * @return The version string. Not null.
	 */
	public static String getVersion() {
		return "1.0-dev";
	}
	
	
	private static ConfigType detectConfigType(Path path, ConfigType typeHint) {
		if (typeHint != ConfigType.AUTO_DETECT) {
			return typeHint;
		}
		
		Path filenamePath = path.getFileName();
		if (filenamePath == null) {
			return ConfigType.AUTO_DETECT; // Cannot get filename
		}
		String filename = filenamePath.toString();
		if (filename.endsWith(".json") || filename.endsWith(".js")) {
			return ConfigType.JSON;
		}
		if (filename.endsWith(".toml")) {
			return ConfigType.TOML;
		}
		
		return ConfigType.AUTO_DETECT; // No matching file extension found
	}
	
	
	/**
	 * Reads a configuration from the specified {@link Path}.
	 * The file extension of the Path determines the parser to use.
	 * The file is expected to be encoded in UTF-8.
	 * @param <C> The type of the configuration to read.
	 * @param configFactory A factory that creates an uninitialized instance of the configuration to read. Not null. For example, {@code Config::new}.
	 * @param path The {@link Path} to the configuration file that should be read. Not null.
	 * @return The configuration that was read. Not null.
	 * @throws ConfigError If the configuration could not be read or parsed.
	 * This may also wrap an {@link IOException}.
	 */
	public static <C extends AbstractConfig<C>> C load(Supplier<C> configFactory, Path path) throws ConfigError {
		return load(configFactory, path, ConfigType.AUTO_DETECT);
	}
	
	/**
	 * Reads a configuration from the specified {@link Path}.
	 * The {@link ConfigType} determines which parser should be used.
	 * The file is expected to be encoded in UTF-8.
	 * @param <C> The type of the configuration to read.
	 * @param configFactory A factory that creates an uninitialized instance of the configuration to read. Not null. For example, {@code Config::new}.
	 * @param path The {@link Path} to the configuration file that should be read. Not null.
	 * @param type The {@link ConfigType} that determines which parser should be used. Not null.
	 * @return The configuration that was read. Not null.
	 * @throws ConfigError If the configuration could not be read or parsed.
	 * This may also wrap an {@link IOException}.
	 */
	public static <C extends AbstractConfig<C>> C load(Supplier<C> configFactory, Path path, ConfigType type) throws ConfigError {
		type = detectConfigType(path, type);
		if (type == ConfigType.AUTO_DETECT) {
			throw new ConfigError("Unsupported config file extension: " + path);
		}
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return load(configFactory, reader, path.toString(), type);
		} catch (IOException e) {
			throw new ConfigError("Failed to read the config file", e);
		}
	}
	
	/**
	 * Reads a configuration from the specified {@link Reader}.
	 * The {@link ConfigType} determines which parser should be used.
	 * @param <C> The type of the configuration to read.
	 * @param configFactory A factory that creates an uninitialized instance of the configuration to read. Not null. For example, {@code Config::new}.
	 * @param reader The {@link Reader} to read the configuration from. Not null.
	 * @param resourceName The name of the resource to read. May be null. The resource name is used to improve error messages.
	 * @param type The {@link ConfigType} that determines which parser should be used. Not null.
	 * @return The configuration that was read. Not null.
	 * @throws ConfigError If the configuration could not be read or parsed.
	 * This may also wrap an {@link IOException}.
	 */
	public static <C extends AbstractConfig<C>> C load(Supplier<C> configFactory, Reader reader, String resourceName, ConfigType type) throws ConfigError {
		requireNotNull(type, "The config type ...");
		
		ConfigElement rootElement = null;
		switch (type) {
		case JSON:
			rootElement = JSONConfigParser.parse(reader);
			break;
		case TOML:
			try {
				rootElement = TOMLConfigParser.parse(reader);
			} catch (CompilerException e) {
				throw new ConfigError(e.applyLocation(resourceName));
			} catch (IOException e) {
				throw new ConfigError(e);
			}
			break;
		case AUTO_DETECT:
			throw new ConfigError("Cannot detect config file type");
		}
		
		if (rootElement == null) {
			throw new ConfigError("Config is not present (null)");
		}
		
		C config = configFactory.get();
		if (config == null) {
			throw new ConfigError("The config factory returned null");
		}
		
		config.init(configFactory, rootElement);
		return config;
	}
	
}
