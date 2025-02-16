package dev.tomaten.config;

class ConfigBoolean extends ConfigElement {
	private final boolean value;
	
	public ConfigBoolean(String fullName, boolean value) {
		super(fullName);
		this.value = value;
	}
	
	@Override
	public boolean getBoolean() {
		return this.value;
	}
	
	@Override
	public boolean getBooleanOrDefault(boolean defaultValue) {
		return this.value;
	}
	
	@Override
	public Type getType() {
		return Type.BOOLEAN;
	}
}
