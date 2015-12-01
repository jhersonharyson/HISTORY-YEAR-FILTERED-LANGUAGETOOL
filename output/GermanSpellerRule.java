/* LanguageTool, a natural language style checker 
 * Copyright (C) 2012 Daniel Naber (http://www.danielnaber.de)
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

import de.danielnaber.jwordsplitter.GermanWordSplitter;
import org.jetbrains.annotations.Nullable;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.German;
import org.languagetool.rules.Example;
import org.languagetool.rules.spelling.hunspell.CompoundAwareHunspellRule;
import org.languagetool.rules.spelling.morfologik.MorfologikMultiSpeller;
import org.languagetool.synthesis.Synthesizer;
import org.languagetool.tagging.Tagger;
import org.languagetool.tokenizers.de.GermanCompoundTokenizer;
import org.languagetool.tools.StringTools;

import java.io.*;
import java.util.*;

public class GermanSpellerRule extends CompoundAwareHunspellRule {

  public static final String RULE_ID = "GERMAN_SPELLER_RULE";
  
  private static final int MAX_EDIT_DISTANCE = 2;
  private static final int SUGGESTION_MIN_LENGTH = 2;
  private static final List<Replacement> REPL = Arrays.asList(
      // see de_DE.aff:
      new Replacement("f", "ph"),
      new Replacement("ph", "f"),
      new Replacement("ß", "ss"),
      new Replacement("ss", "ß"),
      new Replacement("s", "ss"),
      new Replacement("ss", "s"),
      new Replacement("i", "ie"),
      new Replacement("ie", "i"),
      new Replacement("ee", "e"),
      new Replacement("o", "oh"),
      new Replacement("oh", "o"),
      new Replacement("a", "ah"),
      new Replacement("ah", "a"),
      new Replacement("e", "eh"),
      new Replacement("eh", "e"),
      new Replacement("ae", "ä"),
      new Replacement("oe", "ö"),
      new Replacement("ue", "ü"),
      new Replacement("Ae", "Ä"),
      new Replacement("Oe", "Ö"),
      new Replacement("Ue", "Ü"),
      new Replacement("d", "t"),
      new Replacement("t", "d"),
      new Replacement("th", "t"),
      new Replacement("t", "th"),
      new Replacement("r", "rh"),
      new Replacement("ch", "k"),
      new Replacement("k", "ch"),
      // not in de_DE.aff (not clear what uppercase replacement we need...):
      new Replacement("F", "Ph"),
      new Replacement("Ph", "F")
  );
  
  private final LineExpander lineExpander = new LineExpander();
  private final GermanCompoundTokenizer compoundTokenizer;
  private final GermanWordSplitter splitter;
  private final Synthesizer synthesizer;
  private final Tagger tagger;

  public GermanSpellerRule(ResourceBundle messages, German language) {
    super(messages, language, language.getNonStrictCompoundSplitter(), getSpeller(language));
    addExamplePair(Example.wrong("LanguageTool kann mehr als eine <marker>nromale</marker> Rechtschreibprüfung."),
                   Example.fixed("LanguageTool kann mehr als eine <marker>normale</marker> Rechtschreibprüfung."));
    compoundTokenizer = language.getStrictCompoundTokenizer();
    tagger = language.getTagger();
    synthesizer = language.getSynthesizer();
    try {
      splitter = new GermanWordSplitter(false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getId() {
    return RULE_ID;
  }

  @Override
  public List<String> getCandidates(String word) {
    List<String> suggestions = new ArrayList<>();
    List<List<String>> partList = splitter.getAllSplits(word);
    final List<String> candidates = new ArrayList<>();
    for (List<String> parts : partList) {
      candidates.addAll(super.getCandidates(parts));
    }
    suggestions.addAll(candidates);
    return suggestions;
  }

  @Override
  protected void addIgnoreWords(String origLine, Set<String> wordsToBeIgnored) {
    String line;
    if (language.getShortNameWithCountryAndVariant().equals("de-CH")) {
      // hack: Swiss German doesn't use "ß" but always "ss" - replace this, otherwise
      // misspellings (from Swiss point-of-view) like "äußere" wouldn't be found:
      line = origLine.replace("ß", "ss");
    } else {
      line = origLine;
    }
    wordsToBeIgnored.addAll(expandLine(line));
  }

  @Override
  protected List<String> expandLine(String line) {
    return lineExpander.expandLine(line);
  }

  @Nullable
  private static MorfologikMultiSpeller getSpeller(Language language) {
    if (!language.getShortName().equals(Locale.GERMAN.getLanguage())) {
      throw new RuntimeException("Language is not a variant of German: " + language);
    }
    try {
      final String morfoFile = "/de/hunspell/de_" + language.getCountries()[0] + ".dict";
      if (JLanguageTool.getDataBroker().resourceExists(morfoFile)) {
        // spell data will not exist in LibreOffice/OpenOffice context
        try (InputStream stream = JLanguageTool.getDataBroker().getFromResourceDirAsStream("/de/hunspell/spelling.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"))) {
          return new MorfologikMultiSpeller(morfoFile, new ExpandingReader(br), MAX_EDIT_DISTANCE);
        }
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not set up morfologik spell checker", e);
    }
  }

  @Override
  protected void filterForLanguage(List<String> suggestions) {
    if (language.getShortNameWithCountryAndVariant().equals("de-CH")) {
      for (int i = 0; i < suggestions.size(); i++) {
        String s = suggestions.get(i);
        suggestions.set(i, s.replace("ß", "ss"));
      }
    }
  }

  // Use hunspell-style replacements to get good suggestions for "heisse", namely "heiße" etc
  // TODO: remove this when the Morfologik speller can do this directly during tree iteration:
  @Override
  protected List<String> sortSuggestionByQuality(String misspelling, List<String> suggestions) {
    List<String> sorted1 = sortByReplacements(misspelling, suggestions);
    List<String> sorted2 = sortByCase(misspelling, sorted1);
    return sorted2;
  }

  @Override
  protected boolean ignoreWord(List<String> words, int idx) throws IOException {
    boolean ignore = super.ignoreWord(words, idx);
    boolean ignoreByHyphen = !ignore && words.get(idx).endsWith("-") && ignoreByHangingHyphen(words, idx);
    return ignore || ignoreByHyphen;
  }

  @Override
  protected List<String> getAdditionalTopSuggestions(List<String> suggestions, String word) {
    String w = word.replaceFirst("\\.$", "");
    if ("unzwar".equals(w)) {
      return Collections.singletonList("und zwar");
    } else if ("desweiteren".equals(w)) {
      return Collections.singletonList("des Weiteren");
    } else if ("wieviel".equals(w)) {
      return Collections.singletonList("wie viel");
    } else if ("wieviele".equals(w)) {
      return Collections.singletonList("wie viele");
    } else if ("wievielen".equals(w)) {
      return Collections.singletonList("wie vielen");
    } else if ("vorteilen".equals(w)) {
      return Collections.singletonList("Vorteilen");
    } else if ("Trons".equals(w)) {
      return Collections.singletonList("Trance");
    } else if ("einzigste".equals(w)) {
      return Collections.singletonList("einzige");
    } else if (word.endsWith("standart")) {
      return Collections.singletonList(word.replaceFirst("standart$", "standard"));
    } else if (word.endsWith("standarts")) {
      return Collections.singletonList(word.replaceFirst("standarts$", "standards"));
    } else if (word.equals("Rolladen")) {
      return Collections.singletonList("Rollladen");
    } else if (word.equals("Maßname")) {
      return Collections.singletonList("Maßnahme");
    } else if (word.equals("Maßnamen")) {
      return Collections.singletonList("Maßnahmen");
    } else if (word.equals("nanten")) {
      return Collections.singletonList("nannten");
    } else if (!StringTools.startsWithUppercase(word)) {
      String ucWord = StringTools.uppercaseFirstChar(word);
      if (!suggestions.contains(ucWord) && !hunspellDict.misspelled(ucWord)) {
        // Hunspell doesn't always automatically offer the most obvious suggestion for compounds:
        return Collections.singletonList(ucWord);
      }
    }
    String verbSuggestion = getPastTenseVerbSuggestion(word);
    if (verbSuggestion != null) {
      return Collections.singletonList(verbSuggestion);
    }
    String participleSuggestion = getParticipleSuggestion(word);
    if (participleSuggestion != null) {
      return Collections.singletonList(participleSuggestion);
    }
    return Collections.emptyList();
  }

  // Get a correct suggestion for invalid words like greifte, denkte, gehte: useful for
  // non-native speakers and cannot be found by just looking for similar words.
  @Nullable
  private String getPastTenseVerbSuggestion(String word) {
    if (word.endsWith("e")) {
      String wordStem = word.replaceFirst("e$", "");
      try {
        String lemma = baseForThirdPersonSingularVerb(wordStem);
        if (lemma != null) {
          AnalyzedToken token = new AnalyzedToken(lemma, null, lemma);
          String[] forms = synthesizer.synthesize(token, "VER:3:SIN:PRT:.*", true);
          if (forms.length > 0) {
            return forms[0];
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  @Nullable
  private String baseForThirdPersonSingularVerb(String word) throws IOException {
    List<AnalyzedTokenReadings> readings = tagger.tag(Collections.singletonList(word));
    for (AnalyzedTokenReadings reading : readings) {
      if (reading.hasPartialPosTag("VER:3:SIN:")) {
        return reading.getReadings().get(0).getLemma();
      }
    }
    return null;
  }

  // Get a correct suggestion for invalid words like geschwimmt, geruft: useful for
  // non-native speakers and cannot be found by just looking for similar words.
  @Nullable
  private String getParticipleSuggestion(String word) {
    if (word.startsWith("ge") && word.endsWith("t")) {
      String baseform = word.replaceFirst("^ge", "").replaceFirst("t$", "en");
      try {
        String participle = getParticipleForBaseform(baseform);
        if (participle != null) {
          return participle;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  @Nullable
  private String getParticipleForBaseform(String baseform) throws IOException {
    AnalyzedToken token = new AnalyzedToken(baseform, null, baseform);
    String[] forms = synthesizer.synthesize(token, "VER:PA2:.*", true);
    if (forms.length > 0 && !hunspellDict.misspelled(forms[0])) {
      return forms[0];
    }
    return null;
  }

  private boolean ignoreByHangingHyphen(List<String> words, int idx) {
    String word = words.get(idx);
    String nextWord = getWordAfterEnumerationOrNull(words, idx);
    boolean isCompound = nextWord != null && compoundTokenizer.tokenize(nextWord).size() > 1;
    if (isCompound) {
      return !hunspellDict.misspelled(word.replaceFirst("-$", ""));  // "Stil- und Grammatikprüfung" or "Stil-, Text- und Grammatikprüfung"
    }
    return false;
  }

  // for "Stil- und Grammatikprüfung", get "Grammatikprüfung" when at position of "Stil-"
  @Nullable
  private String getWordAfterEnumerationOrNull(List<String> words, int idx) {
    for (int i = idx; i < words.size(); i++) {
      String word = words.get(i);
      boolean inEnumeration = ",".equals(word) || "und".equals(word) || "oder".equals(word) || word.trim().isEmpty() || word.endsWith("-");
      if (!inEnumeration) {
        return word;
      }
    }
    return null;
  }

  private List<String> sortByReplacements(String misspelling, List<String> suggestions) {
    final List<String> result = new ArrayList<>();
    for (String suggestion : suggestions) {
      boolean moveSuggestionToTop = false;
      for (Replacement replacement : REPL) {
        final String modifiedMisspelling = misspelling.replace(replacement.key, replacement.value);
        final boolean equalsAfterReplacement = modifiedMisspelling.equals(suggestion);
        if (equalsAfterReplacement) {
          moveSuggestionToTop = true;
          break;
        }
      }
      if (!ignoreSuggestion(suggestion)) {
        if (moveSuggestionToTop) {
          // this should be preferred, as the replacements make it equal to the suggestion:
          result.add(0, suggestion);
        } else {
          result.add(suggestion);
        }
      }
    }
    return result;
  }

  private List<String> sortByCase(String misspelling, List<String> suggestions) {
    final List<String> result = new ArrayList<>();
    for (String suggestion : suggestions) {
      if (misspelling.equalsIgnoreCase(suggestion)) {
        // this should be preferred - only case differs:
        result.add(0, suggestion);
      } else {
        result.add(suggestion);
      }
    }
    return result;
  }

  private boolean ignoreSuggestion(String suggestion) {
    String[] parts = suggestion.split(" ");
    if (parts.length > 1) {
      for (String part : parts) {
        if (part.length() < SUGGESTION_MIN_LENGTH) {
          return true;
        }
      }
    }
    return false;
  }

  private static class Replacement {
    final String key;
    final String value;
    private Replacement(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  static class ExpandingReader extends BufferedReader {

    private final List<String> buffer = new ArrayList<>();
    private final LineExpander lineExpander = new LineExpander();

    ExpandingReader(Reader in) {
      super(in);
    }

    @Override
    public String readLine() throws IOException {
      if (buffer.size() > 0) {
        return buffer.remove(0);
      } else {
        String line = super.readLine();
        if (line == null) {
          return null;
        }
        buffer.addAll(lineExpander.expandLine(line));
        return buffer.remove(0);
      }
    }
  }

}
