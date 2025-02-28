package dev.tomaten.config;

import static de.tomatengames.lib.compiler.prefixlexer.PrefixLexerContextWithBuffer.bufferEvent;
import static de.tomatengames.lib.compiler.prefixlexer.PrefixLexerContextWithBuffer.clearBufferEvent;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;

import de.tomatengames.lib.compiler.CompilerException;
import de.tomatengames.lib.compiler.LexicalSymbolSet;
import de.tomatengames.lib.compiler.prefixlexer.PrefixLexerContext;
import de.tomatengames.lib.compiler.prefixlexer.PrefixLexerContextWithBuffer;
import de.tomatengames.lib.compiler.prefixlexer.PrefixLexerGrammar;
import de.tomatengames.lib.compiler.prefixlexer.PrefixLexerOption;
import de.tomatengames.util.HexUtil;

/**
 * A parser for TOML-like configuration files.
 * This implementation contains some extensions to the TOML specification, like multi-line inline tables.
 * All input that is valid TOML according to the TOML specification should be handled correctly.
 */
class TOMLConfigParser {
	private static final Object MARKER_TABLE_DEFINED = new Object();
	
	private static final LexicalSymbolSet<Context> symbolSet = LexicalSymbolSet.createDefault();
	static {
		symbolSet.add("key", (c, context) -> ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') ||
				('0' <= c && c <= '9') || c == '_' || c == '-' ||
				// In TOML 1.1, also some Unicode ranges are allowed for bare keys
				c == '\u00B2' || c == '\u00B3' || c == '\u00B9' || ('\u00BC' <= c && c <= '\u00BE') ||
				('\u00C0' <= c && c <= '\u00D6') || ('\u00D8' <= c && c <= '\u00F6') || ('\u00F8' <= c && c <= '\u037D') ||
				('\u037F' <= c && c <= '\u1FFF') ||
				('\u200C' <= c && c <= '\u200D') || ('\u203F' <= c && c <= '\u2040') ||
				('\u2070' <= c && c <= '\u218F') || ('\u2460' <= c && c <= '\u24FF') ||
				('\u2C00' <= c && c <= '\u2FEF') || ('\u3001' <= c && c <= '\uD7FF') ||
				('\uF900' <= c && c <= '\uFDCF') || ('\uFDF0' <= c && c <= '\uFFFD') ||
				// Ignore the range 10000-EFFFF, because a single char cannot represent these.
				// To be able to use these ranges, all surrogate characters are allowed by this implementation.
				Character.isSurrogate(c)
				);
		symbolSet.add("oct", (c, context) -> '0' <= c && c <= '7');
		symbolSet.add("sign", (c, context) -> c == '+' || c == '-');
	}
	
