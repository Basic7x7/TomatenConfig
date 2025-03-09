package dev.tomaten.config.test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import dev.tomaten.config.Config;
import dev.tomaten.config.ConfigElement;
import dev.tomaten.config.ConfigError;
import dev.tomaten.config.TomatenConfig;

class TOMLConfigTest {
	private static final String lf = System.lineSeparator();
	
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
	public void testString2() {
		Config config = load("string2");
		assertEquals("Test", config.getString("s1").orError());
		assertEquals("ABCDEF", config.getString("s2").orError());
		assertEquals("\"T\"", config.getString("s3").orError());
		assertEquals("\"X\"", config.getString("s4").orError());
		assertEquals("A" + lf + "B" + lf, config.getString("s5").orError());
		assertEquals("\"A\"", config.getString("s6").orError());
		assertEquals("\"\"B\"\"", config.getString("s7").orError());
		assertEquals("\"\"\"\"", config.getString("s8").orError());
		assertEquals("test\t2", config.getString("s9").orError());
		assertEquals("A''B'", config.getString("s10").orError());
		assertEquals("C" + lf + "D" + lf, config.getString("s11").orError());
		assertEquals("X\\\"Y\\\"Z" + lf, config.getString("s12").orError());
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
		assertThrows(NoSuchElementException.class, () -> it.next());
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
	public void testArray2() {
		Config config = load("array2");
		assertEquals(Arrays.asList("apple", "banana", "cherry"),
				config.getListOf("fruits", ConfigElement::getString).orError());
		assertArrayEquals(new Integer[][] { {1,2}, {3,4} },
				config.getList("matrix").orError().stream()
				.map(c -> c.stream().map(c2 -> c2.getInt().orError()).toArray(Integer[]::new))
				.toArray(Integer[][]::new));
	}
	
	@Test
	public void testBooleansAndNumbers() {
		Config config = load("booleans-and-numbers");
		assertEquals(true, config.getBoolean("is_enabled").orError());
		assertEquals(10, config.getInt("max_connections").orError());
		assertEquals(10L, config.getLong("max_connections").orError());
		assertEquals(10.0, config.getDouble("max_connections").orError());
		assertEquals(1.0, config.getDouble("one").orError());
		assertEquals(3.14159e-2, config.getDouble("precision").orError());
		assertEquals(1.0, config.getDirect("one").orError().getDouble().orDefault(0.0));
	}
	
	@Test
	public void testInlineTable() {
		Config config = load("inline-table");
		Config table = config.getObject("table_inline").orError();
		assertEquals("value1", table.getString("key1").orError());
		assertEquals("value2", table.getString("key2").orError());
	}
	
	@Test
	public void testOrDefault() {
		Config config = load("inline-table");
		Config table = config.getObject("table_inline").orError();
		assertEquals("value1", table.getString("key1").orDefault("test"));
		assertEquals("test", table.getString("key3").orDefault("test"));
		assertEquals(null, table.getString("key4").orDefault(null));
		assertEquals("value2", table.getString("key2").orDefault(null));
		assertEquals(null, table.getString("key5").orNull());
		assertEquals("value1", table.getString("key1").orNull());
		assertEquals(null, table.getInt("key1").orNull());
		assertEquals(12, table.getInt("key1").orDefault(12));
	}
	
	@Test
	public void testArrayOfTables() {
		Config config = load("array-of-tables");
		Iterator<Config> it = config.getList("products").orError().iterator();
		
		assertTrue(it.hasNext());
		Config product = it.next();
		assertEquals("Hammer", product.getString("name").orError());
		assertEquals(34, product.getInt("id").orError());
		assertEquals(null, product.getString("color").orNull());
		assertEquals("A hammer", product.getString("metadata.description").orNull());
		
		assertTrue(it.hasNext());
		product = it.next();
		assertEquals("Nail", product.getString("name").orError());
		assertEquals(42, product.getInt("id").orError());
		assertEquals("gray", product.getString("color").orNull());
		assertEquals("A nail", product.getString("metadata.description").orNull());
		
		assertFalse(false);
	}
	
	@Test
	public void testTables() {
		Config config = load("tables");
		
		assertEquals("Me", config.getString("owner.name").orError());
		assertEquals(LocalDate.of(2025, Month.MARCH, 9), config.getLocalDate("owner.date").orError());
		assertEquals(LocalTime.of(16, 45, 12), config.getLocalTime("owner.time").orError());
		assertEquals(ZonedDateTime.of(2025, 3, 9, 16, 42, 45, 0, ZoneId.of("Z")), config.getDateTime("owner.datetime").orError());
		
		Config database = config.getObject("database").orError();
		assertEquals("192.168.1.1", database.getString("server").orError());
		assertEquals(Arrays.asList(8001, 8002, 8003), database.getListOf("ports", ConfigElement::getInt).orError());
		assertEquals(5000, database.getInt("connection_max").orError());
		assertEquals(true, database.getBoolean("enabled").orError());
		assertEquals("A database", database.getString("metadata.description").orError());
	}
	
	@Test
	public void testNavigationError() {
		Config config = load("tables");
		assertThrows(ConfigError.class, () -> config.getList("owner").orError()); // "owner" is an object
		assertThrows(ConfigError.class, () -> config.getInt("owner.name.length").orError()); // "owner.name" is a string
		assertThrows(ConfigError.class, () -> config.getInt("database.ports.default").orError()); // "database.ports" is a list
		assertEquals(8001, config.getInt("database.ports.0").orError()); // "database.ports" is a list
	}
	
	@Test
	public void testOneOrMany() {
		Config config = load("one-or-many");
		assertEquals(asList(
				asList("A"),
				asList("B"),
				asList("A", "B"),
				asList("C"),
				asList("B", "C", "D")
			),
			config.getList("project").orError().stream()
				.map(c -> c.getOneOrMany("author", ConfigElement::getString).orError())
				.collect(Collectors.toList()));
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
