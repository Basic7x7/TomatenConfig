package dev.tomaten.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

class ConfigObject extends ConfigElement {
	private final Map<String, ConfigElement> map;
	private final Set<String> keys;
	
	public ConfigObject(String fullName, Map<String, ConfigElement> map) {
		super(fullName);
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
	
}
