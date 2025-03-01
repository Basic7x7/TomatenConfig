package dev.tomaten.config;

import dev.tomaten.json.generic.JSONElement;
import dev.tomaten.json.generic.JSONNumber;

class ConfigInt extends ConfigElement {
	private final long value;
	
	public ConfigInt(String name, String fullName, long value, String originalType) {
		super(name, fullName, originalType);
		this.value = value;
	}
	
	@Override
	public Type getType() {
		return Type.INTEGER;
	}
	
	@Override
	public long getLong() {
		return this.value;
	}
	
	@Override
	public long getLongOrDefault(long defaultValue) {
		return this.value;
	}
	
	@Override
	public double getDouble() {
		// Double values can also be specified as integers.
		return (double) this.value;
	}
	
	@Override
	public double getDoubleOrDefault(double defaultValue) {
		return (double) this.value;
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
