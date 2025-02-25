package dev.tomaten.config.test;

import java.nio.file.Paths;

import dev.tomaten.config.Config;
import dev.tomaten.config.TomatenConfig;

class CustomConfigTest {
	
	public static void main(String[] args) {
		Config config = TomatenConfig.load(Config::new, Paths.get("../../../Test/test.toml"));
		Config a = config.getObject("a").orError();
		System.out.println(a.getDouble("x").orError());
		System.out.println(config.getString("b"));
		
		for (Config el : config.getList("arr").orEmptyIterable(Config.class)) {
			System.out.println(el.getInt().orError());
		}
	}
	
}
