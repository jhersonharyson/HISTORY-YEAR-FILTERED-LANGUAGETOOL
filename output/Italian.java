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
package org.languagetool.language;

import java.util.Arrays;
import java.util.List;

import org.languagetool.Language;
import org.languagetool.rules.CommaWhitespaceRule;
import org.languagetool.rules.DoublePunctuationRule;
import org.languagetool.rules.GenericUnpairedBracketsRule;
import org.languagetool.rules.Rule;
import org.languagetool.rules.UppercaseSentenceStartRule;
import org.languagetool.rules.WhitespaceRule;
// 181 +
//import org.languagetool.rules.WordRepeatRule;
import org.languagetool.rules.it.ItalianWordRepeatRule;
// 181 -
import org.languagetool.rules.it.MorfologikItalianSpellerRule;
// 3607406 +
import org.languagetool.rules.WhitespaceBeforePunctuationRule;
// 3607406 -

import org.languagetool.tagging.Tagger;
import org.languagetool.tagging.it.ItalianTagger;

public class Italian extends Language {

  private Tagger tagger;

  @Override
  public String getName() {
    return "Italian";
  }

  @Override
  public String getShortName() {
    return "it";
  }
  
  @Override
  public String[] getCountries() {
    return new String[]{"IT", "CH"};
  }

  @Override
  public String[] getUnpairedRuleStartSymbols() {
    return new String[]{ "[", "(", "{", "»", "«" /*"‘"*/ };
  }

  @Override
  public String[] getUnpairedRuleEndSymbols() {
    return new String[]{ "]", ")", "}", "«", "»" /*"’"*/ };
  }
  
  @Override
  public Tagger getTagger() {
    if (tagger == null) {
      tagger = new ItalianTagger();
    }
    return tagger;
  }

  @Override
  public Contributor[] getMaintainers() {
    final Contributor contributor = new Contributor("Paolo Bianchini");
    return new Contributor[] { contributor };
  }

  @Override
  public List<Class<? extends Rule>> getRelevantRules() {
    return Arrays.asList(
// 3607406 +
            WhitespaceBeforePunctuationRule.class,
// 3607406 -
            CommaWhitespaceRule.class,
            DoublePunctuationRule.class,
            GenericUnpairedBracketsRule.class,
            MorfologikItalianSpellerRule.class,
            UppercaseSentenceStartRule.class,
// 181 +
//            WordRepeatRule.class,
            ItalianWordRepeatRule.class,
// 181 -
            WhitespaceRule.class
    );
  }

}
