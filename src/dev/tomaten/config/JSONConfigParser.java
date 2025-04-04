package dev.tomaten.config;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

import dev.tomaten.json.JSONReader;
import dev.tomaten.json.JSONReader.ElementType;

class JSONConfigParser {
	
	public static ConfigElement parse(Reader r) throws ConfigError {
		try {
			JSONReader reader = new JSONReader(r);
			reader.setStrict(false);
			return read(reader, "root", "");
		} catch (IOException e) {
			throw new ConfigError("Failed to read JSON config", e);
		}
	}
	
	private static ConfigElement read(JSONReader reader, String name, String fullName) throws IOException, ConfigError {
		ElementType type = reader.type();
		try {
			switch (type) {
				case STRING: {
					String value = reader.readString(Long.MAX_VALUE);
					return new ConfigString(name, fullName, value, "string");
				}
				case NUMBER: {
					String numberStr = reader.readNumberString(Long.MAX_VALUE);
					// If the number has no special character (like '.'), try to parse it as long.
					if (numberStr.chars().allMatch(c -> ('0' <= c && c <= '9') || c == '-' || c == '+')) {
						try {
							long longValue = Long.parseLong(numberStr);
							return new ConfigInt(name, fullName, longValue, "number");
						} catch (NumberFormatException e) {
							// continue with parseDouble
						}
					}
					// If the number could not be parsed as long, try to parse it as double.
					try {
						double doubleValue = Double.parseDouble(numberStr);
						return new ConfigFloat(name, fullName, doubleValue, "number");
					} catch (NumberFormatException e) {
						throw new ConfigError("Could not parse JSON number" + (fullName.isEmpty() ? "" : " for '" + fullName + "'"), e);
					}
				}
				case FALSE: {
					reader.readFalse();
					return new ConfigBoolean(name, fullName, false, "boolean");
				}
				case TRUE: {
					reader.readTrue();
					return new ConfigBoolean(name, fullName, true, "boolean");
				}
				case NULL: {
					reader.readNull();
					return null; // There is no config-null value
				}
				case ARRAY: {
					ArrayList<ConfigElement> elements = new ArrayList<>();
					reader.enterArray();
					while (reader.nextEntry()) {
						String key = String.valueOf(elements.size());
						ConfigElement e = read(reader, key, (fullName.isEmpty() ? "" : fullName + ".") + key);
						if (e != null) {
							elements.add(e);
						}
					}
					reader.exitArray();
					return new ConfigList(name, fullName, elements, "array");
				}
				case OBJECT: {
					HashMap<String, ConfigElement> map = new HashMap<>();
					reader.enterObject();
					while (reader.nextEntry()) {
						String key = reader.readKey(Long.MAX_VALUE);
						ConfigElement e = read(reader, key, (fullName.isEmpty() ? "" : fullName + ".") + key);
						if (e != null) {
							map.put(key, e);
						}
					}
					reader.exitObject();
					return new ConfigObject(name, fullName, map, "object");
				}
				case INVALID: {
					throw new ConfigError(msgReadError(null, fullName) + ": Invalid JSON");
				}
			}
		} catch (IOException e) {
			throw new ConfigError(msgReadError(type, fullName), e);
		}
		
		// Should only happen if new JSON ElementTypes are added
		throw new ConfigError(msgReadError(null, fullName) + ": Unknown JSON type");
	}
	
	private static String msgReadError(ElementType type, String fullName) {
		return "Failed to read JSON config" + (type != null ? " " + type.toString() : "") +
				(fullName.isEmpty() ? "" : " at '" + fullName + "'");
	}
	
}
