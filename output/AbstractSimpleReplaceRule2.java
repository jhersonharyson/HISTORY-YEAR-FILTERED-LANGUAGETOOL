/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.rules;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.tools.StringTools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A rule that matches words which should not be used and suggests correct ones instead. 
 * Romanian implementations. Loads the list of words from
 * <code>/ro/replace.txt</code>.
 *
 * <p>Unlike AbstractSimpleReplaceRule, supports multiple words (Ex: "aqua forte" -&gt; "acvaforte").
 *
 * Note: Merge this into {@link AbstractSimpleReplaceRule}
 *
 * @author Ionuț Păduraru
 */
public abstract class AbstractSimpleReplaceRule2 extends Rule {

  private final Language language;

  public abstract String getFileName();
  @Override
  public abstract String getId();
  @Override
  public abstract String getDescription();
  public abstract String getShort();
  public abstract String getSuggestion();
  /**
   * @return the word used to separate multiple suggestions; used only before last suggestion, the rest are comma-separated.  
   */
  public abstract String getSuggestionsSeparator();
  /**
   * locale used on case-conversion
   */
  public abstract Locale getLocale();

  // list of maps containing error-corrections pairs.
  // the n-th map contains key strings of (n+1) words 
  private final List<Map<String, String>> wrongWords;
  
  public AbstractSimpleReplaceRule2(final ResourceBundle messages, Language language) throws IOException {
    super(messages);
    this.language = Objects.requireNonNull(language);
    super.setCategory(new Category(messages.getString("category_misc")));
    wrongWords = loadWords(JLanguageTool.getDataBroker().getFromRulesDirAsStream(getFileName()));
  }

  /**
   * use case-insensitive matching.
   */
  public boolean isCaseSensitive() {
    return false;
  }

  /**
   * @return the list of wrong words for which this rule can suggest correction. The list cannot be modified.
   */
  public List<Map<String, String>> getWrongWords() {
    return wrongWords;
  }

  /**
   * Load the list of words.
   * Same as {@link AbstractSimpleReplaceRule#load} but allows multiple words.   
   * @param stream the stream to load.
   * @return the list of maps containing the error-corrections pairs. The n-th map contains key strings of (n+1) words.
   */
  private List<Map<String, String>> loadWords(final InputStream stream)
          throws IOException {
    final List<Map<String, String>> list = new ArrayList<>();
    try (
      InputStreamReader isr = new InputStreamReader(stream, "utf-8");
      BufferedReader br = new BufferedReader(isr)) 
    {
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.charAt(0) == '#') { // ignore comments
          continue;
        }

        final String[] parts = line.split("=");
        if (parts.length != 2) {
          throw new IOException("Format error in file "
                  + JLanguageTool.getDataBroker().getFromRulesDirAsUrl(getFileName())
                  + ", line: " + line);
        }

        final String[] wrongForms = parts[0].split("\\|"); // multiple incorrect forms
        for (String wrongForm : wrongForms) {
          int wordCount = 0;
          final List<String> tokens = language.getWordTokenizer().tokenize(wrongForm);
          for (String token : tokens) {
            if (!StringTools.isWhitespace(token)) {
              wordCount++;
            }
          }
          // grow if necessary
          for (int i = list.size(); i < wordCount; i++) {
            list.add(new HashMap<String, String>());
          }
          list.get(wordCount - 1).put(wrongForm, parts[1]);
        }
      }
    }
    // seal the result (prevent modification from outside this class)
    final List<Map<String,String>> result = new ArrayList<>();
    for (Map<String, String> map : list) {
      result.add(Collections.unmodifiableMap(map));
    }
    return Collections.unmodifiableList(result);
  }

  private void addToQueue(AnalyzedTokenReadings token,
                          Queue<AnalyzedTokenReadings> prevTokens) {
    final boolean inserted = prevTokens.offer(token);
    if (!inserted) {
      prevTokens.poll();
      prevTokens.offer(token);
    }
  }

  @Override
  public RuleMatch[] match(final AnalyzedSentence sentence) {
    final List<RuleMatch> ruleMatches = new ArrayList<>();
    final AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();

    final Queue<AnalyzedTokenReadings> prevTokens = new ArrayBlockingQueue<>(wrongWords.size());

    for (int i = 1; i < tokens.length; i++) {
      addToQueue(tokens[i], prevTokens);
      final StringBuilder sb = new StringBuilder();
      final List<String> variants = new ArrayList<>();
      final List<AnalyzedTokenReadings> prevTokensList =
              Arrays.asList(prevTokens.toArray(new AnalyzedTokenReadings[prevTokens.size()]));
      for (int j = prevTokensList.size() - 1; j >= 0; j--) {
        if (j != prevTokensList.size() - 1 && prevTokensList.get(j + 1).isWhitespaceBefore()) {
          sb.insert(0, " ");
        }
        sb.insert(0, prevTokensList.get(j).getToken());
        variants.add(0, sb.toString());
      }
      final int len = variants.size(); // prevTokensList and variants have now the same length
      for (int j = 0; j < len; j++) { // longest words first
        final String crt = variants.get(j);
        final int crtWordCount = len - j;
        final String crtMatch = isCaseSensitive() ? wrongWords.get(crtWordCount - 1).get(crt) : wrongWords.get(crtWordCount- 1).get(crt.toLowerCase(getLocale()));
        if (crtMatch != null) {
          final List<String> replacements = Arrays.asList(crtMatch.split("\\|"));
          String msg = crt + getSuggestion();
          for (int k = 0; k < replacements.size(); k++) {
            if (k > 0) {
              msg = msg + (k == replacements.size() - 1 ? getSuggestionsSeparator(): ", ");
            }
            msg += "<suggestion>" + replacements.get(k) + "</suggestion>";
          }
          final int startPos = prevTokensList.get(len - crtWordCount).getStartPos();
          final int endPos = prevTokensList.get(len - 1).getEndPos();
          final RuleMatch potentialRuleMatch = new RuleMatch(this, startPos, endPos, msg, getShort());

          if (!isCaseSensitive() && StringTools.startsWithUppercase(crt)) {
            for (int k = 0; k < replacements.size(); k++) {
              replacements.set(k, StringTools.uppercaseFirstChar(replacements.get(k)));
            }
          }
          potentialRuleMatch.setSuggestedReplacements(replacements);
          ruleMatches.add(potentialRuleMatch);
          break;
        }
      }
    }
    return toRuleMatchArray(ruleMatches);
  }

  @Override
  public void reset() {
  }

}
