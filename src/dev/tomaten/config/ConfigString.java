package dev.tomaten.config;

import static de.tomatengames.util.RequirementUtil.requireNotNull;

class ConfigString extends ConfigElement {
	private final String value;
	
	public ConfigString(String fullName, String value, String originalType) {
		super(fullName, originalType);
		requireNotNull(value, "The string value ...");
		this.value = value;
	}
	
	@Override
	public Type getType() {
		return Type.STRING;
	}
	
	@Override
	public String getString() {
		return this.value;
	}
	
	@Override
	public String getStringOrDefault(String defaultValue) {
		return this.value;
	}
	
	@Override
	public long getLong() throws ConfigError {
		try {
			return Long.parseLong(this.value);
		} catch (NumberFormatException e) {
			throw new ConfigError(typeErrorMessage(Type.INTEGER), e);
		}
	}
	
	@Override
	public long getLongOrDefault(long defaultValue) {
		try {
			return Long.parseLong(this.value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	@Override
	public double getDouble() throws ConfigError {
		try {
			return Double.parseDouble(this.value);
		} catch (NumberFormatException e) {
			throw new ConfigError(typeErrorMessage(Type.DOUBLE), e);
		}
	}
	
	@Override
	public double getDoubleOrDefault(double defaultValue) {
		try {
			return Double.parseDouble(this.value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	@Override
	public boolean getBoolean() throws ConfigError {
		if ("true".equalsIgnoreCase(this.value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(this.value)) {
			return false;
		}
		throw new ConfigError(typeErrorMessage(Type.BOOLEAN));
	}
	
	@Override
	public boolean getBooleanOrDefault(boolean defaultValue) {
		if ("true".equalsIgnoreCase(this.value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(this.value)) {
			return false;
		}
		return defaultValue;
	}
	
	
	@Override
	public String toString() {
		return super.toString() + "=\"" + this.value.replaceAll("\"", "\\\"") + "\"";
	}
}
