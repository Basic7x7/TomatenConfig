package dev.tomaten.config;

public class ConfigDouble extends ConfigElement {
	private final double value;
	
	public ConfigDouble(String fullName, double value) {
		super(fullName);
		this.value = value;
	}
	
	@Override
	public Type getType() {
		return Type.DOUBLE;
	}
	
	@Override
	public double getDouble() {
		return this.value;
	}
	
	@Override
	public double getDoubleOrDefault(double defaultValue) {
		return this.value;
	}
}
