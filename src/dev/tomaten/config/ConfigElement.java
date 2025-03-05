package dev.tomaten.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dev.tomaten.json.generic.JSONElement;

/**
 * A config element that can be of any type.
 * <p>
 * Every {@link IConfig} is based on a ConfigElement and provides an extended API to interact with it.
 * 
 * @version 2025-03-25 last modified
 * @version 2025-02-15 created
 * @since 1.0
 */
public abstract class ConfigElement {
	private final String name;
	private final String fullName;
	private final String originalType;
	
	protected ConfigElement(String name, String fullName, String originalType) {
		this.name = name;
		this.fullName = fullName;
		this.originalType = originalType;
	}
	
	/**
	 * Returns the name of this config element.
	 * In general, the name is the last segment of the full name.
	 * @return The name. Not null.
	 * @see #getFullName()
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the full name of this config element.
	 * In general, the full name is a dot-separated path to this config element.
	 * @return The full name. Not null.
	 * @see #getName()
	 */
	public String getFullName() {
		return fullName;
	}
	
	/**
	 * Returns the {@link Type} of this config element.
	 * @return The type. Not null.
	 */
	public abstract ConfigElement.Type getType();
	
	
	/**
	 * Returns the <i>original type</i> of this config element.
	 * @return The original type. Null if the original type is not known.
	 * @see IConfig#getOriginalType()
	 */
	public String getOriginalType() {
		return this.originalType;
	}
	
	
	protected String typeErrorMessage(Type expected) {
		String fullName = this.getFullName();
		return "Invalid config value" + (fullName.isEmpty() ? "" : " for '" + this.getFullName() + "'") +
				": Expected " + expected.toString() + ", but found " + this.getType().toString();
	}
	
