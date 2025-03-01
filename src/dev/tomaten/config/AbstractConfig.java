package dev.tomaten.config;

import static de.tomatengames.util.RequirementUtil.requireNotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import dev.tomaten.config.ConfigElement.Type;
import dev.tomaten.json.generic.JSONElement;

public abstract class AbstractConfig<Self extends AbstractConfig<Self>> implements Iterable<Self> {
	private Supplier<Self> factory;
	private ConfigElement data;
	
	
	protected AbstractConfig() {
	}
	
	protected void init(Supplier<Self> factory, ConfigElement data) {
		this.factory = factory;
		this.data = data;
	}
	
	public ConfigElement getData() {
		return this.data;
	}
	
	public String getName() {
		return this.data.getName();
	}
	
	public String getFullName() {
		return this.data.getFullName();
	}
	
	
	protected ConfigElement navigate(String name, boolean allowNull, boolean interpretDots) throws ConfigError {
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
			
			int partEnd;
			String namePart;
			if (interpretDots) {
				end = name.indexOf('.', start);
				partEnd = end >= 0 ? end : n;
				namePart = name.substring(start, partEnd);
			}
			else {
				end = -1;
				partEnd = n;
				namePart = name;
			}
			
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
		ConfigElement element = this.navigate(name, true, true); // no throw error, may be null, interpret dots
		return element != null ? element.getType() : null;
	}
	
	public Type getType(int index) {
		ConfigElement element = this.data.getOrNull(index);
		return element != null ? element.getType() : null;
	}
	
	public String getOriginalType() {
		return this.data.getOriginalType();
	}
	
	
	public Collection<String> getKeys() {
		return this.data.getKeys();
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
		return this.getImpl(name, true, transformer);
	}
	
	public <V> ConfigValue<V> get(String name, ConfigTransformer<? super Self, V> transformer) {
		return this.getImpl(name, true, configTransformerWrapper(transformer));
	}
	
	public ConfigValue<Self> getDirect(String name) {
		return this.getImpl(name, false, element -> this.newSubConfig(element));
	}
	
	public <V> ConfigValue<V> getDirect(String name, ConfigElementTransformer<V> transformer) {
		return this.getImpl(name, false, transformer);
	}
	
	public <V> ConfigValue<V> getDirect(String name, ConfigTransformer<? super Self, V> transformer) {
		return this.getImpl(name, false, configTransformerWrapper(transformer));
	}
	
