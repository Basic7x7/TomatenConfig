package dev.tomaten.config;

public interface ConfigTransformer<C extends IConfig<?>, V> {
	public V transform(ConfigElement element, C config) throws ConfigError;
}
