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
package org.languagetool.language;

import java.util.Arrays;
import java.util.List;

import org.languagetool.Language;
import org.languagetool.rules.CommaWhitespaceRule;
import org.languagetool.rules.DoublePunctuationRule;
import org.languagetool.rules.Rule;
import org.languagetool.rules.UppercaseSentenceStartRule;
import org.languagetool.rules.WhitespaceRule;
import org.languagetool.rules.be.MorfologikBelarusianSpellerRule;
import org.languagetool.tagging.Tagger;
import org.languagetool.tagging.be.BelarusianTagger;
import org.languagetool.tokenizers.SRXSentenceTokenizer;
import org.languagetool.tokenizers.SentenceTokenizer;

/**
 * Belarusian language declarations.
 * 
 * Copyright (C) 2010 Alex Buloichik (alex73mail@gmail.com)
 */
public class Belarusian extends Language {

    private Tagger tagger;
    private SentenceTokenizer sentenceTokenizer;
    
    @Override
    public String getName() {
        return "Belarusian";
    }

    @Override
    public String getShortName() {
        return "be";
    }

    @Override
    public String[] getCountries() {
        return new String[]{"BY"};
    }

    @Override
    public Tagger getTagger() {
        if (tagger == null) {
            tagger = new BelarusianTagger();
        }
        return tagger;
    }

    
      @Override
    public SentenceTokenizer getSentenceTokenizer() {
    if (sentenceTokenizer == null) {
       sentenceTokenizer = new SRXSentenceTokenizer(this);
    }
    return sentenceTokenizer;
  }
    
    @Override
    public Contributor[] getMaintainers() {
        return new Contributor[] { new Contributor("Alex Buloichik") };
    }

    @Override
    public List<Class<? extends Rule>> getRelevantRules() {
      return Arrays.asList(
              CommaWhitespaceRule.class,
              DoublePunctuationRule.class,
              MorfologikBelarusianSpellerRule.class,
              UppercaseSentenceStartRule.class,
              WhitespaceRule.class
      );
    }

}