	/**
	 * Returns the value of this element as a string.
	 * @return The string. Not null.
	 * @throws ConfigError If this element cannot be represented as a string.
	 */
	public String getString() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.STRING));
	}
	
	/**
	 * Returns the value of this element as a string, or the specified default value if it cannot be represented as a string.
	 * @param defaultValue The default value.
	 * @return The string or the default value.
	 */
	public String getStringOrDefault(String defaultValue) {
		return defaultValue;
	}
	
	/**
	 * Returns the value of this element as a double.
	 * @return The double. Not null.
	 * @throws ConfigError If this element cannot be represented as a double.
	 */
	public double getDouble() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.DOUBLE));
	}
	
	/**
	 * Returns the value of this element as a double, or the specified default value if it cannot be represented as a double.
	 * @param defaultValue The default value.
	 * @return The double or the default value.
	 */
	public double getDoubleOrDefault(double defaultValue) {
		return defaultValue;
	}
	
	/**
	 * Returns the value of this element as a long.
	 * @return The long. Not null.
	 * @throws ConfigError If this element cannot be represented as a long.
	 */
	public long getLong() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.INTEGER));
	}
	
	/**
	 * Returns the value of this element as a long, or the specified default value if it cannot be represented as a long.
	 * @param defaultValue The default value.
	 * @return The long or the default value.
	 */
	public long getLongOrDefault(long defaultValue) {
		return defaultValue;
	}
	
	/**
	 * Returns the value of this element as an integer.
	 * @return The integer. Not null.
	 * @throws ConfigError If this element cannot be represented as an integer.
	 */
	public int getInt() throws ConfigError {
		long value = this.getLong();
		if (value < (long) Integer.MIN_VALUE || value > (long) Integer.MAX_VALUE) {
			String fullName = this.getFullName();
			throw new ConfigError("Invalid config value" + (fullName.isEmpty() ? "" : " for '" + this.getFullName() + "'") +
					": The number " + value + " cannot be represented as a 32-bit integer");
		}
		return (int) value;
	}
	
	/**
	 * Returns the value of this element as an integer, or the specified default value if it cannot be represented as an integer.
	 * @param defaultValue The default value.
	 * @return The integer or the default value.
	 */
	public int getIntOrDefault(int defaultValue) {
		long value = this.getLongOrDefault(defaultValue);
		if (value < (long) Integer.MIN_VALUE || value > (long) Integer.MAX_VALUE) {
			return defaultValue;
		}
		return (int) value;
	}
	
	/**
	 * Returns the value of this element as a boolean.
	 * @return The boolean. Not null.
	 * @throws ConfigError If this element cannot be represented as a boolean.
	 */
	public boolean getBoolean() throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.BOOLEAN));
	}
	
	/**
	 * Returns the value of this element as a boolean, or the specified default value if it cannot be represented as a boolean.
	 * @param defaultValue The default value.
	 * @return The boolean or the default value.
	 */
	public boolean getBooleanOrDefault(boolean defaultValue) {
		return defaultValue;
	}
	
	
	/**
	 * Returns the child element of this element with the specified name.
	 * @param name The name of the child element. Not null.
	 * @return The child element. Not null.
	 * @throws ConfigError If no child element with the specified name exists.
	 */
	public ConfigElement get(String name) throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.OBJECT));
	}
	
	/**
	 * Returns the child element of this element with the specified name, or null if no such element exists.
	 * @param name The name of the child element. Not null.
	 * @return The child element or null.
	 */
	public ConfigElement getOrNull(String name) {
		return null;
	}
	
	/**
	 * Returns all child element names of this element.
	 * If this element is not of a type that has child elements, this method returns an empty collection.
	 * @return The child element names. Not null.
	 */
	public Collection<String> getKeys() {
		return Collections.emptyList();
	}
	
	
	/**
	 * Returns the element at the specified index.
	 * @param index The index of the element. Must be non-negative and less than the size of this list.
	 * @return The element at the specified index. Not null.
	 * @throws ConfigError If the element is not a list or if the index is out of bounds.
	 */
	public ConfigElement get(int index) throws ConfigError {
		throw new ConfigError(this.typeErrorMessage(Type.LIST));
	}
	
	/**
	 * Returns the element at the specified index or null if no such element exists.
	 * If this element is not a list or if the index is out of bounds, this method returns null.
	 * @param index The index of the element.
	 * @return The element at the specified index or null.
	 */
	public ConfigElement getOrNull(int index) {
		return null;
	}
	
	/**
	 * Returns the size of this element.
	 * Indicates passed to the {@link #get(int)} method should be less than this value.
	 * @return The size of this element. Not negative.
	 */
	public int size() {
		return 0;
	}
	
	/**
	 * Returns an unmodifiable list of all <i>by-index</i> accessible elements.
	 * If this element is of a type that does not support access by index, this method returns an empty collection.
	 * @return The list of elements. Not null.
	 */
	public List<ConfigElement> getList() {
		return Collections.emptyList();
	}
	
	
	@Override
	public String toString() {
		return this.fullName + "[" + this.getType().name() + (this.originalType != null ? "/" + this.originalType : "") + "]";
	}
	
	/**
	 * Creates a {@link JSONElement} that represents this element.
	 * @return The {@link JSONElement}. Not null.
	 */
	public abstract JSONElement toJSON();
	
	
	/**
	 * The type of a {@link ConfigElement}.
	 */
	public enum Type {
		/**
		 * An object, which is a collection of key-value pairs.
		 * Elements of the type OBJECT should support access <i>by-name</i>.
		 */
		OBJECT,
		
		/**
		 * A list, which is a collection of elements.
		 * Elements of the type LIST should support access <i>by-index</i>.
		 */
		LIST,
		
		/**
		 * A string.
		 */
		STRING,
		
		/**
		 * A 64-bit floating-point number.
		 */
		DOUBLE,
		
		/**
		 * A 64-bit integer.
		 */
		INTEGER,
		
		/**
		 * A boolean value.
		 */
		BOOLEAN
	}
}
