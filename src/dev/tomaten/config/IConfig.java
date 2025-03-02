package dev.tomaten.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;

import dev.tomaten.config.ConfigElement.Type;

public interface IConfig<Self extends IConfig<?>> extends Iterable<Self> {
	
	/**
	 * Returns the {@link Type} of the element that this config represents.
	 * @return The type of the config element. Not null.
	 */
	public Type getType();
	
	/**
	 * Navigates to the given name and returns the {@link Type} of that element.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separators for nested elements.
	 * @return The type of the config element at the given name. Null if no such element exists.
	 */
	public Type getType(String name);
	
	/**
	 * Returns the {@link Type} of the element at the given index.
	 * @param index The index of the element to get.
	 * @return The type of the config element at the given index. Null if no such element exists or if the index is out of bounds.
	 */
	public Type getType(int index);
	
	/**
	 * Returns the <i>original type</i> of the config element that this config represents.
	 * <p>
	 * The <i>original type</i> is an implementation-specific string that describes the type of the config element in a more detailed way than the {@link Type} enum.
	 * For example, it might include additional information about how the data has been parsed or formatted in the config.
	 * @return The original type of the config element. May be null if the type is not known.
	 */
	public String getOriginalType();
	
	/**
	 * Returns a collection of all keys in this config, not including any nested keys.
	 * If this config represents an element that has no children (i.e. it is an integer or string), then the collection will be empty.
	 * @return A collection of all keys in this config. Not null.
	 */
	public Collection<String> getKeys();
	
	/**
	 * Returns the size of the element represented by this config.
	 * In general, the element will be a list element and the returned size will be the number of elements in the list.
	 * @return The size of the element. Not negative.
	 * @throws ConfigError If the element has no size.
	 */
	public int size() throws ConfigError;
	
	
	
	
	/**
	 * Returns a {@link ConfigValue} representing this config element applied to the specified transformer.
	 * <p>
	 * If the transformer throws a {@link ConfigError}, the ConfigValue will be empty.
	 * @param <V> The type of the value returned by the transformer.
	 * @param transformer The transformer to apply to this config element. Not null.
	 * @return A {@link ConfigValue} representing this config element applied to the specified transformer. Not null.
	 */
	public <V> ConfigValue<V> get(ConfigTransformer<Self, V> transformer);
	
