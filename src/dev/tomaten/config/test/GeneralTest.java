package dev.tomaten.config.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import dev.tomaten.config.Config;
import dev.tomaten.config.ConfigElement.Type;
import dev.tomaten.config.ConfigError;
import dev.tomaten.config.ConfigType;
import dev.tomaten.config.TomatenConfig;

class GeneralTest {
	
	@Test
	public void testLoadEmpty() {
		Config config = TomatenConfig.loadEmpty(Config::new);
		assertEquals(Type.OBJECT, config.getType());
		assertEquals(null, config.getString("a").orNull());
		assertEquals(false, config.exists("b"));
		assertEquals(0, config.streamObjectEntries().count());
	}
	
	@Test
	public void testLoadFromString() {
		Config config = TomatenConfig.load(Config::new, "{ a: 5, x: 2.5 }", ConfigType.JSON);
		assertEquals(Type.OBJECT, config.getType());
		assertEquals(5, config.getInt("a").orError());
		assertEquals(2.5, config.getDouble("x").orError());
		assertEquals(null, config.getString("str").orNull());
	}
	
	@Test
	public void testEquals1() {
		Config c1 = TomatenConfig.load(Config::new, "{ a: 5, x: 2.5 }", ConfigType.JSON);
		Config c2 = TomatenConfig.load(Config::new, "{ a: 5, x: 2.5 }", ConfigType.JSON);
		Config c3 = TomatenConfig.load(Config::new, "{ b: 5, x: 2.5 }", ConfigType.JSON);
		Config c4 = TomatenConfig.load(Config::new, "{ a: 5, x: 2.5, y: 1.0 }", ConfigType.JSON);
		Config c5 = TomatenConfig.load(Config::new, "a = 5\nx = 2.5", ConfigType.TOML);
		
		assertTrue(c1.equals(c2));
		assertEquals(c1.hashCode(), c2.hashCode());
		assertFalse(c1.equals(c3));
		assertFalse(c1.equals(c4));
		assertTrue(c1.equals(c5));
		assertEquals(c1.hashCode(), c5.hashCode());
		assertFalse(c3.equals(c4));
		assertFalse(c3.equals(c5));
		assertTrue(c5.equals(c2));
		assertEquals(c5.hashCode(), c2.hashCode());
	}
	
	@Test
	public void testEquals2() {
		Config large = TomatenConfig.load(Config::new, "{ a: { x: 3 }, b: { x: 3 }, c: { y: 3 }, d: { x: 4 }, e: [1,2], f: [2,1], g: [1,2] }", ConfigType.JSON);
		Config a = large.getAny("a").orError();
		Config b = large.getAny("b").orError();
		Config c = large.getAny("c").orError();
		Config d = large.getAny("d").orError();
		Config e = large.getAny("e").orError();
		Config f = large.getAny("f").orError();
		Config g = large.getAny("g").orError();
		
		assertTrue(a.equals(b));
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(c));
		assertFalse(a.equals(d));
		assertTrue(b.equals(a));
		assertEquals(b.hashCode(), a.hashCode());
		assertFalse(c.equals(d));
		
		assertFalse(a.equals(e));
		assertFalse(e.equals(f));
		assertTrue(e.equals(g));
		assertEquals(e.hashCode(), g.hashCode());
	}
	
	
	@Test
	public void testFindConfigJSON() {
		Config config = TomatenConfig.load(Config::new, Paths.get("testdata/general"), "test");
		assertEquals(42, config.getInt("a").orError());
	}
	
	@Test
	public void testFindConfigTOML() {
		Config config = TomatenConfig.load(Config::new, Paths.get("testdata/general"), "test2");
		assertEquals(7, config.getInt("x").orError());
	}
	
	@Test
	public void testFindConfigAmbigous() {
		assertThrows(ConfigError.class, () -> TomatenConfig.load(Config::new, Paths.get("testdata/general"), "ambigous"));
	}
	
}
