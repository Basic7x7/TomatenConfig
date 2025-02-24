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

class TOMLConfigParser {
	private static final LexicalSymbolSet<Context> symbolSet = LexicalSymbolSet.createDefault();
	static {
		symbolSet.add("key", (c, context) -> ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') ||
				('0' <= c && c <= '9') || c == '_' || c == '-');
		symbolSet.add("oct", (c, context) -> '0' <= c && c <= '7');
		symbolSet.add("sign", (c, context) -> c == '+' || c == '-');
	}
	
	private static final PrefixLexerGrammar<Context> grammar = new PrefixLexerGrammar<>(symbolSet, "LINESTART", PrefixLexerOption.IGNORE_UTF8_BOM);
	static {
		grammar.enableLookAhead("COMMENT");
		grammar.add("COMMENT -> '#' INNER_COMMENT");
		grammar.add("COMMENT -> :NEWLINE: LINESTART");
		grammar.add("INNER_COMMENT -> any THIS");
		grammar.add("INNER_COMMENT -> :NEWLINE: LINESTART");
		
		
		grammar.add("LINESTART -> space THIS");
		grammar.add("LINESTART -> COMMENT");
		grammar.add("LINESTART -> '[[' KEY ']]' LINEEND").withPostEvent((t, context) -> {
			context.key = context.keyBuffer();
			ConfigListBuilder list = context.rootTable.createOrGetList(context.key);
			context.table = list.addObject();
		});
		grammar.add("LINESTART -> '[' KEY ']' LINEEND").withPostEvent((t, context) -> {
			context.key = context.keyBuffer();
			context.table = context.rootTable.createObject(context.key);
		});
		grammar.add("LINESTART -> KEY '=' VALUE LINEEND").withIntermediateEvent(1, (t, context) -> {
			context.key = context.keyBuffer();
			context.value = null;
		});
		grammar.add("LINESTART ->"); // Final state
		
		
		grammar.add("LINEEND -> space THIS");
		grammar.add("LINEEND -> COMMENT");
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
		grammar.add("KEY_ -> STRING :VOID:").withPostEvent((t, context) -> {
			if (context.keyMustEnd) {
				throw new CompilerException("Key '" + context.keysBuf.peekLast() + "' cannot continue after it has ended");
			}
			context.keysBuf.peekLast().append(context.flushBuffer());
		});
		
		
		// --- Strings ---
		
		grammar.enableLookAhead("STRING");
		
		grammar.add("STRING -> '\"\"\"' ML_BASIC_STRING_START").withEvent(clearBufferEvent()); 
		grammar.add("ML_BASIC_STRING_START -> :NEWLINE: ML_BASIC_STRING"); // Ignore first direct line break
		grammar.add("ML_BASIC_STRING_START -> ML_BASIC_STRING");
		
		grammar.add("ML_BASIC_STRING -> '\"\"\"'");
		// Note: The output will contain the system specific line separator. This doesn't exactly comply with the TOML spec.
		grammar.add("ML_BASIC_STRING -> :NEWLINE: THIS").withEvent(bufferEvent(System.lineSeparator()));
		// \<newline> skips the leading spaces in the next line.
		grammar.add("ML_BASIC_STRING -> '\\' :NEWLINE: :SPACE: THIS").withEvent(bufferEvent(System.lineSeparator()));
		grammar.add("ML_BASIC_STRING -> '\\' :ESCAPE: THIS");
		grammar.add("ML_BASIC_STRING -> any THIS").withEvent(bufferEvent(0));
		
		grammar.add("STRING -> ''''' :NEWLINE: ML_LITERAL_STRING_START").withEvent(clearBufferEvent());
		grammar.add("ML_LITERAL_STRING_START -> :NEWLINE: ML_LITERAL_STRING"); // Ignore first direct line break
		grammar.add("ML_LITERAL_STRING_START -> ML_LITERAL_STRING");
		
		grammar.add("ML_LITERAL_STRING -> '''''");
		grammar.add("ML_LITERAL_STRING -> :NEWLINE: THIS").withEvent(bufferEvent(System.lineSeparator()));
		grammar.add("ML_LITERAL_STRING -> any THIS").withEvent(bufferEvent(0));
		
		grammar.add("STRING -> '\"' BASIC_STRING").withEvent(clearBufferEvent());
		grammar.add("BASIC_STRING -> '\"'");
		// Note: The escape sequences :ESCAPE: provides, don't exactly match these defined by the TOML spec.
		grammar.add("BASIC_STRING -> '\\' :ESCAPE: THIS");
		grammar.add("BASIC_STRING -> any THIS").withEvent(bufferEvent(0));
		
		grammar.add("STRING -> ''' LITERAL_STRING").withEvent(clearBufferEvent());
		grammar.add("LITERAL_STRING -> '''");
		grammar.add("LITERAL_STRING -> any THIS").withEvent(bufferEvent(0));
		
		
		// --- NUmbers ---
		
		grammar.enableLookAhead("NUMBER");
		
		grammar.add("NUMBER -> '0x' hex HEX").withEvent(bufferEvent(2));
		grammar.add("HEX -> hex THIS").withEvent(bufferEvent(0));
		grammar.add("HEX -> '_' hex THIS").withEvent(bufferEvent(1));
		grammar.add("HEX ->").withEvent((t, context) -> {
			try {
				long value = Long.parseUnsignedLong(context.flushBuffer(), 16);
				context.value = context.insertIntValue(value);
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
				context.value = context.insertIntValue(value);
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
				context.value = context.insertIntValue(value);
			} catch (NumberFormatException e) {
				throw new CompilerException(e);
			}
		});
		
		grammar.add("NUMBER -> sign '0.' digit FLOAT").withEvent(bufferEvent(0)).withEvent(bufferEvent("0.")).withEvent(bufferEvent(3));
		grammar.add("NUMBER -> '0.' digit FLOAT").withEvent(bufferEvent("0.")).withEvent(bufferEvent(3));
		grammar.add("NUMBER -> sign '0' digit").withEvent(PrefixLexerContext.errorEvent("Decimal numbers must not start with a leading zero"));
		grammar.add("NUMBER -> '0' digit").withEvent(PrefixLexerContext.errorEvent("Decimal numbers must not start with a leading zero"));
		grammar.add("NUMBER -> sign '0'").withEvent((t, context) -> context.value = context.insertIntValue(0L));
		grammar.add("NUMBER -> '0'").withEvent((t, context) -> context.value = context.insertIntValue(0L));
		grammar.add("NUMBER -> sign nonzero_digit DEC").withEvent(bufferEvent(0)).withEvent(bufferEvent(1));
		grammar.add("NUMBER -> nonzero_digit DEC").withEvent(bufferEvent(0));
		
		grammar.add("NUMBER -> '+inf'").withEvent((t, context) -> context.value = context.insertDoubleValue(Double.POSITIVE_INFINITY));
		grammar.add("NUMBER -> '-inf'").withEvent((t, context) -> context.value = context.insertDoubleValue(Double.NEGATIVE_INFINITY));
		grammar.add("NUMBER -> 'inf'").withEvent((t, context) -> context.value = context.insertDoubleValue(Double.POSITIVE_INFINITY));
		
		grammar.add("NUMBER -> sign 'nan'").withEvent((t, context) -> context.value = context.insertDoubleValue(Double.NaN));
		grammar.add("NUMBER -> 'nan'").withEvent((t, context) -> context.value = context.insertDoubleValue(Double.NaN));
		
		grammar.add("DEC -> digit THIS").withEvent(bufferEvent(0));
		grammar.add("DEC -> '_' digit THIS").withEvent(bufferEvent(1)); // Underscores must be surrounded by digits
		grammar.add("DEC -> '.' digit FLOAT").withEvent(bufferEvent(0)).withEvent(bufferEvent(1));
		grammar.add("DEC -> `e` sign digit EXP").withEvent(bufferEvent(0)).withEvent(bufferEvent(1)).withEvent(bufferEvent(2));
		grammar.add("DEC -> `e` digit EXP").withEvent(bufferEvent(0)).withEvent(bufferEvent(1));
		grammar.add("DEC ->").withEvent((t, context) -> {
			try {
				long value = Long.parseLong(context.flushBuffer());
				context.value = context.insertIntValue(value);
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
				context.value = context.insertDoubleValue(value);
			} catch (NumberFormatException e) {
				throw new CompilerException(e);
			}
		});
		
		grammar.add("EXP -> digit THIS").withEvent(bufferEvent(0));
		grammar.add("EXP -> '_' digit THIS").withEvent(bufferEvent(1));
		grammar.add("EXP ->").withEvent((t, context) -> {
			try {
				double value = Double.parseDouble(context.flushBuffer());
				context.value = context.insertDoubleValue(value);
			} catch (NumberFormatException e) {
				throw new CompilerException(e);
			}
		});
		
		
		grammar.add("VALUE -> space THIS");
		grammar.add("VALUE -> 'true'").withEvent((t, context) -> {
			context.value = context.insertBooleanValue(true);
		});
		grammar.add("VALUE -> 'false'").withEvent((t, context) -> {
			context.value = context.insertBooleanValue(false);
		});
		// TODO Array, Inline Table, ...
		grammar.add("VALUE -> NUMBER");
		grammar.add("VALUE -> STRING :VOID:").withPostEvent((t, context) -> {
			context.value = context.insertStringValue(context.flushBuffer());
		});
	}
	
