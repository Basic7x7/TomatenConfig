package dev.tomaten.config.testtoml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import dev.tomaten.config.Config;
import dev.tomaten.config.ConfigError;
import dev.tomaten.config.ConfigType;
import dev.tomaten.config.TomatenConfig;
import dev.tomaten.json.generic.JSONArray;
import dev.tomaten.json.generic.JSONElement;
import dev.tomaten.json.generic.JSONObject;
import dev.tomaten.json.generic.JSONString;

class TOMLTestIntegration {
	
	public static void main(String[] args) {
		
		// Read the TOML input from System.in
		Config config;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
			config = TomatenConfig.load(Config::new, reader, ConfigType.TOML);
		} catch (IOException | ConfigError e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}
		
		try {
			// Convert the parsed config into a JSON representation and print it to System.out
			JSONElement json = toJSON(config);
			System.out.println(json);
		} catch (ConfigError e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}
		
		System.exit(0);
	}
	
	private static JSONElement toJSON(Config config) throws ConfigError {
		switch (config.getType()) {
			case OBJECT: {
				JSONObject obj = new JSONObject();
				for (String key : config.getKeys()) {
					Config value = config.getAny(key).orError();
					obj.set(key, toJSON(value));
				}
				return obj;
			}
			case LIST: {
				JSONArray array = new JSONArray();
				for (Config value : config) {
					array.add(toJSON(value));
				}
				return array;
			}
			default: {
				JSONObject obj = new JSONObject();
				obj.set("type", new JSONString(config.getOriginalType()));
				String value = config.getString().orError(); // Elemental types can be represented as string
				obj.set("value", new JSONString(value));
				return obj;
			}
		}
	}
}
