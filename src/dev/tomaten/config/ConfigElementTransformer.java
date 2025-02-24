package dev.tomaten.config;

public interface ConfigElementTransformer<V> {
	public V transform(ConfigElement element) throws ConfigError;
}
