package dev.tomaten.config;

/**
 * Transforms a configuration into a value.
 * <p>
 * This interface can be used to read a configuration into an application-specific value.
 * <p>
 * Note: This interface provides direct access to the {@link ConfigElement} that is represented by the configuration.
 * This is done to distinguish it from {@link ConfigElementTransformer} in lambda expressions.
 * 
 * @param <C> The type of the configuration.
 * @param <V> The type of the resulting value.
 * 
 * @since 1.0
 */
public interface ConfigTransformer<C extends IConfig<?>, V> {
	
	/**
	 * Transforms the given configuration into a value.
	 * @param element The element that the configuration represents. Not null.
	 * @param config The configuration to transform. Not null.
	 * @return The transformed value. May be null, but it is recommended to throw a {@link ConfigError} if the transformation fails.
	 * @throws ConfigError If the configuration cannot be transformed or if an error occurs during the transformation.
	 */
	public V transform(ConfigElement element, C config) throws ConfigError;
}
