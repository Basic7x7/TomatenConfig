package dev.tomaten.config;

import static de.tomatengames.util.RequirementUtil.requireNotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents an optional configuration value.
 * <p>
 * A configuration value has an associated type.
 * If config contains the value, but with a different type, the value of this object will not be present.
 * <p>
 * The concept of ConfigValue objects is similar to {@link Optional} objects,
 * but ConfigValues also store additional information like error handling data.
 * 
 * @param <V> the type of the value.
 */
public class ConfigValue<V> {
	private final V value;
	private final ConfigElement.Type type;
	private final ConfigError error;
	
	ConfigValue(V value, ConfigElement.Type type, ConfigError error) {
		this.value = value;
		this.type = type;
		this.error = error;
	}
	
	/**
	 * Returns the type of this configuration value.
	 * <p>
	 * This method does always return a non-null value if the value exists in the configuration,
	 * even if the type is different from the expected type.
	 * That means, for example, if the ConfigValue was obtained by {@code config.getString("key")},
	 * but the config contains an integer, the type will be {@link ConfigElement.Type#INTEGER} and the value of this object will not be present.
	 * 
	 * @return the type of this configuration value. Null if the configuration value does not exist.
	 */
	public ConfigElement.Type getType() {
		return this.type;
	}
	
	/**
	 * Returns if this value exists in the configuration.
	 * <p>
	 * This method does <b>not</b> necessarily mean that the value of this object is present.
	 * The value is only present if the type matches the expected type.
	 * This method returns true if the configuration contains a value for this key,
	 * even if the type is different from the expected type.
	 * @return If the value exists in the configuration.
	 * @see #isPresent()
	 */
	public boolean exists() {
		return this.type != null;
	}
	
	/**
	 * Returns if this value exists in the configuration and its type matches the expected type.
	 * If both of these conditions are met, the value of this object is <i>present</i>.
	 * <p>
	 * If this method returns {@code true}, the {@code orXYZ()} methods will return this object's value.
	 * @return If the value of this object is present.
	 * @see #exists()
	 */
	public boolean isPresent() {
		return this.value != null;
	}
	
	/**
	 * Returns the value of this configuration value if it is present.
	 * If the value is not present, returns the provided default value.
	 * @param defaultValue The default value to return if the configuration value is not present. May be null.
	 * @return The value of this configuration value if it is present; otherwise the provided default value.
	 * Only null if the default value is null and the configuration value is not present.
	 * @see #isPresent()
	 */
	public V orDefault(V defaultValue) {
		return value != null ? value : defaultValue;
	}
	
	/**
	 * Returns the value of this configuration value if it is present.
	 * If the value is not present, returns {@code null}.
	 * @return The value of this configuration value if it is present; otherwise {@code null}.
	 * @see #isPresent()
	 */
	public V orNull() {
		return value;
	}
	
	/**
	 * Returns the value of this configuration value if it is present.
	 * If the value is not present, a {@link ConfigError} is thrown.
	 * In most cases, the provided error message is meaningful.
	 * <p>
	 * This method allows to easily access mandatory configuration values.
	 * @return The value of this configuration value if it is present. Not null.
	 * @throws ConfigError If the configuration value is not present.
	 * @see #isPresent()
	 */
	public V orError() throws ConfigError {
		if (value == null) {
			throw new ConfigError(error != null ? error.getMessage() : null, error);
		}
		return value;
	}
	
	/**
	 * Returns the value of this configuration value if it is present.
	 * If the value is not present, an exception is thrown using the provided factory method.
	 * @param <T> The type of the exception to be thrown.
	 * @param exceptionFactory A factory method that creates an exception that should be thrown if the value is not present. Not null.
	 * The parameter passed to the factory method is the error that describes why the value is not present.
	 * It is not mandatory to use this error parameter. The error parameter may be null if the reason is unknown.
	 * @return The value of this configuration value if it is present. Not null.
	 * @throws T If the configuration value is not present.
	 * @see #isPresent()
	 */
	public <T extends Throwable> V orThrow(Function<ConfigError, T> exceptionFactory) throws T {
		if (value == null) {
			throw exceptionFactory.apply(this.error);
		}
		return value;
	}
	
