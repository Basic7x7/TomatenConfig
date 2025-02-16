package dev.tomaten.config;

import java.util.Collections;
import java.util.List;

class ConfigList extends ConfigElement {
	private final List<ConfigElement> elements;
	private final List<ConfigElement> unmodElements;
	
	public ConfigList(String fullName, List<ConfigElement> elements) {
		super(fullName);
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
	
}
