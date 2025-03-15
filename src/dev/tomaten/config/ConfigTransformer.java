package dev.tomaten.config;

/**
 * Transforms a configuration into a value.
 * <p>
 * This interface can be used to read a configuration into an application-specific value.
 * 
 * @param <C> The type of the configuration.
 * @param <V> The type of the resulting value.
 * 
 * @since 1.0
 */
public interface ConfigTransformer<C extends IConfig<?>, V> {
	
	/**
	 * Transforms the given configuration into a value.
	 * @param config The configuration to transform. Not null.
	 * @return The transformed value. May be null, but it is recommended to throw a {@link ConfigError} if the transformation fails.
	 * @throws ConfigError If the configuration cannot be transformed or if an error occurs during the transformation.
	 */
	public V transform(C config) throws ConfigError;
}
