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

import org.apache.commons.lang.StringUtils;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.tools.StringTools;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * A rule that matches words which should not be used and suggests
 * correct ones instead. Loads the relevant words from
 * <code>rules/XX/replace.txt</code>, where XX is a code of the language.
 * 
 * @author Andriy Rysin
 */
public abstract class AbstractSimpleReplaceRule extends Rule {

  private static final String FILE_ENCODING = "utf-8";

  private final Map<String, List<String>> wrongWords;
  
  private boolean ignoreTaggedWords = false;

  public abstract String getFileName();

  public String getEncoding() {
    return FILE_ENCODING;
  }

  /**
   * Indicates if the rule is case-sensitive. Default value is <code>true</code>.
   * 
   * @return true if the rule is case-sensitive, false otherwise.
   */
  public boolean isCaseSensitive() {
    return true;
  }

  /**
   * @return the locale used for case conversion when {@link #isCaseSensitive()}
   *         is set to <code>false</code>.
   */
  public Locale getLocale() {
    return Locale.getDefault();
  }
  
  /**
   * Skip words that are known in the POS tagging dictionary, assuming they
   * cannot be incorrect.
   * @since 2.3
   */
  public void setIgnoreTaggedWords() {
    ignoreTaggedWords = true;
  }

  public AbstractSimpleReplaceRule(final ResourceBundle messages)
      throws IOException {
    if (messages != null) {
      super.setCategory(new Category(messages.getString("category_misc")));
    }
    wrongWords = loadWords(JLanguageTool.getDataBroker()
        .getFromRulesDirAsStream(getFileName()));
  }

  @Override
  public String getId() {
    return "SIMPLE_REPLACE";
  }

  @Override
  public String getDescription() {
    return "Checks for wrong words/phrases";
  }

  public String getMessage(String tokenStr, List<String> replacements) {
    return tokenStr + " is not valid. Use: "
        + StringUtils.join(replacements, ", ") + ".";
  }

  public String getShort() {
    return "Wrong word";
  }

  private String cleanup(String word) {
    if (!isCaseSensitive()) {
      word = word.toLowerCase(getLocale());
    }
    return word;
  }

  @Override
  public final RuleMatch[] match(final AnalyzedSentence text) {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    AnalyzedTokenReadings[] tokens = text.getTokensWithoutWhitespace();

    for (AnalyzedTokenReadings tokenReadings : tokens) {
      String originalTokenStr = tokenReadings.getToken();
      if (ignoreTaggedWords && tokenReadings.isTagged()) {
        continue;
      }
      String tokenString = cleanup(originalTokenStr);

      if (!wrongWords.containsKey(tokenString)) {
        for (AnalyzedToken analyzedToken : tokenReadings.getReadings()) {
          String lemma = analyzedToken.getLemma();
          if (lemma != null) {
            lemma = cleanup(lemma);
            if (wrongWords.containsKey(lemma)) {
              tokenString = lemma;
              break;
            }
          }
        }
      }

      List<String> possibleReplacements = wrongWords.get(tokenString);     

      if (possibleReplacements != null && possibleReplacements.size() > 0) {
        List<String> replacements = new ArrayList<>();  
        replacements.addAll(possibleReplacements);
        if (replacements.contains(originalTokenStr)) {
          replacements.remove(originalTokenStr);
        }
        if (replacements.size() > 0) {
          RuleMatch potentialRuleMatch = createRuleMatch(tokenReadings,
              replacements);
          ruleMatches.add(potentialRuleMatch);
        }
      }
    }
    return toRuleMatchArray(ruleMatches);
  }

  private RuleMatch createRuleMatch(AnalyzedTokenReadings tokenReadings,
      List<String> replacements) {
    String tokenString = tokenReadings.getToken();
    int pos = tokenReadings.getStartPos();

    RuleMatch potentialRuleMatch = new RuleMatch(this, pos, pos
        + tokenString.length(), getMessage(tokenString, replacements), getShort());

    if (!isCaseSensitive() && StringTools.startsWithUppercase(tokenString)) {
      for (int i = 0; i < replacements.size(); i++) {
        replacements
            .set(i, StringTools.uppercaseFirstChar(replacements.get(i)));
      }
    }

    potentialRuleMatch.setSuggestedReplacements(replacements);

    return potentialRuleMatch;
  }

  private Map<String, List<String>> loadWords(final InputStream stream)
      throws IOException {
    Map<String, List<String>> map = new HashMap<>();

    try (Scanner scanner = new Scanner(stream, getEncoding())) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.length() < 1 || line.charAt(0) == '#') { // # = comment
          continue;
        }
        String[] parts = line.split("=");
        if (parts.length != 2) {
          throw new IOException("Format error in file "
                  + JLanguageTool.getDataBroker().getFromRulesDirAsUrl(
                  getFileName()) + ", line: " + line);
        }

        String[] replacements = parts[1].split("\\|");

        // multiple incorrect forms
        final String[] wrongForms = parts[0].split("\\|");
        for (String wrongForm : wrongForms) {
          map.put(wrongForm, Arrays.asList(replacements));
        }
      }
    }
    return map;
  }

  @Override
  public void reset() {
  }

}
