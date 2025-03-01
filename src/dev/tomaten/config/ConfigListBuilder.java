package dev.tomaten.config;

import java.util.ArrayList;

import de.tomatengames.lib.compiler.CompilerException;

class ConfigListBuilder extends ConfigElementBuilder {
	private final ArrayList<ConfigElementBuilder> list;
	
	protected ConfigListBuilder(ConfigElementBuilder parent, String key) {
		super(parent, key);
		this.list = new ArrayList<>();
	}
	
	@Override
	public ConfigElement toElement() {
		ArrayList<ConfigElement> elementList = new ArrayList<>();
		for (ConfigElementBuilder builder : this.list) {
			elementList.add(builder.toElement());
		}
		return new ConfigList(this.getKey(), this.getFullKey(), elementList, this.getOriginalType());
	}
	
	
	public ConfigObjectBuilder getLastObject() throws CompilerException {
		int n = this.list.size();
		if (n <= 0) {
			throw new CompilerException("No object in the list '" + this.getFullKey() + "'");
		}
		ConfigElementBuilder element = this.list.get(n-1);
		if (!(element instanceof ConfigObjectBuilder)) {
			throw new CompilerException("The last element in the list '" + this.getFullKey() + "' is not an object");
		}
		return (ConfigObjectBuilder) element;
	}
	
	
	public ConfigElementBuilder addString(String value) throws CompilerException {
		requireNotClosed();
		ConfigElementBuilder builder = ConfigElementBuilder.ofString(this, String.valueOf(this.list.size()), value);
		this.list.add(builder);
		return builder;
	}
	
	public ConfigElementBuilder addBoolean(boolean value) throws CompilerException {
		requireNotClosed();
		ConfigElementBuilder builder = ConfigElementBuilder.ofBoolean(this, String.valueOf(this.list.size()), value);
		this.list.add(builder);
		return builder;
	}
	
	public ConfigElementBuilder addInt(long value) throws CompilerException {
		requireNotClosed();
		ConfigElementBuilder builder = ConfigElementBuilder.ofInt(this, String.valueOf(this.list.size()), value);
		this.list.add(builder);
		return builder;
	}
	
	public ConfigElementBuilder addDouble(double value) throws CompilerException {
		requireNotClosed();
		ConfigElementBuilder builder = ConfigElementBuilder.ofDouble(this, String.valueOf(this.list.size()), value);
		this.list.add(builder);
		return builder;
	}
	
	public ConfigObjectBuilder addObject() throws CompilerException {
		requireNotClosed();
		ConfigObjectBuilder builder = new ConfigObjectBuilder(this, String.valueOf(this.list.size()));
		this.list.add(builder);
		return builder;
	}
	
	public ConfigListBuilder addList() throws CompilerException {
		requireNotClosed();
		ConfigListBuilder builder = new ConfigListBuilder(this, String.valueOf(this.list.size()));
		this.list.add(builder);
		return builder;
	}
	
	
	protected void requireNotClosed() throws CompilerException {
		if (this.isClosed()) {
			throw new CompilerException("The list '" + this.getFullKey() + "' cannot be modified");
		}
	}
	
}