	private static class Context extends PrefixLexerContextWithBuffer {
		private final ArrayDeque<StringBuilder> keysBuf = new ArrayDeque<>();
		private boolean keyMustEnd = false;
		
		private final ConfigObjectBuilder rootTable = new ConfigObjectBuilder(null, "");
		
		private ConfigObjectBuilder table = this.rootTable;
		private ConfigListBuilder list = null;
		
		private String[] key = null;
		private ConfigElementBuilder value = null;
		
		
		public String[] keyBuffer() {
			return this.keysBuf.stream().map(buf -> buf.toString()).toArray(String[]::new);
		}
		
		@Override
		public boolean supportsLineCounter() {
			return true;
		}
		
		
		public ConfigElementBuilder insertStringValue(String value) throws CompilerException {
			if (this.list != null) {
				return this.list.addString(value);
			}
			if (this.key == null) {
				throw new CompilerException("No key found to insert the string value");
			}
			return this.table.setString(this.key, value);
		}
		
		public ConfigElementBuilder insertBooleanValue(boolean value) throws CompilerException {
			if (this.list != null) {
				return this.list.addBoolean(value);
			}
			if (this.key == null) {
				throw new CompilerException("No key found to insert the boolean value");
			}
			return this.table.setBoolean(this.key, value);
		}
		
		public ConfigElementBuilder insertIntValue(long value) throws CompilerException {
			if (this.list != null) {
				return this.list.addInt(value);
			}
			if (this.key == null) {
				throw new CompilerException("No key found to insert the int value");
			}
			return this.table.setInt(this.key, value);
		}
		
		public ConfigElementBuilder insertDoubleValue(double value) throws CompilerException {
			if (this.list != null) {
				return this.list.addDouble(value);
			}
			if (this.key == null) {
				throw new CompilerException("No key found to insert the double value");
			}
			return this.table.setDouble(this.key, value);
		}
	}
	
	
	public static ConfigElement parse(Reader reader) throws CompilerException, IOException {
		Context context = new Context();
		grammar.run(reader, context);
		return context.rootTable.toElement();
	}
}
