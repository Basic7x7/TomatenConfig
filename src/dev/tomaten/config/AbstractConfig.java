package dev.tomaten.config;

import static de.tomatengames.util.RequirementUtil.requireNotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import dev.tomaten.config.ConfigElement.Type;

public abstract class AbstractConfig<Self extends AbstractConfig<Self>> implements Iterable<Self> {
	private Supplier<Self> factory;
	private ConfigElement data;
	
	
	protected AbstractConfig() {
	}
	
	protected void init(Supplier<Self> factory, ConfigElement data) {
		this.factory = factory;
		this.data = data;
	}
	
	protected ConfigElement getData() {
		return this.data;
	}
	
	
	protected ConfigElement navigate(String name, boolean allowNull) throws ConfigError {
		requireNotNull(name, "The name ...");
		ConfigElement current = this.data;
		int n = name.length();
		int start = 0;
		int end = 0; // Initial value relevant for error messages
		do {
			if (current.getType() != Type.OBJECT) {
				if (allowNull) {
					return null;
				}
				throw new ConfigError("Cannot access '" + name + "': Type of " + (end <= 0 ? "config" : "'" + name.substring(0, end) + "'") +
						" is " + current.getType().toString());
			}
			
			end = name.indexOf('.', start);
			int partEnd = end >= 0 ? end : n;
			String namePart = name.substring(start, partEnd);
			
			current = current.getOrNull(namePart);
			if (current == null) {
				if (allowNull) {
					return null;
				}
				if (partEnd == n) {
					throw new ConfigError("Cannot access '" + name + "': Not found");
				}
				else {
					throw new ConfigError("Cannot access '" + name + "': Element '" + name.substring(0, partEnd) + "' not found");
				}
			}
			
			start = end+1; // After the '.' (Not relevant if end < 0)
		} while (end >= 0);
		
		return current;
	}
	
	private Self newSubConfig(ConfigElement data) {
		Self newConfig = this.factory.get();
		newConfig.init(this.factory, data);
		return newConfig;
	}
	
	private ConfigElement typeCheck(ConfigElement element, Type expected) throws ConfigError {
		if (element == null) {
			return null;
		}
		if (element.getType() != expected) {
			throw new ConfigError(element.typeErrorMessage(expected));
		}
		return element;
	}
	
	
	public Type getType() {
		return this.data.getType();
	}
	
	public Type getType(String name) {
		ConfigElement element = this.navigate(name, true);
		return element != null ? element.getType() : null;
	}
	
	public Type getType(int index) {
		ConfigElement element = this.data.getOrNull(index);
		return element != null ? element.getType() : null;
	}
	
	
	private <V> ConfigElementTransformer<V> configTransformerWrapper(ConfigTransformer<? super Self, V> transformer) {
		if (transformer == null) {
			return null;
		}
		return element -> {
			Self conf = this.newSubConfig(element);
			return transformer.transform(element, conf);
		};
	}
	
	public <V> ConfigValue<V> get(ConfigElementTransformer<V> transformer) {
		try {
			V value = transformer != null ? transformer.transform(this.data) : null;
			return new ConfigValue<>(value, this.data.getType(), null);
		} catch (ConfigError e) {
			return new ConfigValue<>(null, this.data.getType(), e);
		}
	}
	
	public <V> ConfigValue<V> get(ConfigTransformer<? super Self, V> transformer) {
		return this.get(configTransformerWrapper(transformer));
	}
	
	public <V> ConfigValue<V> get(String name, ConfigElementTransformer<V> transformer) {
		ConfigElement element = null;
		try {
			element = this.navigate(name, false);
			V value = transformer != null ? transformer.transform(element) : null;
			return new ConfigValue<>(value, element.getType(), null);
		} catch (ConfigError e) {
			return new ConfigValue<>(null, element != null ? element.getType() : null, e);
		}
	}
	
	public <V> ConfigValue<V> get(String name, ConfigTransformer<? super Self, V> transformer) {
		return this.get(name, configTransformerWrapper(transformer));
	}
	
	public <V> ConfigValue<V> get(int index, ConfigElementTransformer<V> transformer) {
		ConfigElement element = null;
		try {
			element = this.data.get(index);
			V value = transformer != null ? transformer.transform(element) : null;
			return new ConfigValue<>(value, element.getType(), null);
		} catch (ConfigError e) {
			return new ConfigValue<>(null, element != null ? element.getType() : null, e);
		}
	}
	
	public <V> ConfigValue<V> get(int index, ConfigTransformer<? super Self, V> transformer) {
		return this.get(index, configTransformerWrapper(transformer));
	}
	
	
	
	public ConfigValue<Self> getAny(String name) {
		return this.get(name, element -> this.newSubConfig(element));
	}
	
	public ConfigValue<Self> getAny(int index) {
		return this.get(index, element -> this.newSubConfig(element));
	}
	
	
	public ConfigValue<Self> getObject(String name) {
		return this.get(name, element -> this.newSubConfig(this.typeCheck(element, Type.OBJECT)));
	}
	
