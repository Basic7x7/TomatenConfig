package dev.tomaten.config;

import static de.tomatengames.util.RequirementUtil.requireNotNull;

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

/**
 * An abstract base class for configuration objects.
 * <p>
 * This class implements the methods declared by {@link IConfig}.
 * 
 * @param <Self> The concrete type of this configuration.
 * If you extend this class with a class {@code MyConfig}, this type parameter should be {@code MyConfig}.
 * In this case, {@code MyConfig} should be {@code final}, as it would not be possible to chose this type parameter accordingly.
 * 
 * @version 2025-03-03 last modified
 * @version 2025-02-15 created
 * @since 1.0
 */
public abstract class AbstractConfig<Self extends AbstractConfig<Self>> implements IConfig<Self> {
	private Supplier<Self> factory;
	private ConfigElement data;
	
	
	/**
	 * Creates a new {@link AbstractConfig} object.
	 * The {@link #init(Supplier, ConfigElement)} method must be called before this object is used.
	 */
	protected AbstractConfig() {
	}
	
	/**
	 * Initializes this object with the given data.
	 * @param factory The factory to create a new instance of this class. Not null.
	 * @param data The {@link ConfigElement} that backs this configuration. Not null.
	 */
	protected void init(Supplier<Self> factory, ConfigElement data) {
		this.factory = factory;
		this.data = data;
	}
	
	/**
	 * Returns the {@link ConfigElement} that backs this configuration.
	 * @return The ConfigElement. Not null.
	 */
	protected ConfigElement getData() {
		return this.data;
	}
	
	@Override
	public String getName() {
		return this.data.getName();
	}
	
