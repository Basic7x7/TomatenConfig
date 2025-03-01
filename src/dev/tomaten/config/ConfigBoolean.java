package dev.tomaten.config;

import dev.tomaten.json.generic.JSONBoolean;
import dev.tomaten.json.generic.JSONElement;

class ConfigBoolean extends ConfigElement {
	private final boolean value;
	
	public ConfigBoolean(String name, String fullName, boolean value, String originalType) {
		super(name, fullName, originalType);
		this.value = value;
	}
	
	@Override
	public boolean getBoolean() {
		return this.value;
	}
	
	@Override
	public boolean getBooleanOrDefault(boolean defaultValue) {
		return this.value;
	}
	
	@Override
	public String getString() throws ConfigError {
		return this.value ? "true" : "false";
	}
	
	@Override
	public String getStringOrDefault(String defaultValue) {
		return this.value ? "true" : "false";
	}
	
	@Override
	public Type getType() {
		return Type.BOOLEAN;
	}
	
	@Override
	public String toString() {
		return super.toString() + "=" + this.value;
	}
	
	@Override
	public JSONElement toJSON() {
		return new JSONBoolean(this.value);
	}
}