	private static final PrefixLexerGrammar<Context> grammar = new PrefixLexerGrammar<>(symbolSet, "LINESTART", PrefixLexerOption.IGNORE_UTF8_BOM);
	static {
		grammar.enableLookAhead("COMMENT");
		grammar.add("COMMENT -> '#' INNER_COMMENT");
		grammar.add("COMMENT -> :NEWLINE:");
		grammar.add("INNER_COMMENT -> any THIS");
		grammar.add("INNER_COMMENT -> :NEWLINE:");
		grammar.add("INNER_COMMENT ->"); // end of input
		
		
		grammar.add("LINESTART -> space THIS");
		grammar.add("LINESTART -> COMMENT LINESTART");
		grammar.add("LINESTART -> '[[' KEY ']]' LINEEND").withPostEvent((t, context) -> {
			context.key = context.keyBuffer();
			ConfigListBuilder list = context.rootTable.createOrGetList(context.key);
			list.setOriginalType("array-of-tables");
			ConfigObjectBuilder obj = list.addObject();
			obj.setOriginalType("table");
			context.table = obj;
		});
		grammar.add("LINESTART -> '[' KEY ']' LINEEND").withPostEvent((t, context) -> {
			context.key = context.keyBuffer();
			ConfigObjectBuilder obj = context.rootTable.createOrGetObject(context.key);
			if (obj.isClosed()) {
				throw new CompilerException("Table '" + obj.getFullKey() + "' cannot be modified");
			}
			if (obj.isMarkerSet(MARKER_TABLE_DEFINED)) {
				throw new CompilerException("Table '" + obj.getFullKey() + "' specified multiple times");
			}
			obj.setMarker(MARKER_TABLE_DEFINED);
			obj.setOriginalType("table");
			context.table = obj;
		});
		grammar.add("LINESTART -> KEY '=' VALUE LINEEND").withIntermediateEvent(1, (t, context) -> {
			context.key = context.keyBuffer();
		});
		grammar.add("LINESTART ->"); // Final state
		
		
		grammar.add("LINEEND -> space THIS");
		grammar.add("LINEEND -> COMMENT LINESTART");
		grammar.add("LINEEND ->"); // Final state
		
		
		
		// --- Keys ---
		
		grammar.enableLookAhead("KEY");
		grammar.add("KEY -> KEY_ KEY_LIST").withEvent((t, context) -> {
			context.keysBuf.clear();
			context.keysBuf.add(new StringBuilder());
			context.keyMustEnd = false;
		});
		
		grammar.add("KEY_LIST -> KEY_ THIS");
		grammar.add("KEY_LIST ->");
		
		grammar.enableLookAhead("KEY_");
		grammar.add("KEY_ -> space").withEvent((t, context) -> {
			if (context.keysBuf.peekLast().length() > 0) {
				context.keyMustEnd = true;
			}
		});
		grammar.add("KEY_ -> key").withEvent((t, context) -> {
			if (context.keyMustEnd) {
				throw new CompilerException("Key '" + context.keysBuf.peekLast() + "' cannot continue after it has ended");
			}
			context.keysBuf.peekLast().append(t[0]);
		});
		grammar.add("KEY_ -> '.'").withEvent((t, context) -> {
			context.keysBuf.addLast(new StringBuilder());
			context.keyMustEnd = false;
		});
		grammar.add("KEY_ -> SL_STRING :VOID:").withPostEvent((t, context) -> {
			if (context.keyMustEnd) {
				throw new CompilerException("Key '" + context.keysBuf.peekLast() + "' cannot continue after it has ended");
			}
			context.keysBuf.peekLast().append(context.flushBuffer());
		});
		
		
		// --- Strings ---
		
		grammar.enableLookAhead("STRING");
		grammar.add("STRING -> ML_STRING");
		grammar.add("STRING -> SL_STRING");
		
		grammar.enableLookAhead("ML_STRING");
		grammar.enableLookAhead("SL_STRING");
		
		grammar.add("ML_STRING -> '\"\"\"' ML_BASIC_STRING_START").withEvent(clearBufferEvent());
		grammar.add("ML_BASIC_STRING_START -> :NEWLINE: ML_BASIC_STRING"); // Ignore first direct line break
		grammar.add("ML_BASIC_STRING_START -> ML_BASIC_STRING");
		
		grammar.add("ML_BASIC_STRING -> '\"\"\"\"\"'").withEvent(bufferEvent("\"\"")); // 2 quote characters + string end
		grammar.add("ML_BASIC_STRING -> '\"\"\"\"'").withEvent(bufferEvent('"')); // 1 quote character + string end
		grammar.add("ML_BASIC_STRING -> '\"\"\"'");
		// Note: The output will contain the system specific line separator. This doesn't exactly comply with the TOML spec.
		grammar.add("ML_BASIC_STRING -> :NEWLINE: THIS").withEvent(bufferEvent(System.lineSeparator()));
		grammar.add("ML_BASIC_STRING -> '\\' ML_ESCAPE THIS");
		grammar.add("ML_BASIC_STRING -> any THIS").withEvent(bufferEvent(0));
		
		grammar.add("ML_STRING -> ''''' ML_LITERAL_STRING_START").withEvent(clearBufferEvent());
		grammar.add("ML_LITERAL_STRING_START -> :NEWLINE: ML_LITERAL_STRING"); // Ignore first direct line break
		grammar.add("ML_LITERAL_STRING_START -> ML_LITERAL_STRING");
		
		grammar.add("ML_LITERAL_STRING -> '''''''").withEvent(bufferEvent("''")); // 2 quote characters + string end
		grammar.add("ML_LITERAL_STRING -> ''''''").withEvent(bufferEvent("'")); // 1 quote character + string end
		grammar.add("ML_LITERAL_STRING -> '''''");
		grammar.add("ML_LITERAL_STRING -> :NEWLINE: THIS").withEvent(bufferEvent(System.lineSeparator()));
		grammar.add("ML_LITERAL_STRING -> any THIS").withEvent(bufferEvent(0));
		
		grammar.add("SL_STRING -> '\"' BASIC_STRING").withEvent(clearBufferEvent());
		grammar.add("BASIC_STRING -> '\"'");
		// Note: The escape sequences :ESCAPE: provides, don't exactly match these defined by the TOML spec.
		grammar.add("BASIC_STRING -> '\\' BASIC_ESCAPE THIS");
		grammar.add("BASIC_STRING -> any THIS").withEvent(bufferEvent(0));
		
		grammar.add("SL_STRING -> ''' LITERAL_STRING").withEvent(clearBufferEvent());
		grammar.add("LITERAL_STRING -> '''");
		grammar.add("LITERAL_STRING -> any THIS").withEvent(bufferEvent(0));
		
		
		grammar.add("ML_ESCAPE -> BASIC_ESCAPE");
		// \<newline> skips all spaces and newlines until the next solid character.
		grammar.add("ML_ESCAPE -> space :SPACE: :NEWLINE: TRIM_SPACE");
		grammar.add("ML_ESCAPE -> :NEWLINE: TRIM_SPACE");
		
		grammar.add("TRIM_SPACE -> space THIS");
		grammar.add("TRIM_SPACE -> :NEWLINE: THIS");
		grammar.add("TRIM_SPACE ->");
		
		grammar.enableLookAhead("BASIC_ESCAPE");
		grammar.add("BASIC_ESCAPE -> 'b'").withEvent(bufferEvent('\b'));
		grammar.add("BASIC_ESCAPE -> 't'").withEvent(bufferEvent('\t'));
		grammar.add("BASIC_ESCAPE -> 'n'").withEvent(bufferEvent('\n'));
		grammar.add("BASIC_ESCAPE -> 'f'").withEvent(bufferEvent('\f'));
		grammar.add("BASIC_ESCAPE -> 'r'").withEvent(bufferEvent('\r'));
		grammar.add("BASIC_ESCAPE -> 'e'").withEvent(bufferEvent('\u001B'));
		grammar.add("BASIC_ESCAPE -> '\"'").withEvent(bufferEvent('"'));
		grammar.add("BASIC_ESCAPE -> '\\'").withEvent(bufferEvent('\\'));
		grammar.add("BASIC_ESCAPE -> 'x' hex hex").withEvent((t, context) -> {
			context.bufferHexCodePoint(new String(t, 1, 2));
		});
		grammar.add("BASIC_ESCAPE -> 'u' hex hex hex hex").withEvent((t, context) -> {
			context.bufferHexCodePoint(new String(t, 1, 4));
		});
		grammar.add("BASIC_ESCAPE -> 'U' hex hex hex hex hex hex hex hex").withEvent((t, context) -> {
			context.bufferHexCodePoint(new String(t, 1, 8));
		});
		
		
		// --- Numbers ---
		
		grammar.enableLookAhead("NUMBER");
		
		grammar.add("NUMBER -> '0x' hex HEX").withEvent(bufferEvent(2));
		grammar.add("HEX -> hex THIS").withEvent(bufferEvent(0));
		grammar.add("HEX -> '_' hex THIS").withEvent(bufferEvent(1));
		grammar.add("HEX ->").withEvent((t, context) -> {
			try {
				long value = Long.parseUnsignedLong(context.flushBuffer(), 16);
				context.insertIntValue(value).setOriginalType("integer");
			} catch (NumberFormatException e) {
				throw new CompilerException(e);
			}
		});
		
		grammar.add("NUMBER -> '0o' oct OCT").withEvent(bufferEvent(2));
		grammar.add("OCT -> oct THIS").withEvent(bufferEvent(0));
		grammar.add("OCT -> '_' oct THIS").withEvent(bufferEvent(1));
		grammar.add("OCT ->").withEvent((t, context) -> {
			try {
				long value = Long.parseUnsignedLong(context.flushBuffer(), 8);
				context.insertIntValue(value).setOriginalType("integer");
			} catch (NumberFormatException e) {
				throw new CompilerException(e);
			}
		});
		
		grammar.add("NUMBER -> '0b' bin BIN").withEvent(bufferEvent(2));
		grammar.add("BIN -> bin THIS").withEvent(bufferEvent(0));
		grammar.add("BIN -> '_' bin THIS").withEvent(bufferEvent(1));
		grammar.add("BIN ->").withEvent((t, context) -> {
			try {
				long value = Long.parseUnsignedLong(context.flushBuffer(), 2);
				context.insertIntValue(value).setOriginalType("integer");
			} catch (NumberFormatException e) {
				throw new CompilerException(e);
			}
		});
		
		grammar.add("NUMBER -> sign '0.' digit FLOAT").withEvent(bufferEvent(0)).withEvent(bufferEvent("0.")).withEvent(bufferEvent(3));
		grammar.add("NUMBER -> '0.' digit FLOAT").withEvent(bufferEvent("0.")).withEvent(bufferEvent(2));
		grammar.add("NUMBER -> '0e' sign digit EXP").withEvent(bufferEvent("0e")).withEvent(bufferEvent(2)).withEvent(bufferEvent(3));
		grammar.add("NUMBER -> '0e' digit EXP").withEvent(bufferEvent("0e")).withEvent(bufferEvent(2));
		grammar.add("NUMBER -> sign '0e' sign digit EXP").withEvent((t, context) -> context.buffer(new String(t, 0, 5)));
		grammar.add("NUMBER -> sign '0e' digit EXP").withEvent((t, context) -> context.buffer(new String(t, 0, 4)));
		grammar.add("NUMBER -> sign '0' digit").withEvent(PrefixLexerContext.errorEvent("Decimal numbers must not start with a leading zero"));
		grammar.add("NUMBER -> '0' digit").withEvent(PrefixLexerContext.errorEvent("Decimal numbers must not start with a leading zero"));
		grammar.add("NUMBER -> sign '0'").withEvent((t, context) -> context.insertIntValue(0L).setOriginalType("integer"));
		grammar.add("NUMBER -> '0'").withEvent((t, context) -> context.insertIntValue(0L).setOriginalType("integer"));
		grammar.add("NUMBER -> sign nonzero_digit DEC").withEvent(bufferEvent(0)).withEvent(bufferEvent(1));
		grammar.add("NUMBER -> nonzero_digit DEC").withEvent(bufferEvent(0));
		
		grammar.add("NUMBER -> '+inf'").withEvent((t, context) -> context.insertDoubleValue(Double.POSITIVE_INFINITY).setOriginalType("float"));
		grammar.add("NUMBER -> '-inf'").withEvent((t, context) -> context.insertDoubleValue(Double.NEGATIVE_INFINITY).setOriginalType("float"));
		grammar.add("NUMBER -> 'inf'").withEvent((t, context) -> context.insertDoubleValue(Double.POSITIVE_INFINITY).setOriginalType("float"));
		
		grammar.add("NUMBER -> sign 'nan'").withEvent((t, context) -> context.insertDoubleValue(Double.NaN).setOriginalType("float"));
		grammar.add("NUMBER -> 'nan'").withEvent((t, context) -> context.insertDoubleValue(Double.NaN).setOriginalType("float"));
		
		grammar.add("DEC -> digit THIS").withEvent(bufferEvent(0));
		grammar.add("DEC -> '_' digit THIS").withEvent(bufferEvent(1)); // Underscores must be surrounded by digits
		grammar.add("DEC -> '.' digit FLOAT").withEvent(bufferEvent(0)).withEvent(bufferEvent(1));
		grammar.add("DEC -> `e` sign digit EXP").withEvent(bufferEvent(0)).withEvent(bufferEvent(1)).withEvent(bufferEvent(2));
		grammar.add("DEC -> `e` digit EXP").withEvent(bufferEvent(0)).withEvent(bufferEvent(1));
		grammar.add("DEC ->").withEvent((t, context) -> {
			try {
				long value = Long.parseLong(context.flushBuffer());
				context.insertIntValue(value).setOriginalType("integer");
			} catch (NumberFormatException e) {
				throw new CompilerException(e);
			}
		});
		
		grammar.add("FLOAT -> digit THIS").withEvent(bufferEvent(0));
		grammar.add("FLOAT -> '_' digit THIS").withEvent(bufferEvent(1));
		grammar.add("FLOAT -> `e` sign digit EXP").withEvent(bufferEvent(0)).withEvent(bufferEvent(1)).withEvent(bufferEvent(2));
		grammar.add("FLOAT -> `e` digit EXP").withEvent(bufferEvent(0)).withEvent(bufferEvent(1));
		grammar.add("FLOAT ->").withEvent((t, context) -> {
			try {
				double value = Double.parseDouble(context.flushBuffer());
				context.insertDoubleValue(value).setOriginalType("float");
			} catch (NumberFormatException e) {
				throw new CompilerException(e);
			}
		});
		
		grammar.add("EXP -> digit THIS").withEvent(bufferEvent(0));
		grammar.add("EXP -> '_' digit THIS").withEvent(bufferEvent(1));
		grammar.add("EXP ->").withEvent((t, context) -> {
			try {
				double value = Double.parseDouble(context.flushBuffer());
				context.insertDoubleValue(value).setOriginalType("float");
			} catch (NumberFormatException e) {
				throw new CompilerException(e);
			}
		});
		
		
		// --- Arrays ---
		
		// Note: This implementation ignores commas, so multiple commas can follow each other.
		// This also allows to specify multiple values after each other with no separating comma.
		// This doesn't exactly match the TOML spec, but all valid TOML arrays are also valid for this implementation.
		grammar.enableLookAhead("ARRAY");
		grammar.add("ARRAY -> '[' INNER_ARRAY").withEvent((t, context) -> {
			ConfigListBuilder list = context.insertNewList();
			list.setOriginalType("array");
			context.inlineStack.addFirst(list);
		});
		grammar.add("INNER_ARRAY -> ']'").withEvent((t, context) -> {
			ConfigElementBuilder removed = context.inlineStack.removeFirst();
			if (!(removed instanceof ConfigListBuilder)) {
				throw new CompilerException("Invalid state: Array closed, but " + removed + " was found");
			}
			removed.close();
		});
		grammar.add("INNER_ARRAY -> space THIS");
		grammar.add("INNER_ARRAY -> ',' THIS");
		grammar.add("INNER_ARRAY -> :NEWLINE: THIS");
		grammar.add("INNER_ARRAY -> COMMENT THIS");
		grammar.add("INNER_ARRAY -> VALUE THIS");
		
		
		// --- Inline Tables ---
		
		// Note: This implementation allows multiple commas directly following each other, a trailing comma and line breaks.
		// This allows to specify key-value pairs in multiple lines without a separating comma.
		// This doesn't match the TOML spec, but all valid TOML inline tables are also valid for this implementation.
		grammar.enableLookAhead("INLINE_TABLE");
		grammar.add("INLINE_TABLE -> '{' INNER_INLINE_TABLE").withEvent((t, context) -> {
			ConfigObjectBuilder obj = context.insertNewObject();
			obj.setOriginalType("table-inline");
			context.inlineStack.addFirst(obj);
		});
		grammar.add("INNER_INLINE_TABLE -> '}'").withEvent((t, context) -> {
			ConfigElementBuilder removed = context.inlineStack.removeFirst();
			if (!(removed instanceof ConfigObjectBuilder)) {
				throw new CompilerException("Invalid state: Inline-Table closed, but " + removed + " was found");
			}
			removed.close();
		});
		grammar.add("INNER_INLINE_TABLE -> space THIS");
		grammar.add("INNER_INLINE_TABLE -> ',' THIS");
		grammar.add("INNER_INLINE_TABLE -> :NEWLINE: THIS");
		grammar.add("INNER_INLINE_TABLE -> COMMENT THIS");
		grammar.add("INNER_INLINE_TABLE -> KEY '=' VALUE THIS").withIntermediateEvent(1, (t, context) -> {
			context.key = context.keyBuffer();
		});
		
		
		// --- Date-Time ---
		
		grammar.enableLookAhead("DATE_TIME");
		grammar.add("DATE_TIME -> digit digit digit digit '-' digit digit '-' digit digit TIME_APPENDIX")
			.withEvent(clearBufferEvent())
			.withEvent((t, context) -> {
				context.buffer(new String(t, 0, "yyyy-mm-dd".length()));
				context.originalTypeBuf = "date-local"; // provisional
			});
		grammar.add("DATE_TIME -> TIME").withEvent(clearBufferEvent()).withEvent((t, context) -> {
			context.originalTypeBuf = "time-local";
		});
		
		grammar.add("TIME_APPENDIX -> `T` TIME TIME_OFFSET").withEvent(bufferEvent('T')).withEvent((t, context) -> {
			context.originalTypeBuf = "datetime-local"; // provisional
		});
		// There is no TIME_APPENDIX if there are multiple spaces without a digit (coming from the TIME).
		// There may be spaces after a DATE_TIME that should be ignored.
		grammar.add("TIME_APPENDIX -> space [digit] TIME TIME_OFFSET").withEvent(bufferEvent('T')).withEvent((t, context) -> {
			context.originalTypeBuf = "datetime-local"; // provisional
		});
		grammar.add("TIME_APPENDIX ->");
		
		grammar.enableLookAhead("TIME");
		grammar.add("TIME -> digit digit ':' digit digit ':' digit digit TIME_SEC_FRACTION").withEvent((t, context) -> {
			context.buffer(new String(t, 0, "hh:mm:ss".length()));
		});
		grammar.add("TIME -> digit digit ':' digit digit TIME_SEC_FRACTION").withEvent((t, context) -> {
			context.buffer(new String(t, 0, "hh:mm".length()));
		});
		
		grammar.add("TIME_SEC_FRACTION -> '.' digit TIME_SEC_FRACTION_LOOP").withEvent(bufferEvent(0)).withEvent(bufferEvent(1));
		grammar.add("TIME_SEC_FRACTION ->");
		grammar.add("TIME_SEC_FRACTION_LOOP -> digit THIS").withEvent(bufferEvent(0));
		grammar.add("TIME_SEC_FRACTION_LOOP ->");
		
		grammar.add("TIME_OFFSET -> `Z`").withEvent(bufferEvent('Z')).withEvent((t, context) -> {
			context.originalTypeBuf = "datetime";
		});
		grammar.add("TIME_OFFSET -> sign digit digit ':' digit digit").withEvent((t, context) -> {
			context.buffer(new String(t, 0, "+00:00".length()));
			context.originalTypeBuf = "datetime";
		});
		grammar.add("TIME_OFFSET ->");
		
		// --- VALUE ---
		
		grammar.add("VALUE -> space THIS");
		grammar.add("VALUE -> 'true'").withEvent((t, context) -> {
			context.insertBooleanValue(true).setOriginalType("bool");
		});
		grammar.add("VALUE -> 'false'").withEvent((t, context) -> {
			context.insertBooleanValue(false).setOriginalType("bool");
		});
		grammar.add("VALUE -> DATE_TIME :VOID:").withPostEvent((t, context) -> {
			// Date/Time values are represented as a string value.
			context.insertStringValue(context.flushBuffer()).setOriginalType(context.originalTypeBuf);
			context.originalTypeBuf = null;
		});
		grammar.add("VALUE -> INLINE_TABLE");
		grammar.add("VALUE -> ARRAY");
		grammar.add("VALUE -> NUMBER");
		grammar.add("VALUE -> STRING :VOID:").withPostEvent((t, context) -> {
			context.insertStringValue(context.flushBuffer()).setOriginalType("string");
		});
	}
	
