package dev.tomaten.config.test;

import java.nio.file.Paths;

import dev.tomaten.config.Config;
import dev.tomaten.config.TomatenConfig;

class CustomConfigTest {
	
	public static void main(String[] args) {
		Config config = TomatenConfig.load(Config::new, Paths.get("../../../Test/test.json"));
		System.out.println(config.getObject("a").getStringOrDefault("x", null));
		System.out.println(config.getString("b"));
	}
	
}
