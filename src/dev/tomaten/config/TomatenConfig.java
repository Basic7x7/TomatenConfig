package dev.tomaten.config;

import static de.tomatengames.util.RequirementUtil.requireNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import de.tomatengames.lib.compiler.CompilerException;

public class TomatenConfig {
	
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
		if (filename.endsWith(".json")) {
			return ConfigType.JSON;
		}
		if (filename.endsWith(".toml")) {
			return ConfigType.TOML;
		}
		
		return ConfigType.AUTO_DETECT; // No matching file extension found
	}
	
	
	public static <C extends AbstractConfig<C>> C load(Supplier<C> configFactory, Path path) throws ConfigError {
		return load(configFactory, path, ConfigType.AUTO_DETECT);
	}
	
	public static <C extends AbstractConfig<C>> C load(Supplier<C> configFactory, Path path, ConfigType type) throws ConfigError {
		type = detectConfigType(path, type);
		if (type == ConfigType.AUTO_DETECT) {
			throw new ConfigError("Unsupported config file extension: " + path);
		}
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return load(configFactory, reader, type);
		} catch (IOException e) {
			throw new ConfigError("Failed to read the config file", e);
		}
	}
	
	public static <C extends AbstractConfig<C>> C load(Supplier<C> configFactory, Reader reader, ConfigType type) throws ConfigError {
		requireNotNull(type, "The config type ...");
		
		ConfigElement rootElement = null;
		switch (type) {
		case JSON:
			rootElement = JSONConfigParser.parse(reader);
			break;
		case TOML:
			try {
				rootElement = TOMLConfigParser.parse(reader);
			} catch (CompilerException | IOException e) {
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
