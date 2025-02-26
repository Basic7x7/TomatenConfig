package dev.tomaten.config;

class ConfigBoolean extends ConfigElement {
	private final boolean value;
	
	public ConfigBoolean(String fullName, boolean value, String originalType) {
		super(fullName, originalType);
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
	
	@Override
	public String toString() {
		return super.toString() + "=" + this.value;
	}
}