	public ConfigValue<Self> getObject(int index) {
		return this.get(index, element -> this.newSubConfig(this.typeCheck(element, Type.OBJECT)));
	}
	
	
	public ConfigValue<Self> getList(String name) {
		return this.get(name, element -> this.newSubConfig(this.typeCheck(element, Type.LIST)));
	}
	
	public ConfigValue<Self> getList(int index) {
		return this.get(index, element -> this.newSubConfig(this.typeCheck(element, Type.LIST)));
	}
	
	
	
	public ConfigValue<String> getString() {
		return this.get(ConfigElement::getString);
	}
	
	public ConfigValue<String> getString(String name) {
		return this.get(name, ConfigElement::getString);
	}
	
	public ConfigValue<String> getString(int index) {
		return this.get(index, ConfigElement::getString);
	}
	
	
	public ConfigValue<Long> getLong() {
		return this.get(ConfigElement::getLong);
	}
	
	public ConfigValue<Long> getLong(String name) {
		return this.get(name, ConfigElement::getLong);
	}
	
	public ConfigValue<Long> getLong(int index) {
		return this.get(index, ConfigElement::getLong);
	}
	
	
	public ConfigValue<Integer> getInt() {
		return this.get(ConfigElement::getInt);
	}
	
	public ConfigValue<Integer> getInt(String name) {
		return this.get(name, ConfigElement::getInt);
	}
	
	public ConfigValue<Integer> getInt(int index) {
		return this.get(index, ConfigElement::getInt);
	}
	
	
	public ConfigValue<Double> getDouble() {
		return this.get(ConfigElement::getDouble);
	}
	
	public ConfigValue<Double> getDouble(String name) {
		return this.get(name, ConfigElement::getDouble);
	}
	
	public ConfigValue<Double> getDouble(int index) {
		return this.get(index, ConfigElement::getDouble);
	}
	
	
	public ConfigValue<Boolean> getBoolean() {
		return this.get(ConfigElement::getBoolean);
	}
	
	public ConfigValue<Boolean> getBoolean(String name) {
		return this.get(name, ConfigElement::getBoolean);
	}
	
	public ConfigValue<Boolean> getBoolean(int index) {
		return this.get(index, ConfigElement::getBoolean);
	}
	
	
	
	private static final ConfigElementTransformer<ZonedDateTime> DATE_TIME_TRANSFORMER = element -> {
		String str = element.getString();
		try {
			return DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault()).parse(str, ZonedDateTime::from);
		} catch (DateTimeParseException e) {
			throw new ConfigError(e);
		}
	};
	
	public ConfigValue<ZonedDateTime> getDateTime() {
		return this.get(DATE_TIME_TRANSFORMER);
	}
	
	public ConfigValue<ZonedDateTime> getDateTime(String name) {
		return this.get(name, DATE_TIME_TRANSFORMER);
	}
	
	public ConfigValue<ZonedDateTime> getDateTime(int index) {
		return this.get(index, DATE_TIME_TRANSFORMER);
	}
	
	
	private static final ConfigElementTransformer<LocalDate> LOCAL_DATE_TRANSFORMER = element -> {
		String str = element.getString();
		try {
			return DateTimeFormatter.ISO_LOCAL_DATE.parse(str, LocalDate::from);
		} catch (DateTimeParseException e) {
			throw new ConfigError(e);
		}
	};
	
	public ConfigValue<LocalDate> getLocalDate() {
		return this.get(LOCAL_DATE_TRANSFORMER);
	}
	
	public ConfigValue<LocalDate> getLocalDate(String name) {
		return this.get(name, LOCAL_DATE_TRANSFORMER);
	}
	
	public ConfigValue<LocalDate> getLocalDate(int index) {
		return this.get(index, LOCAL_DATE_TRANSFORMER);
	}
	
	
	private static final ConfigElementTransformer<LocalTime> LOCAL_TIME_TRANSFORMER = element -> {
		String str = element.getString();
		try {
			return DateTimeFormatter.ISO_LOCAL_TIME.parse(str, LocalTime::from);
		} catch (DateTimeParseException e) {
			throw new ConfigError(e);
		}
	};
	
	public ConfigValue<LocalTime> getLocalTime() {
		return this.get(LOCAL_TIME_TRANSFORMER);
	}
	
	public ConfigValue<LocalTime> getLocalTime(String name) {
		return this.get(name, LOCAL_TIME_TRANSFORMER);
	}
	
	public ConfigValue<LocalTime> getLocalTime(int index) {
		return this.get(index, LOCAL_TIME_TRANSFORMER);
	}
	
	
	
	@Override
	public Iterator<Self> iterator() throws ConfigError {
		// Throws a ConfigError if this config data is not iterable / not a list.
		return new Iter();
	}
	
	private class Iter implements Iterator<Self> {
		private final int size;
		private int next;
		
		public Iter() {
			this.size = data.size();
			this.next = 0;
		}
		
		@Override
		public boolean hasNext() {
			return this.next < this.size;
		}
		
		@Override
		public Self next() {
			if (!this.hasNext()) {
				throw new NoSuchElementException();
			}
			ConfigElement element = data.get(this.next++);
			return newSubConfig(element);
		}
	}
	
}
