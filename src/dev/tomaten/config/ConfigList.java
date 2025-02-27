package dev.tomaten.config;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import dev.tomaten.json.generic.JSONArray;
import dev.tomaten.json.generic.JSONElement;

class ConfigList extends ConfigElement {
	private final List<ConfigElement> elements;
	private final List<ConfigElement> unmodElements;
	
	public ConfigList(String fullName, List<ConfigElement> elements, String originalType) {
		super(fullName, originalType);
		this.elements = elements;
		this.unmodElements = Collections.unmodifiableList(elements);
	}
	
	@Override
	public Type getType() {
		return Type.LIST;
	}
	
	@Override
	public ConfigElement get(int index) {
		try {
			return this.elements.get(index);
		} catch (IndexOutOfBoundsException e) {
			String fullName = this.getFullName();
			throw new ConfigError("List index out of bounds" + (fullName.isEmpty() ? "" : " for '" + fullName + "'") +
					": index=" + index + ", size=" + this.elements.size(), e);
		}
	}
	
	@Override
	public ConfigElement getOrNull(int index) {
		if (index < 0 || index >= this.elements.size()) {
			return null;
		}
		return this.elements.get(index);
	}
	
	@Override
	public int size() {
		return this.elements.size();
	}
	
	@Override
	public List<ConfigElement> getList() {
		return this.unmodElements;
	}
	
	
	@Override
	public String toString() {
		return super.toString() + "=[ " + this.elements.stream().map(e -> e.toString()).collect(Collectors.joining(", ")) + " ]";
	}
	
	@Override
	public JSONElement toJSON() {
		JSONArray array = new JSONArray();
		for (ConfigElement configElement : this.elements) {
			JSONElement jsonElement = configElement.toJSON();
			array.add(jsonElement);
		}
		return array;
	}
	
}
