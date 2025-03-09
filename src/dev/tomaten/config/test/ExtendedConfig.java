package dev.tomaten.config.test;

import dev.tomaten.config.AbstractConfig;

public class ExtendedConfig extends AbstractConfig<ExtendedConfig>
	implements IExtension1<ExtendedConfig>, IExtension2<ExtendedConfig> {
	
	public ExtendedConfig() {
	}
	
}
