package dev.tomaten.config;

import dev.tomaten.json.generic.JSONElement;
import dev.tomaten.json.generic.JSONNumber;

class ConfigDouble extends ConfigElement {
	private final double value;
	
	public ConfigDouble(String fullName, double value, String originalType) {
		super(fullName, originalType);
		this.value = value;
	}
	
	@Override
	public Type getType() {
		return Type.DOUBLE;
	}
	
	@Override
	public double getDouble() {
		return this.value;
	}
	
	@Override
	public double getDoubleOrDefault(double defaultValue) {
		return this.value;
	}
	
	@Override
	public String getString() throws ConfigError {
		return String.valueOf(this.value);
	}
	
	@Override
	public String getStringOrDefault(String defaultValue) {
		return String.valueOf(this.value);
	}
	
	@Override
	public String toString() {
		return super.toString() + "=" + this.value;
	}
	
	@Override
	public JSONElement toJSON() {
		return new JSONNumber(this.value);
	}
}
