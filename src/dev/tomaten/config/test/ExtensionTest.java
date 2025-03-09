package dev.tomaten.config.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import dev.tomaten.config.TomatenConfig;

class ExtensionTest {
	@Test
	public void test() {
		ExtendedConfig config = TomatenConfig.load(ExtendedConfig::new, Paths.get("testdata/extended/test.toml"));
		assertEquals(8, config.getMult2("x").orError());
		assertEquals(24, config.getMult2("y").orError());
		assertEquals(-10, config.getMult2("z").orError());
		assertEquals(57, config.getList("arr1").orError().sum().orError());
		assertEquals(0, config.getList("arr2").orError().sum().orError());
		assertEquals(14, config.getList("arr3").orError().sum().orError());
	}
}
