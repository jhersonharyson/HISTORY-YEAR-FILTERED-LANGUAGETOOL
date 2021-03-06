/* LanguageTool, a natural language style checker 
 * Copyright (C) 2018 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.rules.de;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.TestTools;
import org.languagetool.languagemodel.LanguageModel;
import org.languagetool.languagemodel.LuceneLanguageModel;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.ngrams.FakeLanguageModel;

public class ProhibitedCompoundRuleTest {

  private final static Map<String, Integer> map = new HashMap<>();
  static {
    map.put("Mietauto",100);
    map.put("Leerzeile",100);
    map.put("Urberliner",100);
    map.put("Ureinwohner",100);
    map.put("Wohnungsleerstand",50);
    map.put("Xliseihflehrstand",50);
    map.put("Eisensande",100);
    map.put("Eisenstange",101);
  }
  private final ProhibitedCompoundRule rule = new ProhibitedCompoundRule(TestTools.getEnglishMessages(), new FakeLanguageModel(map), null);
  private final JLanguageTool lt = new JLanguageTool(Languages.getLanguageForShortCode("de-DE"));

  @Test
  public void testRule() throws IOException {
    assertMatches("Er ist Uhrberliner.", "Uhrberliner", "Urberliner");
    assertMatches("Er ist Uhr-Berliner.", "Uhr-Berliner", "Urberliner");
    assertMatches("Das ist ein Mitauto.", "Mitauto", "Mietauto");
    assertMatches("Das ist ein Mit-Auto.", "Mit-Auto", "Mietauto");
    assertMatches("Hier leben die Uhreinwohner.", "Uhreinwohner", "Ureinwohner");
    assertMatches("Hier leben die Uhr-Einwohner.", "Uhr-Einwohner", "Ureinwohner");
    assertMatches("Eine Leerzeile einf??gen.", 0);
    assertMatches("Eine Leer-Zeile einf??gen.", 0);
    assertMatches("Eine Lehrzeile einf??gen.", "Lehrzeile", "Leerzeile");
    assertMatches("Eine Lehr-Zeile einf??gen.", "Lehr-Zeile", "Leerzeile");

    assertMatches("Viel Wohnungsleerstand.", 0);
    assertMatches("Viel Wohnungs-Leerstand.", 0);
    assertMatches("Viel Wohnungslehrstand.", "Wohnungslehrstand", "Wohnungsleerstand");
    assertMatches("Viel Wohnungs-Lehrstand.", "Wohnungs-Lehrstand", "Wohnungsleerstand");
    assertMatches("Viel Xliseihfleerstand.", 0);
    assertMatches("Viel Xliseihflehrstand.", 0);  // no correct spelling, so not suggested
    assertMatches("Ein kosmografischer Test", 0);
    assertMatches("Ein Elektrokardiograph", 0);
    assertMatches("Die Elektrokardiographen", 0);

    assertMatches("Den Lehrzeile-Test einf??gen.", "Lehrzeile", "Leerzeile");
    assertMatches("Die Test-Lehrzeile einf??gen.", "Lehrzeile", "Leerzeile");
    assertMatches("Die Versuchs-Test-Lehrzeile einf??gen.", "Lehrzeile", "Leerzeile");
    assertMatches("Den Versuchs-Lehrzeile-Test einf??gen.", "Lehrzeile", "Leerzeile");
  }

  @Test
  public void testMoreThanOneCandidate() throws IOException {
    assertMatches("Die Eisenstande.", "Eisenstande", "Eisenstange");
    Map<String, Integer> map = new HashMap<>();
    map.put("Eisensande", 101);
    map.put("Eisenstange", 100);
    ProhibitedCompoundRule rule = new ProhibitedCompoundRule(TestTools.getEnglishMessages(), new FakeLanguageModel(map), null);
    RuleMatch[] matches = rule.match(lt.getAnalyzedSentence("Die Eisenstande"));
    assertThat(matches[0].getSuggestedReplacements().toString(), is("[Eisensande]"));
  }

  @Test
  public void testRemoveHyphensAndAdaptCase() {
    assertNull(rule.removeHyphensAndAdaptCase("Marathonl??use"));
    assertThat(rule.removeHyphensAndAdaptCase("Marathon-L??use"), is("Marathonl??use"));
    assertThat(rule.removeHyphensAndAdaptCase("Marathon-L??use-Test"), is("Marathonl??usetest"));
    assertThat(rule.removeHyphensAndAdaptCase("Marathon-l??use-test"), is("Marathonl??usetest"));
    assertThat(rule.removeHyphensAndAdaptCase("viele-L??use-Test"), is("vielel??usetest"));
    assertNull(rule.removeHyphensAndAdaptCase("S-Bahn"));
  }

  void assertMatches(String input, int expectedMatches) throws IOException {
    RuleMatch[] matches = rule.match(lt.getAnalyzedSentence(input));
    assertThat("Got matches: " + Arrays.toString(matches), matches.length, is(expectedMatches));
  }

  void assertMatches(String input, String expectedMarkedText, String expectedSuggestions) throws IOException {
    RuleMatch[] matches = rule.match(lt.getAnalyzedSentence(input));
    assertThat("Got matches: " + Arrays.toString(matches), matches.length, is(1));
    String markedText = input.substring(matches[0].getFromPos(), matches[0].getToPos());
    assertThat(markedText, is(expectedMarkedText));
    assertThat(matches[0].getSuggestedReplacements().toString(), is("[" + expectedSuggestions + "]"));
  }

  ProhibitedCompoundRule getRule(String languageModelPath) throws IOException {
    return getRule(languageModelPath, ProhibitedCompoundRule.RULE_ID);
  }

  ProhibitedCompoundRule getRule(String languageModelPath, String ruleId) throws IOException {
    Language lang = Languages.getLanguageForShortCode("de");
    LanguageModel languageModel = new LuceneLanguageModel(new File(languageModelPath, lang.getShortCode()));
    List<Rule> rules = lang.getRelevantLanguageModelRules(JLanguageTool.getMessageBundle(), languageModel, null);
    if (rules == null) {
      throw new RuntimeException("Language " + lang + " doesn't seem to support a language model");
    }
    ProhibitedCompoundRule foundRule = null;
    for (Rule rule : rules) {
      if (rule.getId().equals(ruleId)) {
        foundRule = (ProhibitedCompoundRule) rule;
        break;
      }
    }
    return foundRule;
  }

}