	private static class Context extends PrefixLexerContextWithBuffer {
		private final ArrayDeque<StringBuilder> keysBuf = new ArrayDeque<>();
		private boolean keyMustEnd = false;
		
		private final ConfigObjectBuilder rootTable = new ConfigObjectBuilder(null, "");
		
		private ConfigObjectBuilder table = this.rootTable;
		private ArrayDeque<ConfigElementBuilder> inlineStack = new ArrayDeque<>();
		
		private String[] key = null;
		
		private String originalTypeBuf = null;
		
		
		public String[] keyBuffer() {
			return this.keysBuf.stream().map(buf -> buf.toString()).toArray(String[]::new);
		}
		
		@Override
		public boolean supportsLineCounter() {
			return true;
		}
		
		
		private static interface ListElementInserter<R extends ConfigElementBuilder, V> {
			public R insert(ConfigListBuilder list, V value) throws CompilerException;
		}
		
		private static interface ObjectElementInserter<R extends ConfigElementBuilder, V> {
			public R insert(ConfigObjectBuilder object, String[] key, V value) throws CompilerException;
		}
		
		private <R extends ConfigElementBuilder, V> R insert(V value,
				ListElementInserter<R, V> listInserter, ObjectElementInserter<R, V> objectInserter) throws CompilerException {
			ConfigElementBuilder inline = this.inlineStack.peekFirst();
			if (inline instanceof ConfigListBuilder) {
				return listInserter.insert((ConfigListBuilder) inline, value);
			}
			if (this.key == null) {
				throw new CompilerException("No key found to insert the value");
			}
			ConfigObjectBuilder table = this.table;
			if (inline instanceof ConfigObjectBuilder) {
				table = (ConfigObjectBuilder) inline;
			}
			return objectInserter.insert(table, this.key, value);
		}
		
