package dev.tomaten.config.test;

import java.nio.file.Paths;
import java.time.ZoneId;

import dev.tomaten.config.Config;
import dev.tomaten.config.TomatenConfig;

class CustomConfigTest {
	
	public static void main(String[] args) {
		Config config = TomatenConfig.load(Config::new, Paths.get("../../../Test/test.toml"));
		System.out.println(config.toString());
		
		Config a = config.getObject("a").orError();
		System.out.println(a.getDouble("x").orError());
		System.out.println(config.getString("b"));
		
		System.out.println(config.getString("obj.x"));
		System.out.println(config.getDateTime("obj.x").orError());
		System.out.println(config.getDateTime("obj.x").orError().withZoneSameInstant(ZoneId.systemDefault()));
		
		for (Config el : config.getList("arr").orEmptyIterable(Config.class)) {
			System.out.println(el);
		}
	}
	
}
