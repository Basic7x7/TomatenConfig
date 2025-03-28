package dev.tomaten.config;

/**
 * Transforms a {@link ConfigElement} into a value.
 * <p>
 * This interface can be used to read a config element into an application-specific value.
 * <p>
 * Note: This interface provides direct access to the {@link ConfigElement.Type} of the config element.
 * This is done to distinguish it from {@link ConfigTransformer} in lambda expressions.
 * 
 * @param <V> The type of the resulting value.
 * 
 * @since 1.0
 */
public interface ConfigElementTransformer<V> {
	
	/**
	 * Transforms the given {@link ConfigElement} into a value.
	 * @param element The config element to transform. Not null.
	 * @param type The type of the config element. Not null.
	 * @return The transformed value. May be null, but it is recommended to throw a {@link ConfigError} if the transformation fails.
	 * @throws ConfigError If the element cannot be transformed or if an error occurs during the transformation.
	 */
	public V transform(ConfigElement element, ConfigElement.Type type) throws ConfigError;
}