	/**
	 * Returns the value of this configuration value if it is present and iterable.
	 * If the value is not present or not iterable, an empty collection is returned.
	 * <p>
	 * <i>Warning</i>: The generic type {@code T} of the returned Iterable is determined by how this method is called and unchecked.
	 * The caller must make sure that if the configuration value is present and iterable, it MUST be an {@code Iterable<T>}
	 * with exactly that type {@code T}.
	 * @param <T> The type of elements in the returned Iterable. The configuration value must be an {@code Iterable<T>} if it is present.
	 * @return The value of this configuration value if it is present and iterable, or an empty collection otherwise. Not null.
	 * @see #isPresent()
	 */
	@SuppressWarnings("unchecked")
	public <T> Iterable<T> orEmptyIterable() {
		if (this.value != null && this.value instanceof Iterable) {
			return (Iterable<T>) this.value;
		}
		return Collections.emptyList();
	}
	
	
	/**
	 * If the configuration value is present and iterable, an {@link Iterable} is returned
	 * that contains all elements from the configuration value that match the given class.
	 * If the configuration value is not present or not iterable, an empty collection is returned.
	 * <p>
	 * This method should be used the same way as {@link #orEmptyIterable()}.
	 * In contrast to {@link #orEmptyIterable()}, this method is type-safe.
	 * This method will <b>not</b> return the configuration value itself,
	 * but an Iterable that contains the elements from the iterable configuration value.
	 * 
	 * @param <T> The type of elements in the returned Iterable.
	 * It is recommended that the configuration value is an {@code Iterable<T>} if it is present.
	 * @param elementCls The class of the elements. Not null.
	 * @return An {@link Iterable} that contains all elements from the configuration value that match the given class,
	 * if the configuration value is present and iterable.
	 * Otherwise, an empty collection. Not null.
	 */
	public <T> Iterable<T> orEmptyIterable(Class<T> elementCls) {
		requireNotNull(elementCls, "The element class ...");
		if (this.value != null && this.value instanceof Iterable) {
			Iterable<?> valueIterable = (Iterable<?>) this.value;
			return () -> new TypeFilterIterator<>(valueIterable.iterator(), elementCls);
		}
		return Collections.emptyList();
	}
	
	private static class TypeFilterIterator<O> implements Iterator<O> {
		private final Iterator<?> baseIterator;
		private final Class<O> outputClass;
		
		private boolean nextReady;
		private boolean hasNext;
		private O next;
		
		public TypeFilterIterator(Iterator<?> baseIterator, Class<O> outputClass) {
			this.baseIterator = baseIterator;
			this.outputClass = outputClass;
			
			this.nextReady = false;
			this.hasNext = false;
			this.next = null;
		}
		
		private void makeNextReady() {
			if (this.nextReady) {
				return;
			}
			while (true) {
				if (!this.baseIterator.hasNext()) {
					this.hasNext = false;
					this.next = null;
					this.nextReady = true;
					return;
				}
				Object next = this.baseIterator.next();
				if (next == null || this.outputClass.isInstance(next)) {
					@SuppressWarnings("unchecked")
					O nextOut = (O) next;
					
					this.hasNext = true;
					this.next = nextOut;
					this.nextReady = true;
					return;
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			this.makeNextReady();
			return this.hasNext;
		}
		
		@Override
		public O next() {
			this.makeNextReady();
			if (!this.hasNext) {
				throw new NoSuchElementException();
			}
			this.nextReady = false;
			return this.next;
		}
	}
	
	
	/**
	 * Returns a new {@link ConfigValue} that contains the result of applying the given mapper function to this configuration value.
	 * If the current configuration value is not present, the returned configuration value will also not be present.
	 * The mapper function is only called if the current configuration value is present.
	 * <p>
	 * The {@link #getType()} method of the returned ConfigValue object will return the same type as this ConfigValue,
	 * even if the mapper function returns a different type.
	 * This behavior does not affect the presence of the value.
	 * @param <T> The type of the result of the mapper function.
	 * @param mapperFunction The function that maps this configuration value to a new value. Not null.
	 * The mapper function can assume that the input value is non-null.
	 * If the mapper function returns null or throws a {@link ConfigError}, the resulting configuration value will not be present.
	 * @return A new ConfigValue that contains the result of applying the mapper function to this configuration value. Not null.
	 * @see #isPresent()
	 */
	public <T> ConfigValue<T> map(Function<? super V, T> mapperFunction) {
		if (value == null) {
			return new ConfigValue<>(null, type, error);
		}
		try {
			T mappedValue = mapperFunction.apply(value);
			return new ConfigValue<>(mappedValue, type, error);
		} catch (ConfigError e) {
			return new ConfigValue<>(null, type, e);
		}
	}
	
	/**
	 * If the configuration value is present, performs the given action with the value.
	 * If the value is not present, nothing happens.
	 * @param action The action to be performed if the value is present. Not null.
	 * @see #isPresent()
	 */
	public void ifPresent(Consumer<? super V> action) {
		if (value != null) {
			action.accept(value);
		}
	}
	
	@Override
	public String toString() {
		return "[" + String.valueOf(this.value) + "]";
	}
}
