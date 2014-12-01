/* LanguageTool, a natural language style checker
 * Copyright (C) 2013 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules.patterns;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Demo;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.patterns.Match.CaseConversion;
import org.languagetool.rules.patterns.Match.IncludeRange;

public class PatternRuleMatcherTest {

  private static JLanguageTool langTool;

  @BeforeClass
  public static void setup() throws IOException {
    langTool = new JLanguageTool(new Demo());
  }

  @Test
  public void testMatch() throws Exception {
    final PatternRuleMatcher matcher = new PatternRuleMatcher(getPatternRule("my test"), false);
    assertPartialMatch("This is my test.", matcher);
    assertNoMatch("This is no test.", matcher);
  }

  @Test
  public void testZeroMinOccurrences() throws Exception {
    final Element elementB = makeElement("b");
    elementB.setMinOccurrence(0);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), elementB, makeElement("c"));  // regex syntax: a b? c
    assertNoMatch("b a", matcher);
    assertNoMatch("c a b", matcher);
    assertPartialMatch("b a c", matcher);
    assertPartialMatch("a c b", matcher);
    assertNoMatch("a b b c", matcher);
    assertCompleteMatch("a c", matcher);
    assertCompleteMatch("a b c", matcher);
    assertNoMatch("a X c", matcher);
    final RuleMatch[] matches = getMatches("a b c FOO a b c FOO a c a b c", matcher);
    //......................................^^^^^.....^^^^^.....^^^.^^^^^
    assertThat(matches.length, is(4));
    assertPosition(matches[0], 0, 5);
    assertPosition(matches[1], 10, 15);
    assertPosition(matches[2], 20, 23);
    assertPosition(matches[3], 24, 29);
  }

  @Test
  public void testZeroMinOccurrences2() throws Exception {
    final Element elementB = makeElement("b");
    elementB.setMinOccurrence(0);
    // regex syntax: a b? c d e
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), elementB, makeElement("c"), makeElement("d"), makeElement("e"));
    assertCompleteMatch("a b c d e", matcher);
    assertCompleteMatch("a c d e", matcher);
    assertNoMatch("a d", matcher);
    assertNoMatch("a c b d", matcher);
    assertNoMatch("a c b d e", matcher);
  }

  @Test
  public void testZeroMinOccurrences3() throws Exception {
    final Element elementC = makeElement("c");
    elementC.setMinOccurrence(0);
    // regex syntax: a b c? d e
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), makeElement("b"), elementC, makeElement("d"), makeElement("e"));
    assertCompleteMatch("a b c d e", matcher);
    assertCompleteMatch("a b d e", matcher);
    assertPartialMatch("a b c d e x", matcher);
    assertPartialMatch("x a b c d e", matcher);
    assertNoMatch("a b c e d", matcher);
    assertNoMatch("a c b d e", matcher);
  }

  @Test
  public void testZeroMinOccurrences4() throws Exception {
    final Element elementA = makeElement("a");
    elementA.setMinOccurrence(0);
    final Element elementC = makeElement("c");
    elementC.setMinOccurrence(0);
    // regex syntax: a? b c? d e
    final PatternRuleMatcher matcher = getMatcher(elementA, makeElement("b"), elementC, makeElement("d"), makeElement("e"));
    final RuleMatch[] matches = getMatches("a b c d e", matcher);
    assertThat(matches.length, is(1));  // just the longest match...
    assertPosition(matches[0], 0, 9);
  }

  @Test
  public void testZeroMinOccurrencesWithEmptyElement() throws Exception {
    final Element elementB = makeElement(null);
    elementB.setMinOccurrence(0);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), elementB, makeElement("c"));  // regex syntax: a .? c
    assertNoMatch("b a", matcher);
    assertNoMatch("c a b", matcher);
    assertPartialMatch("b a c", matcher);
    assertPartialMatch("a c b", matcher);
    assertNoMatch("a b b c", matcher);
    assertCompleteMatch("a c", matcher);
    assertCompleteMatch("a b c", matcher);
    assertCompleteMatch("a X c", matcher);
    final RuleMatch[] matches = getMatches("a b c FOO a X c", matcher);
    //......................................^^^^^.....^^^^^
    assertThat(matches.length, is(2));
    assertPosition(matches[0], 0, 5);
    assertPosition(matches[1], 10, 15);
  }

  @Test
  public void testZeroMinOccurrencesWithSuggestion() throws Exception {
    final Element elementB = makeElement("b");
    elementB.setMinOccurrence(0);
    
    List<Element> elements = Arrays.asList(makeElement("a"), elementB, makeElement("c"));   // regex: a b? c
    PatternRule rule = new PatternRule("", new Demo(), elements, "my description", "<suggestion>\\1 \\2 \\3</suggestion>", "short message");
    PatternRuleMatcher matcher = new PatternRuleMatcher(rule, false);
    
    // we need to add this line to trigger proper replacement but I am not sure why :(
    rule.addSuggestionMatch(new Match(null, null, false, null, null, CaseConversion.NONE, false, false, IncludeRange.NONE));
    
    RuleMatch[] matches = getMatches("a b c", matcher);
    assertEquals(Arrays.asList("a b c"), matches[0].getSuggestedReplacements());

    RuleMatch[] matches2 = getMatches("a c", matcher);
    assertEquals(Arrays.asList("a c"), matches2[0].getSuggestedReplacements());
  }

  @Test
  @Ignore("min can only be 0 or 1 so far")
  public void testTwoMinOccurrences() throws Exception {
    final Element elementB = makeElement("b");
    elementB.setMinOccurrence(2);
    elementB.setMaxOccurrence(3);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), elementB, makeElement("c"));  // regex: a b{2,3} c
    assertCompleteMatch("a b b c", matcher);
    assertCompleteMatch("a b b b c", matcher);
    assertNoMatch("a c", matcher);
    assertNoMatch("a b c", matcher);
  }

  @Test
  public void testZeroMinTwoMaxOccurrences() throws Exception {
    final Element elementB = makeElement("b");
    elementB.setMinOccurrence(0);
    elementB.setMaxOccurrence(2);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), elementB, makeElement("c"));
    assertCompleteMatch("a c", matcher);
    assertCompleteMatch("a  b c", matcher);
    assertCompleteMatch("a  b b c", matcher);
    assertNoMatch("a b b b c", matcher);
  }

  @Test
  public void testTwoMaxOccurrencesWithAnyToken() throws Exception {
    final Element anyElement = makeElement(null);
    anyElement.setMaxOccurrence(2);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), anyElement, makeElement("c"));
    assertCompleteMatch("a b c", matcher);
    assertCompleteMatch("a b b c", matcher);
    assertNoMatch("a b b b c", matcher);
  }

  @Test
  public void testThreeMaxOccurrencesWithAnyToken() throws Exception {
    final Element anyElement = makeElement(null);
    anyElement.setMaxOccurrence(3);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), anyElement, makeElement("c"));
    assertCompleteMatch("a b c", matcher);
    assertCompleteMatch("a b b c", matcher);
    assertCompleteMatch("a b b b c", matcher);
    assertNoMatch("a b b b b c", matcher);
  }

  @Test
  public void testZeroMinTwoMaxOccurrencesWithAnyToken() throws Exception {
    final Element anyElement = makeElement(null);
    anyElement.setMinOccurrence(0);
    anyElement.setMaxOccurrence(2);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), anyElement, makeElement("c"));
    assertNoMatch("a b", matcher);
    assertNoMatch("b c", matcher);
    assertNoMatch("c", matcher);
    assertNoMatch("a", matcher);
    assertCompleteMatch("a c", matcher);
    assertCompleteMatch("a x c", matcher);
    assertCompleteMatch("a x x c", matcher);
    assertNoMatch("a x x x c", matcher);
  }

  @Test
  public void testTwoMaxOccurrences() throws Exception {
    final Element elementB = makeElement("b");
    elementB.setMaxOccurrence(2);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), elementB);
    assertNoMatch("a a", matcher);
    assertCompleteMatch("a b", matcher);
    assertCompleteMatch("a b b", matcher);
    assertPartialMatch("a b c", matcher);
    assertPartialMatch("a b b c", matcher);
    assertPartialMatch("x a b b", matcher);

    final RuleMatch[] matches1 = getMatches("a b b b", matcher);
    assertThat(matches1.length, is(1));
    assertPosition(matches1[0], 0, 5);

    final RuleMatch[] matches2 = getMatches("a b b b foo a b b", matcher);
    assertThat(matches2.length, is(2));
    assertPosition(matches2[0], 0, 5);
    assertPosition(matches2[1], 12, 17);
  }

  @Test
  public void testThreeMaxOccurrences() throws Exception {
    final Element elementB = makeElement("b");
    elementB.setMaxOccurrence(3);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), elementB);  // regex: a b{1,3}
    assertNoMatch("a a", matcher);
    assertCompleteMatch("a b", matcher);
    assertCompleteMatch("a b b", matcher);
    assertCompleteMatch("a b b b", matcher);
    assertPartialMatch("a b b b b", matcher);

    final RuleMatch[] matches1 = getMatches("a b b b b", matcher);
    assertThat(matches1.length, is(1));
    assertPosition(matches1[0], 0, 7);
  }

  @Test
  public void testOptionalWithoutExplicitMarker() throws Exception {
    final Element elementA = makeElement("a");
    final Element elementB = makeElement("b");
    elementB.setMinOccurrence(0);
    final Element elementC = makeElement("c");
    final PatternRuleMatcher matcher = getMatcher(elementA, elementB, elementC);  // regex syntax: a .? c

    final RuleMatch[] matches1 = getMatches("A B C ZZZ", matcher);
    assertThat(matches1.length, is(1));
    assertPosition(matches1[0], 0, 5);

    final RuleMatch[] matches2 = getMatches("A C ZZZ", matcher);
    assertThat(matches2.length , is(1));
    assertPosition(matches2[0], 0, 3);
  }

  @Test
  public void testOptionalWithExplicitMarker() throws Exception {
    final Element elementA = makeElement("a");
    elementA.setInsideMarker(true);
    final Element elementB = makeElement("b");
    elementB.setMinOccurrence(0);
    elementB.setInsideMarker(true);
    final Element elementC = makeElement("c");
    elementC.setInsideMarker(false);
    final PatternRuleMatcher matcher = getMatcher(elementA, elementB, elementC);  // regex syntax: (a .?) c

    final RuleMatch[] matches1 = getMatches("A B C ZZZ", matcher);
    //.......................................^^^--
    assertThat(matches1.length , is(1));
    assertPosition(matches1[0], 0, 3);

    final RuleMatch[] matches2 = getMatches("A C ZZZ", matcher);
    //.......................................^--
    assertThat(matches2.length , is(1));
    assertPosition(matches2[0], 0, 1);
  }

  @Test
  public void testOptionalAnyTokenWithExplicitMarker() throws Exception {
    final Element elementA = makeElement("a");
    elementA.setInsideMarker(true);
    final Element elementB = makeElement(null);
    elementB.setMinOccurrence(0);
    elementB.setInsideMarker(true);
    final Element elementC = makeElement("c");
    elementC.setInsideMarker(false);
    final PatternRuleMatcher matcher = getMatcher(elementA, elementB, elementC);  // regex syntax: (a .?) c

    final RuleMatch[] matches1 = getMatches("A x C ZZZ", matcher);
    //.......................................^^^--
    assertThat(matches1.length , is(1));
    assertPosition(matches1[0], 0, 3);

    final RuleMatch[] matches2 = getMatches("A C ZZZ", matcher);
    //.......................................^--
    assertThat(matches2.length , is(1));
    assertPosition(matches2[0], 0, 1);
  }

  @Test
  public void testOptionalAnyTokenWithExplicitMarker2() throws Exception {
    final Element elementA = makeElement("the");
    elementA.setInsideMarker(true);
    final Element elementB = makeElement(null);
    elementB.setMinOccurrence(0);
    elementB.setInsideMarker(true);
    final Element elementC = makeElement("bike");
    elementC.setInsideMarker(false);
    final PatternRuleMatcher matcher = getMatcher(elementA, elementB, elementC);  // regex syntax: (a .?) c

    final RuleMatch[] matches1 = getMatches("the nice bike ZZZ", matcher);
    //.......................................^^^^^^^^-----
    assertThat(matches1.length , is(1));
    assertPosition(matches1[0], 0, 8);

    final RuleMatch[] matches2 = getMatches("the bike ZZZ", matcher);
    //.......................................^^^-----
    assertThat(matches2.length, is(1));
    assertPosition(matches2[0], 0, 3);
  }

  @Test
  public void testUnlimitedMaxOccurrences() throws Exception {
    final Element elementB = makeElement("b");
    elementB.setMaxOccurrence(-1);
    final PatternRuleMatcher matcher = getMatcher(makeElement("a"), elementB, makeElement("c"));
    assertNoMatch("a c", matcher);
    assertNoMatch("a b", matcher);
    assertNoMatch("b c", matcher);
    assertCompleteMatch("a b c", matcher);
    assertCompleteMatch("a b b c", matcher);
    assertCompleteMatch("a b b b b b b b b b b b b b b b b b b b b b b b b b c", matcher);
  }

  @Test
  public void testMaxTwoAndThreeOccurrences() throws Exception {
    final Element elementA = makeElement("a");
    elementA.setMaxOccurrence(2);
    final Element elementB = makeElement("b");
    elementB.setMaxOccurrence(3);
    final PatternRuleMatcher matcher = getMatcher(elementA, elementB);  // regex: a{1,2} b{1,3}
    assertCompleteMatch("a b", matcher);
    assertCompleteMatch("a b b", matcher);
    assertCompleteMatch("a b b b", matcher);
    assertNoMatch("a a", matcher);
    assertNoMatch("a x b b b", matcher);
    final RuleMatch[] matches2 = getMatches("a a b", matcher);
    assertThat(matches2.length , is(1)); // just the longest match
    assertPosition(matches2[0], 0, 5);

    final RuleMatch[] matches3 = getMatches("a a b b", matcher);
    assertThat(matches3.length , is(1));
    assertPosition(matches3[0], 0, 7); // again, only the longest match

    final RuleMatch[] matches4 = getMatches("a a b b b", matcher);
    assertThat(matches4.length , is(1));
    assertPosition(matches4[0], 0, 9);
  }

  @Test
  public void testInfiniteSkip() throws Exception {
    final Element elementA = makeElement("a");
    elementA.setSkipNext(-1);
    final PatternRuleMatcher matcher = getMatcher(elementA, makeElement("b"));
    assertCompleteMatch("a b", matcher);
    assertCompleteMatch("a x b", matcher);
    assertCompleteMatch("a x x b", matcher);
    assertCompleteMatch("a x x x b", matcher);
  }

  @Test
  public void testInfiniteSkipWithMatchReference() throws Exception {
    final Element elementAB = new Element("a|b", false, true, false);
    elementAB.setSkipNext(-1);
    final Element elementC = makeElement("\\0");
    Match match = new Match(null, null, false, null, null, Match.CaseConversion.NONE, false, false, Match.IncludeRange.NONE);
    match.setTokenRef(0);
    match.setInMessageOnly(true);
    elementC.setMatch(match);
    final PatternRuleMatcher matcher = getMatcher(elementAB, elementC);
    assertCompleteMatch("a a", matcher);
    assertCompleteMatch("b b", matcher);
    assertCompleteMatch("a x a", matcher);
    assertCompleteMatch("b x b", matcher);
    assertCompleteMatch("a x x a", matcher);
    assertCompleteMatch("b x x b", matcher);

    assertNoMatch("a b", matcher);
    assertNoMatch("b a", matcher);
    assertNoMatch("b x a", matcher);
    assertNoMatch("b x a", matcher);
    assertNoMatch("a x x b", matcher);
    assertNoMatch("b x x a", matcher);

    final RuleMatch[] matches = getMatches("a foo a and b foo b", matcher);
    assertThat(matches.length , is(2));
    assertPosition(matches[0], 0, 7);
    assertPosition(matches[1], 12, 19);

    final RuleMatch[] matches2 = getMatches("xx a b x x x b a", matcher);
    assertThat(matches2.length , is(1));
    assertPosition(matches2[0], 3, 16);
  }

  private RuleMatch[] getMatches(String input, PatternRuleMatcher matcher) throws IOException {
    return matcher.match(langTool.getAnalyzedSentence(input));
  }

  private PatternRuleMatcher getMatcher(Element... patternElements) {
    return new PatternRuleMatcher(getPatternRule(Arrays.asList(patternElements)), false);
  }

  private void assertPosition(RuleMatch match, int expectedFromPos, int expectedToPos) {
    assertThat("Wrong start position", match.getFromPos(), is(expectedFromPos));
    assertThat("Wrong end position", match.getToPos(), is(expectedToPos));
  }

  private void assertNoMatch(String input, PatternRuleMatcher matcher) throws IOException {
    final RuleMatch[] matches = getMatches(input, matcher);
    assertThat(matches.length , is(0));
  }

  private void assertPartialMatch(String input, PatternRuleMatcher matcher) throws IOException {
    final RuleMatch[] matches = getMatches(input, matcher);
    assertThat(matches.length , is(1));
    assertTrue("Expected partial match, got '" + matches[0] + "' for '" + input + "'",
        matches[0].getFromPos() > 0 || matches[0].getToPos() < input.length());
  }

  private void assertCompleteMatch(String input, PatternRuleMatcher matcher) throws IOException {
    final RuleMatch[] matches = getMatches(input, matcher);
    assertThat("Got matches: " + Arrays.toString(matches), matches.length , is(1));
    assertThat("Wrong start position", matches[0].getFromPos(), is(0));
    assertThat("Wrong end position", matches[0].getToPos(), is(input.length()));
  }

  private Element makeElement(String token) {
    return new Element(token, false, false, false);
  }

  private PatternRule getPatternRule(String pattern) {
    final String[] parts = pattern.split(" ");
    List<Element> elements = new ArrayList<>();
    for (String part : parts) {
      elements.add(new Element(part, false, false, false));
    }
    return getPatternRule(elements);
  }

  private PatternRule getPatternRule(List<Element> elements) {
    return new PatternRule("", new Demo(), elements, "my description", "my message", "short message");
  }
}