		public ConfigElementBuilder insertStringValue(String value) throws CompilerException {
			return this.insert(value,
					(list, val) -> list.addString(val),
					(obj, key, val) -> obj.setString(key, val));
		}
		
		public ConfigElementBuilder insertBooleanValue(boolean value) throws CompilerException {
			return this.insert(value,
					(list, val) -> list.addBoolean(val),
					(obj, key, val) -> obj.setBoolean(key, val));
		}
		
		public ConfigElementBuilder insertIntValue(long value) throws CompilerException {
			return this.insert(value,
					(list, val) -> list.addInt(val),
					(obj, key, val) -> obj.setInt(key, val));
		}
		
		public ConfigElementBuilder insertDoubleValue(double value) throws CompilerException {
			return this.insert(value,
					(list, val) -> list.addDouble(val),
					(obj, key, val) -> obj.setDouble(key, val));
		}
		
		public ConfigListBuilder insertNewList() throws CompilerException {
			return this.insert(null,
					(list, val) -> list.addList(),
					(obj, key, val) -> obj.createList(key));
		}
		
		public ConfigObjectBuilder insertNewObject() throws CompilerException {
			return this.insert(null,
					(list, val) -> list.addObject(),
					(obj, key, val) -> obj.createObject(key));
		}
		
		public void bufferHexCodePoint(String codePointHex) throws CompilerException {
			try {
				int codePoint = HexUtil.hexToInt(codePointHex);
				String str = new String(new int[] {codePoint}, 0, 1);
				this.buffer(str);
			} catch (IllegalArgumentException e) {
				throw new CompilerException(e);
			}
		}
	}
	
	
	public static ConfigElement parse(Reader reader) throws CompilerException, IOException {
		Context context = new Context();
		grammar.run(reader, context);
		return context.rootTable.toElement();
	}
}
