package dev.tomaten.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class ConfigElement {
	private final String fullName;
	private final String originalType;
	
	protected ConfigElement(String fullName, String originalType) {
		this.fullName = fullName;
		this.originalType = originalType;
	}
	
	public String getFullName() {
		return fullName;
	}
	
	public abstract ConfigElement.Type getType();
	
	public String getOriginalType() {
		return this.originalType;
	}
	
	
	protected String typeErrorMessage(Type expected) {
		String fullName = this.getFullName();
		return "Invalid config value" + (fullName.isEmpty() ? "" : " for '" + this.getFullName() + "'") +
				": Expected " + expected.toString() + ", but found " + this.getType().toString();
	}
	
	public String getString() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.STRING));
	}
	
	public String getStringOrDefault(String defaultValue) {
		return defaultValue;
	}
	
	public double getDouble() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.DOUBLE));
	}
	
	public double getDoubleOrDefault(double defaultValue) {
		return defaultValue;
	}
	
	public long getLong() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.INTEGER));
	}
	
	public long getLongOrDefault(long defaultValue) {
		return defaultValue;
	}
	
	public int getInt() throws ConfigError {
		long value = this.getLong();
		if (value < (long) Integer.MIN_VALUE || value > (long) Integer.MAX_VALUE) {
			String fullName = this.getFullName();
			throw new ConfigError("Invalid config value" + (fullName.isEmpty() ? "" : " for '" + this.getFullName() + "'") +
					": The number " + value + " cannot be represented as a 32-bit integer");
		}
		return (int) value;
	}
	
	public int getIntOrDefault(int defaultValue) throws ConfigError {
		long value = this.getLongOrDefault(defaultValue);
		if (value < (long) Integer.MIN_VALUE || value > (long) Integer.MAX_VALUE) {
			return defaultValue;
		}
		return (int) value;
	}
	
	public boolean getBoolean() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.BOOLEAN));
	}
	
	public boolean getBooleanOrDefault(boolean defaultValue) {
		return defaultValue;
	}
	
	
	public ConfigElement get(String name) throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.OBJECT));
	}
	
	public ConfigElement getOrNull(String name) {
		return null;
	}
	
	public Collection<String> getKeys() {
		return Collections.emptyList();
	}
	
	
	public ConfigElement get(int index) throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.LIST));
	}
	
	public ConfigElement getOrNull(int index) {
		return null;
	}
	
	public int size() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.LIST));
	}
	
	public List<ConfigElement> getList() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.LIST));
	}
	
	
	@Override
	public String toString() {
		return this.fullName + "[" + this.getType().name() + (this.originalType != null ? "/" + this.originalType : "") + "]";
	}
	
	
	public enum Type {
		OBJECT,
		LIST,
		STRING,
		DOUBLE,
		INTEGER,
		BOOLEAN
	}
}
