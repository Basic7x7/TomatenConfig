package dev.tomaten.config.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

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
	
	@Test
	public void testArray1ByName() {
		Config config = load("array1");
		assertEquals(1, config.getInt("array.0.x").orError());
		assertEquals(5, config.getInt("array.1.x").orError());
		assertEquals(12, config.getInt("array.2.x").orError());
		assertEquals(0, config.getInt("array.3.x").orError());
		assertThrows(ConfigError.class, () -> config.getInt("array.4.x").orError());
	}
	
	@Test
	public void testArray1BySubConfig() {
		Config config = load("array1");
		Config array = config.getList("array").orError();
		assertEquals(1, array.getObject(0).orError().getInt("x").orError());
		assertEquals(5, array.getObject(1).orError().getInt("x").orError());
		assertEquals(12, array.getObject(2).orError().getInt("x").orError());
		assertEquals(0, array.getObject(3).orError().getInt("x").orError());
		assertThrows(ConfigError.class, () -> array.getObject(4).orError().getInt("x").orError());
	}
	
	@Test
	public void testArray1ByIterator() {
		Config config = load("array1");
		Config array = config.getList("array").orError();
		Iterator<Config> it = array.iterator();
		assertTrue(it.hasNext());
		assertEquals(1, it.next().getInt("x").orError());
		assertTrue(it.hasNext());
		assertEquals(5, it.next().getInt("x").orError());
		assertTrue(it.hasNext());
		assertEquals(12, it.next().getInt("x").orError());
		assertTrue(it.hasNext());
		assertEquals(0, it.next().getInt("x").orError());
		assertFalse(it.hasNext());
	}
	
	@Test
	public void testArray1ByTransformer() {
		Config config = load("array1");
		assertEquals(Arrays.asList(1, 5, 12, 0),
				config.getListOf("array", (e, c) -> c.getInt("x").orError()).orError());
	}
	
	@Test
	public void testArray1ByStream() {
		Config config = load("array1");
		assertArrayEquals(new Integer[] {1, 5, 12, 0},
				config.getList("array").orError().stream().map(c -> c.getInt("x").orError()).toArray(Integer[]::new));
	}
	
	
	@Test
	public void testError1() {
		assertThrows(ConfigError.class, () -> load("error1"));
	}
	
	@Test
	public void testErrorFileNotExist() {
		assertThrows(ConfigError.class, () -> load("nonexistent"));
	}
	
	private static Config load(String testName) {
		return TomatenConfig.load(Config::new, Paths.get("testdata/toml").resolve(testName + ".toml"));
	}
}
