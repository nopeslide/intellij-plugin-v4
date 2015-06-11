package org.antlr.intellij.plugin.test;

import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewParser;
import org.antlr.intellij.plugin.parsing.TokenStreamSubset;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.LookaheadEventInfo;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestAmbigPreviewTrees {
	public static final String lexerText =
		"lexer grammar L;\n" +
		"DOT  : '.' ;\n" +
		"SEMI : ';' ;\n" +
		"BANG : '!' ;\n" +
		"LPAREN : '(' ;\n" +
		"RPAREN : ')' ;\n" +
		"MULT : '*' ;\n" +
		"PLUS : '+' ;\n" +
		"ID : [a-z]+ ;\n" +
		"INT : [0-9]+ ;\n" +
		"WS : [ \\r\\t\\n]+ -> channel(HIDDEN);\n";

	@Test
	public void testAlts() throws Exception {
		LexerGrammar lg = new LexerGrammar(lexerText);
		Grammar g = new Grammar(
			"parser grammar T;\n" +
			"s : e SEMI EOF ;\n" +
			"e : ID DOT ID\n"+
			"  | ID LPAREN RPAREN\n"+
			"  ;\n",
			lg);

		int decision = 0;
		testLookaheadTrees(lg, g, "a.b;", "s", decision,
						   new String[] {"(e:1 a . b)", "(e:2 a <error .> <error b>)"});
	}

	@Test
	public void testIncludeEOF() throws Exception {
		LexerGrammar lg = new LexerGrammar(lexerText);
		Grammar g = new Grammar(
			"parser grammar T;\n" +
			"s : e ;\n" +
			"e : ID DOT ID EOF\n"+
			"  | ID DOT ID EOF\n"+
			"  ;\n",
			lg);

		int decision = 0;
		testLookaheadTrees(lg, g, "a.b", "s", decision,
						   new String[] {"(e:1 a . b <EOF>)", "(e:2 a . b <EOF>)"});
	}

	@Test
	public void testCallLeftRecursiveRule() throws Exception {
		LexerGrammar lg = new LexerGrammar(lexerText);
		Grammar g = new Grammar(
			"parser grammar T;\n" +
			"s : a BANG EOF;\n" +
			"a : e SEMI \n" +
			"  | ID SEMI \n" +
			"  ;" +
			"e : e MULT e\n" +
			"  | e PLUS e\n" +
			"  | ID\n" +
			"  | INT\n" +
			"  ;\n",
			lg);

		int decision = 0;
		testLookaheadTrees(lg, g, "x;!", "s", decision,
						   new String[] {"(a:1 (e:3 x) ;)", "(a:2 x ;)"}); // shouldn't include BANG, EOF
	}

	public void testLookaheadTrees(LexerGrammar lg, Grammar g,
								   String input,
								   String startRuleName,
								   int decision,
								   String[] expectedTrees)
	{
		int startRuleIndex = g.getRule(startRuleName).index;
		InterpreterRuleContextTextProvider nodeTextProvider =
					new InterpreterRuleContextTextProvider(g.getRuleNames());

		// force parsing first so that we had DFA
		LexerInterpreter lexEngine = lg.createLexerInterpreter(new ANTLRInputStream(input));
		CommonTokenStream tokens = new TokenStreamSubset(lexEngine);
		PreviewParser parser = new PreviewParser(g, tokens);
		parser.setProfile(true);
		ParseTree t = parser.parse(startRuleIndex);
		System.out.println("result tree="+Trees.toStringTree(t, nodeTextProvider));

		LookaheadEventInfo lookaheadEventInfo = parser.getParseInfo().getDecisionInfo()[decision].SLL_MaxLookEvent;
//			new LookaheadEventInfo(decision, null, tokens, startRuleIndex, stopTokenIndex, false);


		List<ParserRuleContext> lookaheadParseTrees =
			ParsingUtils.getLookaheadParseTrees(parser, startRuleIndex, lookaheadEventInfo.decision,
												lookaheadEventInfo.startIndex, lookaheadEventInfo.stopIndex);

		assertEquals(expectedTrees.length, lookaheadParseTrees.size());
		for (int i = 0; i < lookaheadParseTrees.size(); i++) {
			ParserRuleContext lt = lookaheadParseTrees.get(i);
			assertEquals(expectedTrees[i], Trees.toStringTree(lt, nodeTextProvider));
		}
	}
}
