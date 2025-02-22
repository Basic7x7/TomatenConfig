package dev.tomaten.config;

import static de.tomatengames.lib.compiler.prefixlexer.PrefixLexerContextWithBuffer.bufferEvent;
import static de.tomatengames.lib.compiler.prefixlexer.PrefixLexerContextWithBuffer.clearBufferEvent;

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
		grammar.add("LINESTART -> space THIS");
		grammar.add("LINESTART -> KEY '=' VALUE LINEEND").withEvent((t, context) -> {
			context.keysBuf.clear();
			context.keysBuf.add(new StringBuilder());
			context.keyMustEnd = false;
		}); // TODO post event
		
		
		
		// --- Keys ---
		
		grammar.enableLookAhead("KEY");
		grammar.add("KEY -> space THIS").withEvent((t, context) -> {
			if (context.keysBuf.peekLast().length() > 0) {
				context.keyMustEnd = true;
			}
		});
		grammar.add("KEY -> key THIS").withEvent((t, context) -> {
			if (context.keyMustEnd) {
				throw new CompilerException("Key '" + context.keysBuf.peekLast() + "' cannot continue after it has ended", context.getLine());
			}
			context.keysBuf.peekLast().append(t[0]);
		});
		grammar.add("KEY -> '.' THIS").withEvent((t, context) -> {
			context.keysBuf.addLast(new StringBuilder());
			context.keyMustEnd = false;
		});
		grammar.add("KEY -> STRING KEY_END").withPostEvent((t, context) -> {
			if (context.keyMustEnd) {
				throw new CompilerException("Key '" + context.keysBuf.peekLast() + "' cannot continue after it has ended", context.getLine());
			}
			context.keysBuf.peekLast().append(context.flushBuffer());
		});
		grammar.add("KEY ->").withEvent((t, context) -> context.keyMustEnd = false);
		
		
		// --- Strings ---
		
		grammar.enableLookAhead("STRING");
		grammar.add("STRING -> '\"' BASIC_STRING").withEvent(clearBufferEvent());
		grammar.add("BASIC_STRING -> '\"'");
		// Note: The escape sequences :ESCAPE: provides, don't exactly match these defined by the TOML spec.
		grammar.add("BASIC_STRING -> '\\' :ESCAPE: THIS");
		grammar.add("BASIC_STRING -> any THIS").withEvent(bufferEvent(0));
		
		grammar.add("STRING -> ''' LITERAL_STRING").withEvent(clearBufferEvent());
		grammar.add("LITERAL_STRING -> '''");
		grammar.add("LITERAL_STRING -> any THIS").withEvent(bufferEvent(0));
		
		grammar.add("STRING -> '\"\"\"' :NEWLINE: ML_BASIC_STRING").withEvent(clearBufferEvent()); // Ignore first direct line break
		grammar.add("STRING -> '\"\"\"' ML_BASIC_STRING").withEvent(clearBufferEvent());
		grammar.add("ML_BASIC_STRING -> '\"\"\"'");
		// Note: The output will contain the system specific line separator. This doesn't exactly comply with the TOML spec.
		grammar.add("ML_BASIC_STRING -> :NEWLINE: THIS").withEvent(bufferEvent(System.lineSeparator()));
		// \<newline> skips the leading spaces in the next line.
		grammar.add("ML_BASIC_STRING -> '\\' :NEWLINE: :SPACE: THIS").withEvent(bufferEvent(System.lineSeparator()));
		grammar.add("ML_BASIC_STRING -> '\\' :ESCAPE: THIS");
		grammar.add("ML_BASIC_STRING -> any THIS").withEvent(bufferEvent(0));
		
		grammar.add("STRING -> ''''' :NEWLINE: ML_LITERAL_STRING").withEvent(clearBufferEvent()); // Ignore first direct line break
		grammar.add("STRING -> ''''' ML_LITERAL_STRING").withEvent(clearBufferEvent());
		grammar.add("ML_LITERAL_STRING -> '''''");
		grammar.add("ML_LITERAL_STRING -> :NEWLINE: THIS").withEvent(bufferEvent(System.lineSeparator()));
		grammar.add("ML_LITERAL_STRING -> any THIS").withEvent(bufferEvent(0));
	}
	
	private static class Context extends PrefixLexerContextWithBuffer {
		private final ArrayDeque<StringBuilder> keysBuf = new ArrayDeque<>();
		private boolean keyMustEnd = false;
	}
	
	
	public static ConfigElement parse(Reader r) throws ConfigError {
		return null; // TODO
	}
}
