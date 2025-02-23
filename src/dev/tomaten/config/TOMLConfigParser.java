package dev.tomaten.config;

import static de.tomatengames.lib.compiler.prefixlexer.PrefixLexerContextWithBuffer.bufferEvent;
import static de.tomatengames.lib.compiler.prefixlexer.PrefixLexerContextWithBuffer.clearBufferEvent;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;

import de.tomatengames.lib.compiler.CompilerException;
import de.tomatengames.lib.compiler.LexicalSymbolSet;
import de.tomatengames.lib.compiler.prefixlexer.PrefixLexerContextWithBuffer;
import de.tomatengames.lib.compiler.prefixlexer.PrefixLexerGrammar;
import de.tomatengames.lib.compiler.prefixlexer.PrefixLexerOption;

class TOMLConfigParser {
	private static final LexicalSymbolSet<Context> symbolSet = LexicalSymbolSet.createDefault();
	static {
		symbolSet.add("key", (c, context) -> ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') ||
				('0' <= c && c <= '9') || c == '_' || c == '-');
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
		
		
		
		
		grammar.add("VALUE -> space THIS");
		grammar.add("VALUE -> 'true'").withEvent((t, context) -> {
			context.value = context.insertBooleanValue(true);
		});
		grammar.add("VALUE -> 'false'").withEvent((t, context) -> {
			context.value = context.insertBooleanValue(false);
		});
		// TODO Numbers, ...
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