	@Override
	public String getFullName() {
		return this.data.getFullName();
	}
	
	
	/**
	 * Navigates to a sub-config element.
	 * @param name The name of the sub-config element. Not null.
	 * @param allowNull If it should be allowed to return null. If false and the element is not found, a {@link ConfigError} will be thrown.
	 * @param interpretDots If dots in the name should be interpreted as separators. If false, the entire string is considered as a single element name.
	 * @return The sub-config element.
	 * @throws ConfigError If {@code allowNull} is false and the element is not found.
	 */
	private ConfigElement navigate(String name, boolean allowNull, boolean interpretDots) throws ConfigError {
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
	
	@Override
	public Type getType() {
		return this.data.getType();
	}
	
	@Override
	public Type getType(String name) {
		ConfigElement element = this.navigate(name, true, true); // no throw error, may be null, interpret dots
		return element != null ? element.getType() : null;
	}
	
	@Override
	public Type getType(int index) {
		ConfigElement element = this.data.getOrNull(index);
		return element != null ? element.getType() : null;
	}
	
	@Override
	public String getOriginalType() {
		return this.data.getOriginalType();
	}
	
	@Override
	public Collection<String> getKeys() {
		return this.data.getKeys();
	}
	
	@Override
	public int size() {
		return this.data.size();
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
	
	@Override
	public <V> ConfigValue<V> get(ConfigElementTransformer<V> transformer) {
		try {
			V value = transformer != null ? transformer.transform(this.data) : null;
			return new ConfigValue<>(value, this.data.getType(), null);
		} catch (ConfigError e) {
			return new ConfigValue<>(null, this.data.getType(), e);
		}
	}
	
	@Override
	public <V> ConfigValue<V> get(ConfigTransformer<Self, V> transformer) {
		return this.get(configTransformerWrapper(transformer));
	}
	
	@Override
	public <V> ConfigValue<V> get(String name, ConfigElementTransformer<V> transformer) {
		return this.getImpl(name, true, transformer);
	}
	
	@Override
	public <V> ConfigValue<V> get(String name, ConfigTransformer<? super Self, V> transformer) {
		return this.getImpl(name, true, configTransformerWrapper(transformer));
	}
	
	@Override
	public ConfigValue<Self> getDirect(String name) {
		return this.getImpl(name, false, element -> this.newSubConfig(element));
	}
	
	@Override
	public <V> ConfigValue<V> getDirect(String name, ConfigElementTransformer<V> transformer) {
		return this.getImpl(name, false, transformer);
	}
	
	@Override
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
	
	@Override
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
	
	@Override
	public <V> ConfigValue<V> get(int index, ConfigTransformer<? super Self, V> transformer) {
		return this.get(index, configTransformerWrapper(transformer));
	}
	
	
	
	@Override
	public ConfigValue<Self> getAny(String name) {
		return this.get(name, element -> this.newSubConfig(element));
	}
	
	@Override
	public ConfigValue<Self> getAny(int index) {
		return this.get(index, element -> this.newSubConfig(element));
	}
	
	
	@Override
	public ConfigValue<Self> getObject() {
		return this.get(element -> this.newSubConfig(this.typeCheck(element, Type.OBJECT)));
	}
	
	@Override
	public ConfigValue<Self> getObject(String name) {
		return this.get(name, element -> this.newSubConfig(this.typeCheck(element, Type.OBJECT)));
	}
	
	@Override
	public ConfigValue<Self> getObject(int index) {
		return this.get(index, element -> this.newSubConfig(this.typeCheck(element, Type.OBJECT)));
	}
	
	
	@Override
	public ConfigValue<Self> getList() {
		return this.get(element -> this.newSubConfig(this.typeCheck(element, Type.LIST)));
	}
	
	@Override
	public ConfigValue<Self> getList(String name) {
		return this.get(name, element -> this.newSubConfig(this.typeCheck(element, Type.LIST)));
	}
	
	@Override
	public ConfigValue<Self> getList(int index) {
		return this.get(index, element -> this.newSubConfig(this.typeCheck(element, Type.LIST)));
	}
	
	
	
	
	private <V> ConfigTransformer<Self, List<V>> transformerGetListOf(ConfigElementTransformer<V> transformer) {
		return (element, config) -> {
			this.typeCheck(element, Type.LIST);
			return config.stream().map(c -> transformer.transform(c.getData()))
					.collect(Collectors.toList());
		};
	}
	
	@Override
	public <V> ConfigValue<List<V>> getListOf(ConfigElementTransformer<V> transformer) {
		return this.get(transformerGetListOf(transformer));
	}
	
	@Override
	public <V> ConfigValue<List<V>> getListOf(ConfigTransformer<Self, V> transformer) {
		return this.getListOf(configTransformerWrapper(transformer));
	}
	
	@Override
	public <V> ConfigValue<List<V>> getListOf(String name, ConfigElementTransformer<V> transformer) {
		return this.get(name, transformerGetListOf(transformer));
	}
	
	@Override
	public <V> ConfigValue<List<V>> getListOf(String name, ConfigTransformer<Self, V> transformer) {
		return this.getListOf(name, configTransformerWrapper(transformer));
	}
	
	@Override
	public <V> ConfigValue<List<V>> getListOf(int index, ConfigElementTransformer<V> transformer) {
		return this.get(index, transformerGetListOf(transformer));
	}
	
	@Override
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
	
	@Override
	public <V> ConfigValue<List<V>> getOneOrMany(ConfigElementTransformer<V> transformer) {
		return this.get(transformerGetOneOrMany(transformer));
	}
	
	@Override
	public <V> ConfigValue<List<V>> getOneOrMany(ConfigTransformer<Self, V> transformer) {
		return this.getOneOrMany(configTransformerWrapper(transformer));
	}
	
	@Override
	public <V> ConfigValue<List<V>> getOneOrMany(String name, ConfigElementTransformer<V> transformer) {
		return this.get(name, transformerGetOneOrMany(transformer));
	}
	
	@Override
	public <V> ConfigValue<List<V>> getOneOrMany(String name, ConfigTransformer<Self, V> transformer) {
		return this.getOneOrMany(name, configTransformerWrapper(transformer));
	}
	
	@Override
	public <V> ConfigValue<List<V>> getOneOrMany(int index, ConfigElementTransformer<V> transformer) {
		return this.get(index, transformerGetOneOrMany(transformer));
	}
	
	@Override
	public <V> ConfigValue<List<V>> getOneOrMany(int index, ConfigTransformer<Self, V> transformer) {
		return this.getOneOrMany(index, configTransformerWrapper(transformer));
	}
	
	
	
	@Override
	public Stream<Self> streamObjectEntries() {
		if (this.data.getType() == Type.OBJECT) {
			Spliterator<Self> spliterator = Spliterators.spliterator(new ObjectEntryIterator(), this.data.getKeys().size(), Spliterator.NONNULL);
			return StreamSupport.stream(spliterator, false);
		}
		return Stream.empty();
	}
	
	@Override
	public Stream<Self> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}
	
	@Override
	public Iterator<Self> iterator() {
		if (this.data.getType() == Type.LIST) {
			return new ListElementIterator();
		}
		return Collections.singleton(newSubConfig(this.data)).iterator();
	}
	
	@Override
	public Spliterator<Self> spliterator() {
		if (this.data.getType() == Type.LIST) {
			return new ListElementSpliterator(0, this.data.size());
		}
		return Collections.singleton(newSubConfig(this.data)).spliterator();
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
	
	private class ObjectEntryIterator implements Iterator<Self> {
		private final Iterator<String> keysIterator;
		
		public ObjectEntryIterator() {
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
	
	
	@Override
	public JSONElement toJSON() {
		return this.data.toJSON();
	}
}