	private <V> ConfigValue<V> getImpl(String name, boolean interpretDots, ConfigElementTransformer<V> transformer) {
		ConfigElement element = null;
		try {
			element = this.navigate(name, false, interpretDots); // not null, throws error
			V value = transformer != null ? transformer.transform(element) : null;
			return new ConfigValue<>(value, element.getType(), null);
		} catch (ConfigError e) {
			return new ConfigValue<>(null, element != null ? element.getType() : null, e);
		}
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
	
	
	public ConfigValue<Self> getObject() {
		return this.get(element -> this.newSubConfig(this.typeCheck(element, Type.OBJECT)));
	}
	
	public ConfigValue<Self> getObject(String name) {
		return this.get(name, element -> this.newSubConfig(this.typeCheck(element, Type.OBJECT)));
	}
	
	public ConfigValue<Self> getObject(int index) {
		return this.get(index, element -> this.newSubConfig(this.typeCheck(element, Type.OBJECT)));
	}
	
	
	public ConfigValue<Self> getList() {
		return this.get(element -> this.newSubConfig(this.typeCheck(element, Type.LIST)));
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
			TemporalAccessor temp = DateTimeFormatter.ISO_DATE_TIME.parse(str);
			
			// If the parsed date-time has a zone or offset specified, use it to create the ZonedDateTime.
			// If the date-time has no zone or offset, use the system default zone.
			ZoneId zone = temp.query(TemporalQueries.zone());
			if (zone == null) {
				zone = ZoneId.systemDefault();
			}
			
			// Create a ZonedDateTime from the TemporalAccessor using the ZoneId from above.
			// Don't use ZonedDateTime.from(), since it would try to access the zone from the TemporalAccessor, which might not exist.
			// Don't use DateTimeFormatter.ISO_DATE_TIME.withZone(), because it may produce cursed ZonedDateTime objects
			// with the specified zone even if an offset is specified. Additionally, this is buggy on Java 8.
			LocalDate date = LocalDate.from(temp);
			LocalTime time = LocalTime.from(temp);
			return ZonedDateTime.of(date, time, zone);
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
	
	
	
	private <V> ConfigTransformer<Self, List<V>> transformerGetListOf(ConfigElementTransformer<V> transformer) {
		return (element, config) -> {
			this.typeCheck(element, Type.LIST);
			return config.stream().map(c -> transformer.transform(c.getData()))
					.collect(Collectors.toList());
		};
	}
	
	public <V> ConfigValue<List<V>> getListOf(ConfigElementTransformer<V> transformer) {
		return this.get(transformerGetListOf(transformer));
	}
	
	public <V> ConfigValue<List<V>> getListOf(ConfigTransformer<Self, V> transformer) {
		return this.getListOf(configTransformerWrapper(transformer));
	}
	
	public <V> ConfigValue<List<V>> getListOf(String name, ConfigElementTransformer<V> transformer) {
		return this.get(name, transformerGetListOf(transformer));
	}
	
	public <V> ConfigValue<List<V>> getListOf(String name, ConfigTransformer<Self, V> transformer) {
		return this.getListOf(name, configTransformerWrapper(transformer));
	}
	
	public <V> ConfigValue<List<V>> getListOf(int index, ConfigElementTransformer<V> transformer) {
		return this.get(index, transformerGetListOf(transformer));
	}
	
	public <V> ConfigValue<List<V>> getListOf(int index, ConfigTransformer<Self, V> transformer) {
		return this.getListOf(index, configTransformerWrapper(transformer));
	}
	
	
	private <V> ConfigTransformer<Self, List<V>> transformerGetOneOrMany(ConfigElementTransformer<V> transformer) {
		return (element, config) -> {
			if (config.getType() == Type.LIST) {
				return config.stream().map(c -> transformer.transform(c.getData()))
						.collect(Collectors.toList());
			}
			return Collections.singletonList(transformer.transform(element));
		};
	}
	
	public <V> ConfigValue<List<V>> getOneOrMany(ConfigElementTransformer<V> transformer) {
		return this.get(transformerGetOneOrMany(transformer));
	}
	
	public <V> ConfigValue<List<V>> getOneOrMany(ConfigTransformer<Self, V> transformer) {
		return this.getOneOrMany(configTransformerWrapper(transformer));
	}
	
	public <V> ConfigValue<List<V>> getOneOrMany(String name, ConfigElementTransformer<V> transformer) {
		return this.get(name, transformerGetOneOrMany(transformer));
	}
	
	public <V> ConfigValue<List<V>> getOneOrMany(String name, ConfigTransformer<Self, V> transformer) {
		return this.getOneOrMany(name, configTransformerWrapper(transformer));
	}
	
	public <V> ConfigValue<List<V>> getOneOrMany(int index, ConfigElementTransformer<V> transformer) {
		return this.get(index, transformerGetOneOrMany(transformer));
	}
	
	public <V> ConfigValue<List<V>> getOneOrMany(int index, ConfigTransformer<Self, V> transformer) {
		return this.getOneOrMany(index, configTransformerWrapper(transformer));
	}
	
	
	
	public Stream<Self> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}
	
	@Override
	public Iterator<Self> iterator() {
		switch (this.data.getType()) {
		case LIST: return new ListElementIterator();
		case OBJECT: return new ObjectElementIterator();
		default: return Collections.singleton(newSubConfig(this.data)).iterator();
		}
	}
	
	@Override
	public Spliterator<Self> spliterator() {
		switch (this.data.getType()) {
		case LIST: return new ListElementSpliterator(0, this.data.size());
		case OBJECT: return Spliterators.spliterator(new ObjectElementIterator(), this.data.getKeys().size(), Spliterator.NONNULL);
		default: return Collections.singleton(newSubConfig(this.data)).spliterator();
		}
	}
	
	private class ListElementIterator implements Iterator<Self> {
		private final int size;
		private int next;
		
		public ListElementIterator() {
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
		
		// remove() is unsupported.
		// The data should not be modifiable.
	}
	
	private class ListElementSpliterator implements Spliterator<Self> {
		private final int end;
		private int next;
		
		public ListElementSpliterator(int start, int end) {
			this.next = start;
			this.end = end;
		}
		
		@Override
		public boolean tryAdvance(Consumer<? super Self> action) {
			if (this.next >= this.end) {
				return false;
			}
			
			int nextIndex = this.next++;
			ConfigElement nextElement = data.get(nextIndex);
			Self next = newSubConfig(nextElement);
			action.accept(next);
			return true;
		}
		
		@Override
		public Spliterator<Self> trySplit() {
			int start = this.next;
			int dif = this.end - start;
			if (dif <= 1) {
				return null;
			}
			int mid = start + (dif >>> 1);
			this.next = mid;
			return new ListElementSpliterator(start, mid);
		}
		
		@Override
		public long estimateSize() {
			return (long) (this.end - this.next);
		}
		
		@Override
		public int characteristics() {
			return Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
		}
	}
	
	private class ObjectElementIterator implements Iterator<Self> {
		private final Iterator<String> keysIterator;
		
		public ObjectElementIterator() {
			this.keysIterator = data.getKeys().iterator();
		}
		
		@Override
		public boolean hasNext() {
			return this.keysIterator.hasNext();
		}
		
		@Override
		public Self next() {
			String key = this.keysIterator.next();
			ConfigElement nextElement = data.get(key);
			return newSubConfig(nextElement);
		}
		
		// remove() is unsupported.
		// The data should not be modifiable.
	}
	
	
	@Override
	public String toString() {
		return this.data.toString();
	}
	
	public JSONElement toJSON() {
		return this.data.toJSON();
	}
}