	/**
	 * Returns a {@link ConfigValue} representing this config element applied to the specified transformer.
	 * <p>
	 * If the transformer throws a {@link ConfigError}, the ConfigValue will be empty.
	 * @param <V> The type of the value returned by the transformer.
	 * @param transformer The transformer to apply to this config element. Not null.
	 * @return A {@link ConfigValue} representing this config element applied to the specified transformer. Not null.
	 */
	public <V> ConfigValue<V> get(ConfigElementTransformer<V> transformer);
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} representing that element applied to the specified transformer.
	 * <p>
	 * If the element does not exist or if the transformer throws a {@link ConfigError}, the ConfigValue will be empty.
	 * @param <V> The type of the value returned by the transformer.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @param transformer The transformer to apply to the specified element. Not null.
	 * The transformer is only called if the element exists.
	 * @return A {@link ConfigValue} representing the specified element applied to the specified transformer. Not null.
	 */
	public <V> ConfigValue<V> get(String name, ConfigElementTransformer<V> transformer);
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} representing that element applied to the specified transformer.
	 * <p>
	 * If the element does not exist or if the transformer throws a {@link ConfigError}, the ConfigValue will be empty.
	 * @param <V> The type of the value returned by the transformer.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @param transformer The transformer to apply to the specified element. Not null.
	 * The transformer is only called if the element exists.
	 * @return A {@link ConfigValue} representing the specified element applied to the specified transformer. Not null.
	 */
	public <V> ConfigValue<V> get(String name, ConfigTransformer<? super Self, V> transformer);
	
	/**
	 * Gets the element at the specified index and returns a {@link ConfigValue} representing that element applied to the specified transformer.
	 * <p>
	 * If the element does not exist or the index is out of bounds, the ConfigValue will be empty.
	 * @param <V> The type of the value returned by the transformer.
	 * @param index The index of the element to get.
	 * @param transformer The transformer to apply to the element. Not null.
	 * The transformer is only called if the element exists.
	 * @return The ConfigValue. Not null.
	 */
	public <V> ConfigValue<V> get(int index, ConfigElementTransformer<V> transformer);
	
	/**
	 * Gets the element at the specified index and returns a {@link ConfigValue} representing that element applied to the specified transformer.
	 * <p>
	 * If the element does not exist or the index is out of bounds, the ConfigValue will be empty.
	 * @param <V> The type of the value returned by the transformer.
	 * @param index The index of the element to get.
	 * @param transformer The transformer to apply to the element. Not null.
	 * The transformer is only called if the element exists.
	 * @return The ConfigValue. Not null.
	 */
	public <V> ConfigValue<V> get(int index, ConfigTransformer<? super Self, V> transformer);
	
	
	
	/**
	 * Returns a {@link ConfigValue} containing a config representing the direct child element of this config with the specified key.
	 * <p>
	 * If the element does not exist, the ConfigValue will be empty.
	 * @param key The key of the element. Not null.
	 * @return The ConfigValue. Not null.
	 */
	public ConfigValue<Self> getDirect(String key);
	
	/**
	 * Returns a {@link ConfigValue} containing the value of the direct child element of this config with the specified key applied to the specified transformer.
	 * <p>
	 * If the element does not exist or the transformer throws a {@link ConfigError}, the ConfigValue will be empty.
	 * @param <V> The type of the value returned by the transformer.
	 * @param key The key of the element. Not null.
	 * @param transformer The transformer to apply to the specified element. Not null.
	 * The transformer is only called if the element exists.
	 * @return The ConfigValue. Not null.
	 */
	public <V> ConfigValue<V> getDirect(String key, ConfigElementTransformer<V> transformer);
	
	/**
	 * Returns a {@link ConfigValue} containing the value of the direct child element of this config with the specified key applied to the specified transformer.
	 * <p>
	 * If the element does not exist or the transformer throws a {@link ConfigError}, the ConfigValue will be empty.
	 * @param <V> The type of the value returned by the transformer.
	 * @param key The key of the element. Not null.
	 * @param transformer The transformer to apply to the specified element. Not null.
	 * The transformer is only called if the element exists.
	 * @return The ConfigValue. Not null.
	 */
	public <V> ConfigValue<V> getDirect(String key, ConfigTransformer<? super Self, V> transformer);
	
	
	
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing a config representing that element.
	 * This method does not restrict the type of the resulting element.
	 * <p>
	 * If the element does not exist, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public ConfigValue<Self> getAny(String name);
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing a config representing that element.
	 * This method does not restrict the type of the resulting element.
	 * <p>
	 * If the element does not exist or the index is out of bounds, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public ConfigValue<Self> getAny(int index);
	
	
	
	/**
	 * Returns a {@link ConfigValue} containing a config representing the same element as this config, if the type of the element is {@link Type#OBJECT}.
	 * <p>
	 * If the element is not an object, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public ConfigValue<Self> getObject();
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing a config representing that element,
	 * if the type of the element is {@link Type#OBJECT}.
	 * <p>
	 * If the element does not exist or is not an object, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public ConfigValue<Self> getObject(String name);
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing a config representing that element,
	 * if the type of the element is {@link Type#OBJECT}.
	 * <p>
	 * If the index is out of bounds, the element does not exist or is not an object, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public ConfigValue<Self> getObject(int index);
	
	
	/**
	 * Returns a {@link ConfigValue} containing a config representing the same element as this config, if the type of the element is {@link Type#LIST}.
	 * <p>
	 * If the element is not a list, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public ConfigValue<Self> getList();
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing a config representing that element,
	 * if the type of the element is {@link Type#LIST}.
	 * <p>
	 * If the element does not exist or is not a list, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public ConfigValue<Self> getList(String name);
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing a config representing that element,
	 * if the type of the element is {@link Type#LIST}.
	 * <p>
	 * If the index is out of bounds, the element does not exist or is not a list, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public ConfigValue<Self> getList(int index);
	
	
	
	
	
	/**
	 * Returns a {@link ConfigValue} containing the string value of the element represented by this config.
	 * <p>
	 * If the element cannot be represented as a string, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<String> getString() {
		return this.get(ConfigElement::getString);
	}
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing the string value of that element.
	 * <p>
	 * If the element does not exist or cannot be represented as a string, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<String> getString(String name) {
		return this.get(name, ConfigElement::getString);
	}
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing the string value of that element.
	 * <p>
	 * If the index is out of bounds, the element does not exist or cannot be represented as a string, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public  default ConfigValue<String> getString(int index) {
		return this.get(index, ConfigElement::getString);
	}
	
	
	/**
	 * Returns a {@link ConfigValue} containing the {@code long} value of the element represented by this config.
	 * <p>
	 * If the element cannot be represented as a {@code long}, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Long> getLong() {
		return this.get(ConfigElement::getLong);
	}
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing the {@code long} value of that element.
	 * <p>
	 * If the element does not exist or cannot be represented as a {@code long}, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Long> getLong(String name) {
		return this.get(name, ConfigElement::getLong);
	}
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing the {@code long} value of that element.
	 * <p>
	 * If the index is out of bounds, the element does not exist or cannot be represented as a {@code long}, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Long> getLong(int index) {
		return this.get(index, ConfigElement::getLong);
	}
	
	
	/**
	 * Returns a {@link ConfigValue} containing the {@code int} value of the element represented by this config.
	 * <p>
	 * If the element cannot be represented as a {@code int}, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Integer> getInt() {
		return this.get(ConfigElement::getInt);
	}
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing the {@code int} value of that element.
	 * <p>
	 * If the element does not exist or cannot be represented as a {@code int}, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Integer> getInt(String name) {
		return this.get(name, ConfigElement::getInt);
	}
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing the {@code int} value of that element.
	 * <p>
	 * If the index is out of bounds, the element does not exist or cannot be represented as a {@code int}, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Integer> getInt(int index) {
		return this.get(index, ConfigElement::getInt);
	}
	
	
	/**
	 * Returns a {@link ConfigValue} containing the {@code double} value of the element represented by this config.
	 * <p>
	 * If the element cannot be represented as a {@code double}, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Double> getDouble() {
		return this.get(ConfigElement::getDouble);
	}
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing the {@code double} value of that element.
	 * <p>
	 * If the element does not exist or cannot be represented as a {@code double}, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Double> getDouble(String name) {
		return this.get(name, ConfigElement::getDouble);
	}
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing the {@code double} value of that element.
	 * <p>
	 * If the index is out of bounds, the element does not exist or cannot be represented as a {@code double}, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Double> getDouble(int index) {
		return this.get(index, ConfigElement::getDouble);
	}
	
	
	/**
	 * Returns a {@link ConfigValue} containing the {@code boolean} value of the element represented by this config.
	 * <p>
	 * If the element cannot be represented as a {@code boolean}, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Boolean> getBoolean() {
		return this.get(ConfigElement::getBoolean);
	}
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing the {@code boolean} value of that element.
	 * <p>
	 * If the element does not exist or cannot be represented as a {@code boolean}, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Boolean> getBoolean(String name) {
		return this.get(name, ConfigElement::getBoolean);
	}
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing the {@code boolean} value of that element.
	 * <p>
	 * If the index is out of bounds, the element does not exist or cannot be represented as a {@code boolean}, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<Boolean> getBoolean(int index) {
		return this.get(index, ConfigElement::getBoolean);
	}
	
	
	
	/**
	 * A {@link ConfigElementTransformer} that parses the string representation of an element into a {@link ZonedDateTime}.
	 * <p>
	 * The string representation must match the format specified by {@link DateTimeFormatter#ISO_DATE_TIME}.
	 * If the date-time has no zone or offset specified, the system default zone will be used.
	 */
	public static final ConfigElementTransformer<ZonedDateTime> TRANSFORMER_DATE_TIME = element -> {
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
	
	
	/**
	 * Returns a {@link ConfigValue} containing a {@link ZonedDateTime} representation of the element represented by this config.
	 * The element is converted using {@link #TRANSFORMER_DATE_TIME}.
	 * <p>
	 * If the element cannot be represented as a {@link ZonedDateTime}, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<ZonedDateTime> getDateTime() {
		return this.get(TRANSFORMER_DATE_TIME);
	}
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing a {@link ZonedDateTime} representation of that element.
	 * The element is converted using {@link #TRANSFORMER_DATE_TIME}.
	 * <p>
	 * If the element does not exist or cannot be represented as a {@link ZonedDateTime}, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<ZonedDateTime> getDateTime(String name) {
		return this.get(name, TRANSFORMER_DATE_TIME);
	}
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing a {@link ZonedDateTime} representation of that element.
	 * The element is converted using {@link #TRANSFORMER_DATE_TIME}.
	 * <p>
	 * If the index is out of bounds, the element does not exist or cannot be represented as a {@link ZonedDateTime}, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<ZonedDateTime> getDateTime(int index) {
		return this.get(index, TRANSFORMER_DATE_TIME);
	}
	
	
	
	/**
	 * A {@link ConfigElementTransformer} that parses the string representation of an element into a {@link LocalDate}.
	 * <p>
	 * The string representation must match the format specified by {@link DateTimeFormatter#ISO_LOCAL_DATE}.
	 */
	public static final ConfigElementTransformer<LocalDate> TRANSFORMER_LOCAL_DATE = element -> {
		String str = element.getString();
		try {
			return DateTimeFormatter.ISO_LOCAL_DATE.parse(str, LocalDate::from);
		} catch (DateTimeParseException e) {
			throw new ConfigError(e);
		}
	};
	
	/**
	 * Returns a {@link ConfigValue} containing a {@link LocalDate} representation of the element represented by this config.
	 * The element is converted using {@link #TRANSFORMER_LOCAL_DATE}.
	 * <p>
	 * If the element cannot be represented as a {@link LocalDate}, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<LocalDate> getLocalDate() {
		return this.get(TRANSFORMER_LOCAL_DATE);
	}
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing a {@link LocalDate} representation of that element.
	 * The element is converted using {@link #TRANSFORMER_LOCAL_DATE}.
	 * <p>
	 * If the element does not exist or cannot be represented as a {@link LocalDate}, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<LocalDate> getLocalDate(String name) {
		return this.get(name, TRANSFORMER_LOCAL_DATE);
	}
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing a {@link LocalDate} representation of that element.
	 * The element is converted using {@link #TRANSFORMER_LOCAL_DATE}.
	 * <p>
	 * If the index is out of bounds, the element does not exist or cannot be represented as a {@link LocalDate}, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<LocalDate> getLocalDate(int index) {
		return this.get(index, TRANSFORMER_LOCAL_DATE);
	}
	
	
	
	/**
	 * A {@link ConfigElementTransformer} that parses the string representation of an element into a {@link LocalTime}.
	 * <p>
	 * The string representation must match the format specified by {@link DateTimeFormatter#ISO_LOCAL_TIME}.
	 */
	public static final ConfigElementTransformer<LocalTime> TRANSFORMER_LOCAL_TIME = element -> {
		String str = element.getString();
		try {
			return DateTimeFormatter.ISO_LOCAL_TIME.parse(str, LocalTime::from);
		} catch (DateTimeParseException e) {
			throw new ConfigError(e);
		}
	};
	
	/**
	 * Returns a {@link ConfigValue} containing a {@link LocalTime} representation of the element represented by this config.
	 * The element is converted using {@link #TRANSFORMER_LOCAL_TIME}.
	 * <p>
	 * If the element cannot be represented as a {@link LocalTime}, the ConfigValue will be empty.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<LocalTime> getLocalTime() {
		return this.get(TRANSFORMER_LOCAL_TIME);
	}
	
	/**
	 * Navigates to the element with the specified name and returns a {@link ConfigValue} containing a {@link LocalTime} representation of that element.
	 * The element is converted using {@link #TRANSFORMER_LOCAL_TIME}.
	 * <p>
	 * If the element does not exist or cannot be represented as a {@link LocalTime}, the ConfigValue will be empty.
	 * @param name The name of the element to navigate to. Not null. Dots {@code '.'} are used as separator to navigate through nested elements.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<LocalTime> getLocalTime(String name) {
		return this.get(name, TRANSFORMER_LOCAL_TIME);
	}
	
	/**
	 * Gets the element with the specified index and returns a {@link ConfigValue} containing a {@link LocalTime} representation of that element.
	 * The element is converted using {@link #TRANSFORMER_LOCAL_TIME}.
	 * <p>
	 * If the index is out of bounds, the element does not exist or cannot be represented as a {@link LocalTime}, the ConfigValue will be empty.
	 * @param index The index of the element to get.
	 * @return The ConfigValue. Not null.
	 */
	public default ConfigValue<LocalTime> getLocalTime(int index) {
		return this.get(index, TRANSFORMER_LOCAL_TIME);
	}
	
	
	
	// TODO JavaDoc
	
	public <V> ConfigValue<List<V>> getListOf(ConfigElementTransformer<V> transformer);
	
	public <V> ConfigValue<List<V>> getListOf(ConfigTransformer<Self, V> transformer);
	
	public <V> ConfigValue<List<V>> getListOf(String name, ConfigElementTransformer<V> transformer);
	
	public <V> ConfigValue<List<V>> getListOf(String name, ConfigTransformer<Self, V> transformer);
	
	public <V> ConfigValue<List<V>> getListOf(int index, ConfigElementTransformer<V> transformer);
	
	public <V> ConfigValue<List<V>> getListOf(int index, ConfigTransformer<Self, V> transformer);
	
	
	public <V> ConfigValue<List<V>> getOneOrMany(ConfigElementTransformer<V> transformer);
	
	public <V> ConfigValue<List<V>> getOneOrMany(ConfigTransformer<Self, V> transformer);
	
	public <V> ConfigValue<List<V>> getOneOrMany(String name, ConfigElementTransformer<V> transformer);
	
	public <V> ConfigValue<List<V>> getOneOrMany(String name, ConfigTransformer<Self, V> transformer);
	
	public <V> ConfigValue<List<V>> getOneOrMany(int index, ConfigElementTransformer<V> transformer);
	
	public <V> ConfigValue<List<V>> getOneOrMany(int index, ConfigTransformer<Self, V> transformer);
	
	
	public Stream<Self> streamObjectEntries();
	
	public Stream<Self> stream();
	
	public Iterator<Self> iterator();
	
	public Spliterator<Self> spliterator();
	
}
