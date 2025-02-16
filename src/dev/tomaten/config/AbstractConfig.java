package dev.tomaten.config;

import static de.tomatengames.util.RequirementUtil.requireNotNull;

import java.util.function.Supplier;

import dev.tomaten.config.ConfigElement.Type;

public abstract class AbstractConfig<Self extends AbstractConfig<Self>> {
	private Supplier<Self> factory;
	private ConfigElement data;
	
	
	protected AbstractConfig() {
	}
	
	protected void init(Supplier<Self> factory, ConfigElement data) {
		this.factory = factory;
		this.data = data;
	}
	
	
	protected ConfigElement navigate(String name, boolean allowNull) throws ConfigError {
		requireNotNull(name, "The name ...");
		ConfigElement current = this.data;
		int n = name.length();
		int start = 0;
		int end = 0; // Initial value relevant for error messages
		do {
			if (current.getType() != Type.OBJECT) {
				if (allowNull) {
					return null;
				}
				throw new ConfigError("Cannot access '" + name + "': Type of " + (end <= 0 ? "config" : "'" + name.substring(0, end) + "'") +
						" is " + current.getType().toString());
			}
			
			end = name.indexOf('.', start);
			int partEnd = end >= 0 ? end : n;
			String namePart = name.substring(start, partEnd);
			
			current = current.getOrNull(namePart);
			if (current == null) {
				if (allowNull) {
					return null;
				}
				if (partEnd == n) {
					throw new ConfigError("Cannot access '" + name + "': Not found");
				}
				else {
					throw new ConfigError("Cannot access '" + name + "': Element '" + name.substring(0, partEnd) + "' not found");
				}
			}
			
			start = end+1; // After the '.' (Not relevant if end < 0)
		} while (end >= 0);
		
		return current;
	}
	
	private Self newSubConfig(ConfigElement data) {
		Self newConfig = this.factory.get();
		newConfig.init(this.factory, data);
		return newConfig;
	}
	
	private ConfigElement typeCheck(ConfigElement element, Type expected) throws ConfigError {
		if (element == null) {
			return null;
		}
		if (element.getType() != expected) {
			throw new ConfigError(element.typeErrorMessage(expected));
		}
		return element;
	}
	
	
	public Type getType() {
		return this.data.getType();
	}
	
	public Type getType(String name) {
		ConfigElement element = this.navigate(name, true);
		return element != null ? element.getType() : null;
	}
	
	public Type getType(int index) {
		ConfigElement element = this.data.getOrNull(index);
		return element != null ? element.getType() : null;
	}
	
	
	public Self getAny(String name) throws ConfigError {
		return this.newSubConfig(this.navigate(name, false));
	}
	
	public Self getAnyOptional(String name) {
		ConfigElement element = this.navigate(name, true);
		return element != null ? this.newSubConfig(element) : null;
	}
	
	public Self getAny(int index) throws ConfigError {
		return this.newSubConfig(this.data.get(index));
	}
	
	public Self getAnyOptional(int index) {
		ConfigElement element = this.data.getOrNull(index);
		return element != null ? this.newSubConfig(element) : null;
	}
	
	
	public Self getObject(String name) throws ConfigError {
		ConfigElement element = this.typeCheck(this.navigate(name, false), Type.OBJECT);
		return this.newSubConfig(element);
	}
	
	public Self getObjectOptional(String name) {
		ConfigElement element = this.typeCheck(this.navigate(name, true), Type.OBJECT);
		return element != null ? this.newSubConfig(element) : null;
	}
	
	public Self getObject(int index) throws ConfigError {
		ConfigElement element = this.typeCheck(this.data.get(index), Type.OBJECT);
		return this.newSubConfig(element);
	}
	
	public Self getObjectOptional(int index) {
		ConfigElement element = this.typeCheck(this.data.getOrNull(index), Type.OBJECT);
		return element != null ? this.newSubConfig(element) : null;
	}
	
	
	public Self getList(String name) throws ConfigError {
		ConfigElement element = this.typeCheck(this.navigate(name, false), Type.LIST);
		return this.newSubConfig(element);
	}
	
	public Self getListOptional(String name) {
		ConfigElement element = this.typeCheck(this.navigate(name, true), Type.LIST);
		return element != null ? this.newSubConfig(element) : null;
	}
	
	public Self getList(int index) throws ConfigError {
		ConfigElement element = this.typeCheck(this.data.get(index), Type.LIST);
		return this.newSubConfig(element);
	}
	
	public Self getListOptional(int index) {
		ConfigElement element = this.typeCheck(this.data.getOrNull(index), Type.LIST);
		return element != null ? this.newSubConfig(element) : null;
	}
	
	
	public String getString() throws ConfigError {
		return this.data.getString();
	}
	
	public String getStringOrDefault(String defaultValue) {
		return this.data.getStringOrDefault(defaultValue);
	}
	
	public String getString(String name) throws ConfigError {
		return this.navigate(name, false).getString();
	}
	
	public String getStringOrDefault(String name, String defaultValue) {
		ConfigElement element = this.navigate(name, true);
		return element != null ? element.getStringOrDefault(defaultValue) : defaultValue;
	}
	
	public String getString(int index) throws ConfigError {
		return this.data.get(index).getString();
	}
	
	public String getStringOrDefault(int index, String defaultValue) {
		ConfigElement element = this.data.getOrNull(index);
		return element != null ? element.getStringOrDefault(defaultValue) : defaultValue;
	}
}
