package dev.tomaten.config;

import static de.tomatengames.util.RequirementUtil.requireNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.tomatengames.lib.compiler.CompilerException;

/**
 * The main class of the TomatenConfig project.
 * <p>
 * This class provides methods to load configurations from files and other sources.
 * 
 * @version 2025-03-16 last modified
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
	
	
	/**
	 * Creates a configuration that consists of a single root object that has no entries.
	 * @param <C> The type of the configuration.
	 * @param configFactory A factory that creates an uninitialized instance of the configuration. Not null. For example, {@code Config::new}.
	 * @return The configuration that was created. Not null.
	 */
	public static <C extends AbstractConfig<C>> C loadEmpty(Supplier<C> configFactory) {
		requireNotNull(configFactory, "The config factory ...");
		C config = configFactory.get();
		requireNotNull(config, "The config factory returned null");
		
		ConfigObject rootElement = new ConfigObject("root", "", Collections.emptyMap(), null);
		config.init(configFactory, rootElement);
		return config;
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
	 * @param type The {@link ConfigType} that determines which parser should be used. Null is identical to {@link ConfigType#AUTO_DETECT}.
	 * @return The configuration that was read. Not null.
	 * @throws ConfigError If the configuration could not be read or parsed.
	 * This may also wrap an {@link IOException}.
	 */
	public static <C extends AbstractConfig<C>> C load(Supplier<C> configFactory, Path path, ConfigType type) throws ConfigError {
		if (type == ConfigType.AUTO_DETECT || type == null) {
			Path filenamePath = path.getFileName();
			if (filenamePath != null) {
				String filename = filenamePath.toString();
				type = ConfigType.fromFileName(filename);
			}
		}
		if (type == ConfigType.AUTO_DETECT || type == null) {
			throw new ConfigError("Failed to detect config type from file name: " + path);
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
		if (type == null) {
			type = ConfigType.AUTO_DETECT;
		}
		
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
	
	
	/**
	 * Reads a configuration from the specified input string.
	 * The {@link ConfigType} determines which parser should be used.
	 * @param <C> The type of the configuration to read.
	 * @param configFactory A factory that creates an uninitialized instance of the configuration to read. Not null. For example, {@code Config::new}.
	 * @param input The string to read the configuration from. Not null.
	 * @param type The {@link ConfigType} that determines which parser should be used. Not null.
	 * @return The configuration that was read. Not null.
	 * @throws ConfigError If the configuration could not be parsed.
	 * This may also wrap an {@link IOException}.
	 */
	public static <C extends AbstractConfig<C>> C load(Supplier<C> configFactory, String input, ConfigType type) throws ConfigError {
		try (Reader reader = new StringReader(input)) {
			return load(configFactory, reader, null, type);
		} catch (IOException e) {
			throw new ConfigError("Failed to read the config string", e);
		}
	}
	
	
	/**
	 * Reads a configuration with the specified base name from the specified directory.
	 * This method searches for a file with the specified base name and an extension that matches a {@link ConfigType}.
	 * If multiple files match the specified base name and have appropriate extensions, a {@link ConfigError} is thrown.
	 * The file extension of the found file determines the parser that will be used.
	 * The file is expected to be encoded in UTF-8.
	 * <p>
	 * For example, if the directory is {@code dir/} and the base name is {@code "config"},
	 * this method may load a config at {@code dir/config.toml} or {@code dir/config.json}.
	 * @param <C> The type of the configuration to read.
	 * @param configFactory A factory that creates an uninitialized instance of the configuration to read. Not null. For example, {@code Config::new}.
	 * @param configDir The directory to search for the configuration file. Not null.
	 * @param configBaseName The base name of the configuration file, excluding the file extension. Not null.
	 * @return The configuration that was read. Not null.
	 * @throws ConfigError If the configuration could not be found, read or parsed.
	 * This may also wrap an {@link IOException}.
	 */
	public static <C extends AbstractConfig<C>> C load(Supplier<C> configFactory, Path configDir, String configBaseName) throws ConfigError {
		FoundPath found = findConfigPath(configDir, configBaseName);
		if (found == null) {
			throw new ConfigError("Config '" + configBaseName + "' not found at '" + configDir + "'");
		}
		return load(configFactory, found.path, found.type);
	}
	
	private static FoundPath findConfigPath(Path configDir, String fileBaseName) throws ConfigError {
		if (configDir == null || fileBaseName == null) {
			return null;
		}
		try {
			FoundPath[] found = Files.list(configDir).map(path -> {
				Path fileNamePath = path.getFileName();
				if (fileNamePath == null) {
					return null;
				}
				String fileName = fileNamePath.toString();
				if (!fileName.startsWith(fileBaseName + ".")) {
					return null;
				}
				String extension = fileName.substring(fileBaseName.length() + 1);
				ConfigType type = ConfigType.fromExtension(extension);
				if (type == null) {
					return null;
				}
				if (!Files.isRegularFile(path)) {
					return null;
				}
				return new FoundPath(path, fileName, type);
			}).filter(Objects::nonNull).toArray(FoundPath[]::new);
			
			if (found.length <= 0) {
				return null;
			}
			if (found.length == 1) {
				return found[0];
			}
			throw new ConfigError("Config '" + fileBaseName + "' is ambiguous in the directory '" + configDir + "': Found " +
				Arrays.stream(found).map(f -> "'" + f.fileName + "'").collect(Collectors.joining(", ")));
			
		} catch (IOException e) {
			throw new ConfigError("Failed to find config '" + fileBaseName + "' in directory '" + configDir + "'", e);
		}
	}
	
	private static class FoundPath {
		private final Path path;
		private final String fileName;
		private final ConfigType type;
		
		public FoundPath(Path path, String fileName, ConfigType type) {
			this.path = path;
			this.fileName = fileName;
			this.type = type;
		}
	}
}
