package dev.tomaten.config;

class ConfigInt extends ConfigElement {
	private final long value;
	
	public ConfigInt(String fullName, long value) {
		super(fullName);
		this.value = value;
	}
	
	@Override
	public Type getType() {
		return Type.INTEGER;
	}
	
	@Override
	public long getLong() {
		return this.value;
	}
	
	@Override
	public long getLongOrDefault(long defaultValue) {
		return this.value;
	}
	
	@Override
	public double getDouble() {
		// Double values can also be specified as integers.
		return (double) this.value;
	}
	
	@Override
	public double getDoubleOrDefault(double defaultValue) {
		return (double) this.value;
	}
}
