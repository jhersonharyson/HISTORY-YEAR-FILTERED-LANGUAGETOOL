/* LanguageTool, a natural language style checker 
 * Copyright (C) 2007 Daniel Naber (http://www.danielnaber.de)
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

package org.languagetool.tagging.disambiguation.rules;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.languagetool.AnalyzedSentence;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.tagging.disambiguation.Disambiguator;
import org.languagetool.tools.Tools;
import org.xml.sax.SAXException;

/**
 * Rule-based disambiguator.
 * Implements an idea by Agnes Souque.
 * 
 * @author Marcin Miłkowski
 */
public class XmlRuleDisambiguator implements Disambiguator {

  private static final String DISAMBIGUATION_FILE = "disambiguation.xml";
  
  private List<DisambiguationPatternRule> disambiguationRules;
  private final Language language;

  public XmlRuleDisambiguator(final Language language) {
    this.language = Objects.requireNonNull(language);
  }

  @Override
  public AnalyzedSentence disambiguate(final AnalyzedSentence input) throws IOException {
    AnalyzedSentence sentence = input;
    if (disambiguationRules == null) {
      final String disambiguationFile =
        JLanguageTool.getDataBroker().getResourceDir() + "/" + language.getShortName() + "/" + DISAMBIGUATION_FILE;
      try {
        disambiguationRules = loadPatternRules(disambiguationFile);
      } catch (final Exception e) {
        throw new RuntimeException("Problems with loading disambiguation file: " + disambiguationFile, e);
      }
    }
    for (final DisambiguationPatternRule patternRule : disambiguationRules) {
      sentence = patternRule.replace(sentence);
    }
    return sentence;
  }

  /**
   * Load disambiguation rules from an XML file. Use {@link org.languagetool.JLanguageTool#addRule} to add
   * these rules to the checking process.
   * 
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   * @return a List of {@link DisambiguationPatternRule} objects
   */
  protected List<DisambiguationPatternRule> loadPatternRules(final String filename) throws ParserConfigurationException, SAXException, IOException {
    final DisambiguationRuleLoader ruleLoader = new DisambiguationRuleLoader();
    return ruleLoader.getRules(Tools.getStream(filename));
  }

}
