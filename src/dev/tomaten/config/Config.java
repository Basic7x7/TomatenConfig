package dev.tomaten.config;

/**
 * A concrete configuration that provides only the base API declared by {@link IConfig} and has no API extensions.
 * 
 * @version 2025-03-03 last modified
 * @version 2025-02-16 created
 * @since 1.0
 */
public final class Config extends AbstractConfig<Config> {
	
	/**
	 * Creates a new {@link Config} object.
	 * <p>
	 * <b>Warning: Do not create an instance of this class directly.</b>
	 * Use this constructor to create a factory method and let methods like {@link TomatenConfig#load(Supplier, Path)}
	 * create the config objects for you.
	 */
	public Config() {
	}
	
}
