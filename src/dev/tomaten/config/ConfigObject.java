package dev.tomaten.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import dev.tomaten.json.generic.JSONElement;
import dev.tomaten.json.generic.JSONObject;

class ConfigObject extends ConfigElement {
	private final Map<String, ConfigElement> map;
	private final Set<String> keys;
	
	public ConfigObject(String name, String fullName, Map<String, ConfigElement> map, String originalType) {
		super(name, fullName, originalType);
		this.map = map;
		this.keys = Collections.unmodifiableSet(map.keySet());
	}
	
	@Override
	public Type getType() {
		return Type.OBJECT;
	}
	
	@Override
	public ConfigElement get(String name) throws ConfigError {
		ConfigElement element = this.map.get(name);
		if (element == null) {
			String fullName = this.getFullName();
			throw new ConfigError("Missing configuration key '" + name + "'" + (fullName.isEmpty() ? "" : " for '" + fullName + "'"));
		}
		return element;
	}
	
	@Override
	public ConfigElement getOrNull(String name) {
		return this.map.get(name);
	}
	
	@Override
	public Collection<String> getKeys() {
		return this.keys;
	}
	
	@Override
	public String toString() {
		return super.toString() + "={ " + this.map.values().stream().map(e -> e.toString()).collect(Collectors.joining(", ")) + " }";
	}
	
	@Override
	public JSONElement toJSON() {
		JSONObject obj = new JSONObject();
		for (Entry<String, ConfigElement> entry : this.map.entrySet()) {
			JSONElement element = entry.getValue().toJSON();
			obj.set(entry.getKey(), element);
		}
		return obj;
	}
}
