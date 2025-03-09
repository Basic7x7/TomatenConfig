package dev.tomaten.config.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import dev.tomaten.config.Config;
import dev.tomaten.config.ConfigElement;
import dev.tomaten.config.ConfigError;
import dev.tomaten.config.TomatenConfig;

class TOMLConfigTest {
	
	@Test
	public void testEmpty() {
		Config config = load("empty");
		
		assertEquals(null, config.getString("str").orDefault(null));
		assertThrows(ConfigError.class, () -> config.getString("test").orError());
		
		assertEquals(ConfigElement.Type.OBJECT, config.getType());
		assertArrayEquals(new Config[0], config.streamObjectEntries().toArray(Config[]::new));
	}
	
	@Test
	public void testString1() {
		Config config = load("string1");
		assertEquals("Test", config.getString("str").orError());
		assertEquals("ABC", config.getString("test").orError());
		assertEquals(null, config.getString("abc").orDefault(null));
	}
	
	private static Config load(String testName) {
		return TomatenConfig.load(Config::new, Paths.get("testdata/toml").resolve(testName + ".toml"));
	}
}
