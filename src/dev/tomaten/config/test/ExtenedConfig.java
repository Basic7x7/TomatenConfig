package dev.tomaten.config.test;

import dev.tomaten.config.AbstractConfig;

public class ExtenedConfig extends AbstractConfig<ExtenedConfig>
	implements IExtension1<ExtenedConfig>, IExtension2<ExtenedConfig> {
	
	public ExtenedConfig() {
	}
	
}
