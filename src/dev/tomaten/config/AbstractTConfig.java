package dev.tomaten.config;

import java.nio.file.Path;
import java.util.function.Supplier;

public abstract class AbstractTConfig<Self extends AbstractTConfig<Self>> {
	private Supplier<Self> factory;
	private ConfigObject data;
	private Path path;
	
	protected AbstractTConfig() {
	}
	
	private void init(Supplier<Self> factory, ConfigObject data, Path path) {
		this.factory = factory;
		this.data = data;
		this.path = path;
	}
	
	public Self get(String id) {
		return null; // TODO
	}
}
