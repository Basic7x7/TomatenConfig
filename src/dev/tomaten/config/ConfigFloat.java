package dev.tomaten.config;

import dev.tomaten.json.generic.JSONElement;
import dev.tomaten.json.generic.JSONNumber;

class ConfigFloat extends ConfigElement {
	private final double value;
	
	public ConfigFloat(String name, String fullName, double value, String originalType) {
		super(name, fullName, originalType);
		this.value = value;
	}
	
	@Override
	public Type getType() {
		return Type.FLOAT;
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
	
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		return (other instanceof ConfigFloat) && this.value == ((ConfigFloat) other).value;
	}
	
	@Override
	public int hashCode() {
		return Double.hashCode(this.value);
	}
}
