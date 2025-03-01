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
		return new ConfigObject(this.getKey(), this.getFullKey(), elementMap, this.getOriginalType());
	}
	
	private ConfigObjectBuilder navigate(String[] keys, int len, boolean wantModify) throws CompilerException {
		ConfigObjectBuilder obj = this;
		for (int i = 0; i < len; i++) {
			String key = keys[i];
			
			// Check closed for the current object.
			if (wantModify && obj.isClosed()) {
				throw new CompilerException("Cannot navigate to '" + String.join(".", keys) + ": '" +
						obj.getFullKey() + "' cannot be modified");
			}
			
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
			
			// Check closed for the next object.
			// This is done to also check the last/resulting object.
			if (wantModify && obj.isClosed()) {
				throw new CompilerException("Cannot navigate to '" + String.join(".", keys) + ": '" +
						obj.getFullKey() + "' cannot be modified");
			}
		}
		return obj;
	}
	
	public ConfigObjectBuilder createObject(String... key) throws CompilerException {
		requireNotClosed();
		if (key.length <= 0) {
			throw new CompilerException("No key specified");
		}
		ConfigObjectBuilder obj = this.navigate(key, key.length-1, true);
		String lastKey = key[key.length-1];
		ConfigObjectBuilder newObj = new ConfigObjectBuilder(obj, lastKey);
		if (obj.map.put(lastKey, newObj) != null) {
			throw new CompilerException("'" + newObj.getFullKey() + "' does already exist");
		}
		return newObj;
	}
	
	public ConfigObjectBuilder createOrGetObject(String... key) throws CompilerException {
		requireNotClosed();
		if (key.length <= 0) {
			throw new CompilerException("No key specified");
		}
		ConfigObjectBuilder parentObj = this.navigate(key, key.length-1, true);
		String lastKey = key[key.length-1];
		ConfigElementBuilder element = parentObj.map.get(lastKey);
		ConfigObjectBuilder obj;
		if (element == null) {
			obj = new ConfigObjectBuilder(parentObj, lastKey);
			parentObj.map.put(lastKey, obj);
		}
		else if (element instanceof ConfigObjectBuilder) {
			obj = (ConfigObjectBuilder) element;
		}
		else {
			throw new CompilerException("'" + element.getFullKey() + "' does already exist, but it is not a table");
		}
		return obj;
	}
	
	public ConfigListBuilder createOrGetList(String... key) throws CompilerException {
		requireNotClosed();
		if (key.length <= 0) {
			throw new CompilerException("No key specified");
		}
		ConfigObjectBuilder obj = this.navigate(key, key.length-1, true);
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
	
	public ConfigListBuilder createList(String... key) throws CompilerException {
		requireNotClosed();
		if (key.length <= 0) {
			throw new CompilerException("No key specified");
		}
		ConfigObjectBuilder obj = this.navigate(key, key.length-1, true);
		String lastKey = key[key.length-1];
		ConfigElementBuilder element = obj.map.get(lastKey);
		if (element != null) {
			throw new CompilerException("'" + element.getFullKey() + "' does already exist");
		}
		ConfigListBuilder list = new ConfigListBuilder(obj, lastKey);
		obj.map.put(lastKey, list);
		return list;
	}
	
	
	private static interface ElementBuilderFactory {
		public ConfigElementBuilder create(ConfigElementBuilder parent, String key);
	}
	
	private ConfigElementBuilder set(String[] key, ElementBuilderFactory valueFactory) throws CompilerException {
		requireNotClosed();
		if (key.length <= 0) {
			throw new CompilerException("No key specified");
		}
		ConfigObjectBuilder obj = this.navigate(key, key.length-1, true);
		String lastKey = key[key.length-1];
		ConfigElementBuilder newElement = valueFactory.create(obj, lastKey);
		if (obj.map.put(lastKey, newElement) != null) {
			throw new CompilerException("Key does already exist: " + newElement.getFullKey());
		}
		return newElement;
	}
	
	public ConfigElementBuilder setString(String[] key, String value) throws CompilerException {
		requireNotClosed();
		return this.set(key, (parent, k) -> ConfigElementBuilder.ofString(parent, k, value));
	}
	
	public ConfigElementBuilder setBoolean(String[] key, boolean value) throws CompilerException {
		requireNotClosed();
		return this.set(key, (parent, k) -> ConfigElementBuilder.ofBoolean(parent, k, value));
	}
	
	public ConfigElementBuilder setInt(String[] key, long value) throws CompilerException {
		requireNotClosed();
		return this.set(key, (parent, k) -> ConfigElementBuilder.ofInt(parent, k, value));
	}
	
	public ConfigElementBuilder setDouble(String[] key, double value) throws CompilerException {
		requireNotClosed();
		return this.set(key, (parent, k) -> ConfigElementBuilder.ofDouble(parent, k, value));
	}
	
	
	public int entriesCount() {
		return this.map.size();
	}
	
	
	protected void requireNotClosed() throws CompilerException {
		if (this.isClosed()) {
			throw new CompilerException("The object '" + this.getFullKey() + "' cannot be modified");
		}
	}
	
}
