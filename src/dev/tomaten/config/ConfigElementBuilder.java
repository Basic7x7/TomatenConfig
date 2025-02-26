package dev.tomaten.config;

abstract class ConfigElementBuilder {
	private final ConfigElementBuilder parent;
	private final String key;
	private boolean closed;
	private String originalType;
	
	protected ConfigElementBuilder(ConfigElementBuilder parent, String key) {
		this.parent = parent;
		this.key = key;
		this.closed = false;
		this.originalType = null;
	}
	
	public ConfigElementBuilder getParent() {
		return parent;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getFullKey() {
		ConfigElementBuilder parent = this.getParent();
		if (parent == null) {
			return this.key;
		}
		String parentFullKey = parent.getFullKey();
		if (parentFullKey == null || parentFullKey.isEmpty()) {
			return this.key;
		}
		return parentFullKey + "." + this.key;
	}
	
	
	public String getOriginalType() {
		return this.originalType;
	}
	public void setOriginalType(String originalType) {
		this.originalType = originalType;
	}
	
	
	public abstract ConfigElement toElement();
	
	
	public void close() {
		this.closed = true;
	}
	
	protected boolean isClosed() {
		return this.closed;
	}
	
	
	public static ConfigElementBuilder ofString(ConfigElementBuilder parent, String key, String value) {
		return new ConfigElementBuilder(parent, key) {
			@Override
			public ConfigElement toElement() {
				return new ConfigString(this.getFullKey(), value, this.getOriginalType());
			}
		};
	}
	
	public static ConfigElementBuilder ofBoolean(ConfigElementBuilder parent, String key, boolean value) {
		return new ConfigElementBuilder(parent, key) {
			@Override
			public ConfigElement toElement() {
				return new ConfigBoolean(this.getFullKey(), value, this.getOriginalType());
			}
		};
	}
	
	public static ConfigElementBuilder ofInt(ConfigElementBuilder parent, String key, long value) {
		return new ConfigElementBuilder(parent, key) {
			@Override
			public ConfigElement toElement() {
				return new ConfigInt(this.getFullKey(), value, this.getOriginalType());
			}
		};
	}
	
	public static ConfigElementBuilder ofDouble(ConfigElementBuilder parent, String key, double value) {
		return new ConfigElementBuilder(parent, key) {
			@Override
			public ConfigElement toElement() {
				return new ConfigDouble(this.getFullKey(), value, this.getOriginalType());
			}
		};
	}
	
}
