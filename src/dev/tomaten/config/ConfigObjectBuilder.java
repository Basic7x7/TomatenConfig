package dev.tomaten.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.tomatengames.lib.compiler.CompilerException;

class ConfigObjectBuilder extends ConfigElementBuilder {
	private final Map<String, ConfigElementBuilder> map;
	
	protected ConfigObjectBuilder(ConfigElementBuilder parent, String key) {
		super(parent, key);
		this.map = new HashMap<>();
	}
	
	@Override
	public ConfigElement toElement() {
		HashMap<String, ConfigElement> elementMap = new HashMap<>();
		for (Entry<String, ConfigElementBuilder> entry : this.map.entrySet()) {
			ConfigElement e = entry.getValue().toElement();
			elementMap.put(entry.getKey(), e);
		}
		return new ConfigObject(this.getFullKey(), elementMap);
	}
	
	private ConfigObjectBuilder navigate(String[] keys, int len) throws CompilerException {
		ConfigObjectBuilder obj = this;
		for (int i = 0; i < len; i++) {
			String key = keys[i];
			ConfigElementBuilder element = obj.map.get(key);
			// Create nonexistent objects.
			if (element == null) {
				ConfigObjectBuilder newObj = new ConfigObjectBuilder(obj, key);
				obj.map.put(key, newObj);
				obj = newObj;
			}
			// Navigate objects.
			else if (element instanceof ConfigObjectBuilder) {
				obj = (ConfigObjectBuilder) element;
			}
			// Navigate lists by accessing their last element that must be an object.
			// This behavior is useful for the TOML parser.
			else if (element instanceof ConfigListBuilder) {
				ConfigListBuilder list = (ConfigListBuilder) element;
				obj = list.getLastObject();
			}
			else {
				throw new CompilerException("Cannot navigate to '" + String.join(".", keys) + ": '" +
						element.getFullKey() + "' is not a navigatable value");
			}
		}
		return obj;
	}
	
	public ConfigObjectBuilder createObject(String... key) throws CompilerException {
		requireNotClosed();
		if (key.length <= 0) {
			throw new CompilerException("No key specified");
		}
		ConfigObjectBuilder obj = this.navigate(key, key.length-1);
		String lastKey = key[key.length-1];
		ConfigObjectBuilder newObj = new ConfigObjectBuilder(obj, lastKey);
		if (obj.map.put(lastKey, newObj) != null) {
			throw new CompilerException("Key does already exist: " + newObj.getFullKey());
		}
		return newObj;
	}
	
	public ConfigListBuilder createOrGetList(String... key) throws CompilerException {
		requireNotClosed();
		if (key.length <= 0) {
			throw new CompilerException("No key specified");
		}
		ConfigObjectBuilder obj = this.navigate(key, key.length-1);
		String lastKey = key[key.length-1];
		ConfigElementBuilder element = obj.map.get(lastKey);
		ConfigListBuilder list;
		if (element == null) {
			list = new ConfigListBuilder(obj, lastKey);
			obj.map.put(lastKey, list);
		}
		else if (element instanceof ConfigListBuilder) {
			list = (ConfigListBuilder) element;
		}
		else {
			throw new CompilerException("'" + element.getFullKey() + "' does already exist, but it is not a list");
		}
		return list;
	}
	
	
	private static interface ElementBuilderFactory {
		public ConfigElementBuilder create(ConfigElementBuilder parent, String key);
	}
	
	private void set(String[] key, ElementBuilderFactory valueFactory) throws CompilerException {
		requireNotClosed();
		if (key.length <= 0) {
			throw new CompilerException("No key specified");
		}
		ConfigObjectBuilder obj = this.navigate(key, key.length-1);
		String lastKey = key[key.length-1];
		ConfigElementBuilder newElement = valueFactory.create(obj, lastKey);
		if (obj.map.put(lastKey, newElement) != null) {
			throw new CompilerException("Key does already exist: " + newElement.getFullKey());
		}
	}
	
	public void setString(String[] key, String value) throws CompilerException {
		requireNotClosed();
		this.set(key, (parent, k) -> ConfigElementBuilder.ofString(parent, k, value));
	}
	
	public void setBoolean(String[] key, boolean value) throws CompilerException {
		requireNotClosed();
		this.set(key, (parent, k) -> ConfigElementBuilder.ofBoolean(parent, k, value));
	}
	
	public void setInt(String[] key, long value) throws CompilerException {
		requireNotClosed();
		this.set(key, (parent, k) -> ConfigElementBuilder.ofInt(parent, k, value));
	}
	
	public void setDouble(String[] key, double value) throws CompilerException {
		requireNotClosed();
		this.set(key, (parent, k) -> ConfigElementBuilder.ofDouble(parent, k, value));
	}
	
	
	protected void requireNotClosed() throws CompilerException {
		if (this.isClosed()) {
			throw new CompilerException("The object '" + this.getFullKey() + "' cannot be modified");
		}
	}
	
}
