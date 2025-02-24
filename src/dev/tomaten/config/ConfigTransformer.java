package dev.tomaten.config;

public interface ConfigTransformer<C extends AbstractConfig<C>, V> {
	public V transform(ConfigElement element, C config) throws ConfigError;
}
