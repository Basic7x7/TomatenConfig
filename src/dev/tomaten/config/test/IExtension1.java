package dev.tomaten.config.test;

import dev.tomaten.config.ConfigElementTransformer;
import dev.tomaten.config.ConfigValue;
import dev.tomaten.config.IConfig;

public interface IExtension1<Self extends IExtension1<?>> extends IConfig<Self> {
	
	public static final ConfigElementTransformer<Integer> TRANSFORMER_MULT_2 = e -> e.getInt()*2;
	
	public default ConfigValue<Integer> getMult2() {
		return this.get(TRANSFORMER_MULT_2);
	}
	
	public default ConfigValue<Integer> getMult2(String name) {
		return this.get(name, TRANSFORMER_MULT_2);
	}
	
	public default ConfigValue<Integer> getMult2(int index) {
		return this.get(index, TRANSFORMER_MULT_2);
	}
	
}
