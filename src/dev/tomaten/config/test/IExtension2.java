package dev.tomaten.config.test;

import dev.tomaten.config.ConfigValue;
import dev.tomaten.config.IConfig;

public interface IExtension2<Self extends IExtension2<?>> extends IConfig<Self> {
	
	public default ConfigValue<Integer> sum() {
		return this.get(c -> c.stream().mapToInt(el -> el.getInt().orError()).sum());
	}
	
}
