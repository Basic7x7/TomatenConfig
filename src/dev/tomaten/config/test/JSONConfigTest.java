package dev.tomaten.config.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import de.tomatengames.util.linked.Linked;
import dev.tomaten.config.Config;
import dev.tomaten.config.ConfigElement.Type;
import dev.tomaten.config.ConfigError;
import dev.tomaten.config.TomatenConfig;

class JSONConfigTest {
	
	@Test
	public void test1() {
		Config config = load("test1");
		assertEquals("localhost", config.getString("server.host").orError());
		assertEquals(8080, config.getInt("server.port").orDefault(8000));
		assertEquals("info", config.getString("logging.level").orError());
		
		Config server = config.getObject("server").orError();
		assertEquals("localhost", server.getString("host").orError());
		assertEquals("8080", server.getString("port").orDefault("8000"));
		Config logging = config.getObject("logging").orError();
		assertEquals("info", logging.getString("level").orError());
	}
	
	@Test
	public void test2() {
		Config config = load("test2");
		Config database = config.getObject("database").orError();
		assertEquals("test_db", database.getString("name").orError());
		assertEquals(Type.STRING, database.getType("credentials.user"));
		assertEquals(null, database.getType(100));
		assertEquals("admin", database.getString("credentials.user").orError());
		assertEquals("secret", database.getString("credentials.password").orError());
		
		Iterator<Config> it = database.getList("replicas").orEmptyIterable(Config.class).iterator();
		
		assertTrue(it.hasNext());
		Config replica = it.next();
		assertEquals("replica1.local", replica.getString("host").orError());
		assertEquals(5432, replica.getInt("port").orError());
		
		assertTrue(it.hasNext());
		replica = it.next();
		assertEquals("replica2.local", replica.getString("host").orError());
		assertEquals(5433, replica.getInt("port").orError());
		
		assertFalse(it.hasNext());
		
		Linked<String> marker = new Linked<>(null);
		database.getString("name").ifPresent(name -> marker.set(name));
		assertEquals("test_db", marker.get());
		
		database.getString("abc").ifPresent(abc -> fail());
		
		assertEquals(7, database.getString("name").map(name -> name.length()).orError());
	}
	
	@Test
	public void test3() {
		Config config = load("test3");
		
		Config app = config.getAny("app").orError();
		assertEquals("myApp", app.getString("name").orError());
		assertEquals("1.0.0", app.getString("version").orError());
		assertEquals(true, app.getBoolean("features.featureA").orDefault(false));
		assertEquals(false, app.getBoolean("features.featureB").orDefault(false));
		assertEquals(false, app.getBoolean("features.featureC").orDefault(false));
		
		Config settings = config.getObject("settings").orError();
		assertEquals(30, settings.getInt("timeout").orError());
		assertEquals(3, settings.getInt("retryAttempts").orError());
		assertEquals(4.5, settings.getDouble("factor").orError());
		assertThrows(ConfigError.class, () -> settings.getInt("factor").orError());
	}
	
	@Test
	public void test4() {
		Config config = load("test4");
		assertEquals("dark", config.getString("theme").orError());
		assertEquals(true, config.getBoolean("notifications.enabled").orError());
		assertEquals("email", config.getString("notifications.method").orError());
	}
	
	private static Config load(String testName) {
		return TomatenConfig.load(Config::new, Paths.get("testdata/json").resolve(testName + ".json"));
	}
}
