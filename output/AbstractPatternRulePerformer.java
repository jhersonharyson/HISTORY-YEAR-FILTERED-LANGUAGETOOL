/* LanguageTool, a natural language style checker
 * Copyright (C) 2012 Daniel Naber (http://www.danielnaber.de)
 * Copyright (C) 2013 Stefan Lotties
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;

/**
 * @since 2.3
 */
public abstract class AbstractPatternRulePerformer {

  protected boolean prevMatched;
  protected AbstractPatternRule rule;
  protected Unifier unifier;
  protected AnalyzedTokenReadings[] unifiedTokens;

  protected AbstractPatternRulePerformer(AbstractPatternRule rule, Unifier unifier) {
    this.rule = rule;
    this.unifier = unifier;
  }

  protected List<ElementMatcher> createElementMatchers() {
    final List<ElementMatcher> elementMatchers = new ArrayList<>(rule.patternElements.size());
    for (Element el : rule.patternElements) {
      final ElementMatcher matcher = new ElementMatcher(el);
      elementMatchers.add(matcher);
    }
    return elementMatchers;
  }

  protected boolean testAllReadings(final AnalyzedTokenReadings[] tokens,
                                    final ElementMatcher elem, final ElementMatcher prevElement,
                                    final int tokenNo, final int firstMatchToken, final int prevSkipNext)
          throws IOException {
    boolean thisMatched = false;
    final int numberOfReadings = tokens[tokenNo].getReadingsLength();
    elem.prepareAndGroup(firstMatchToken, tokens, rule.getLanguage());

    for (int l = 0; l < numberOfReadings; l++) {
      final AnalyzedToken matchToken = tokens[tokenNo]
              .getAnalyzedToken(l);
      prevMatched = prevMatched || prevSkipNext > 0
              && prevElement != null
              && prevElement.isMatchedByScopeNextException(matchToken);
      if (prevMatched) {
        return false;
      }
      thisMatched = thisMatched || elem.isMatched(matchToken);
      if (!thisMatched
              && !elem.getElement().isInflected()
              && elem.getElement().getPOStag() == null
              && (prevElement != null && prevElement.getElement().getExceptionList() == null)) {
        return false; // the token is the same, we will not get a match
      }
      if (rule.isGroupsOrUnification()) {
        thisMatched &= testUnificationAndGroups(thisMatched,
                l + 1 == numberOfReadings, matchToken, elem);        
      }
    }
    if (thisMatched) {
      for (int l = 0; l < numberOfReadings; l++) {
        if (elem.isExceptionMatchedCompletely(tokens[tokenNo].getAnalyzedToken(l)))
          return false;
      }
      if (tokenNo > 0 && elem.hasPreviousException()) {
        if (elem.isMatchedByPreviousException(tokens[tokenNo - 1]))
          return false;
      }
    }
    if (elem.getElement().getChunkTag() != null) {
      thisMatched &= 
          tokens[tokenNo].getChunkTags().contains(elem.getElement().getChunkTag())
          ^ elem.getElement().getNegation();
    }
    if (elem.getElement().hasAndGroup()) {
        for (Element e : elem.getElement().getAndGroup()) {          
            if (e.getChunkTag() != null) {
                thisMatched &= tokens[tokenNo].getChunkTags().contains(e.getChunkTag())
                    ^ e.getNegation();
            }            
        }
    }    
    return thisMatched;
  }

  protected boolean testUnificationAndGroups(final boolean matched,
                                             final boolean lastReading, final AnalyzedToken matchToken,
                                             final ElementMatcher elemMatcher) {
    boolean thisMatched = matched;
    final boolean elemIsMatched = elemMatcher.isMatched(matchToken);
    final Element elem = elemMatcher.getElement();

    if (rule.testUnification) {
      if (matched && elem.isUnified()) {
        if (elem.isUniNegated()) {
          thisMatched = !(thisMatched && unifier.isUnified(
                  matchToken, elem.getUniFeatures(), lastReading,
                  elemIsMatched));
        } else {
          if (elem.isLastInUnification()) {
            thisMatched = thisMatched
                    && unifier.isUnified(matchToken,
                    elem.getUniFeatures(), lastReading,
                    elemIsMatched);
          } else { // we don't care about the truth value, let it run
            unifier.isUnified(matchToken, elem.getUniFeatures(),
                    lastReading, elemIsMatched);
          }

        }
      }
      if (thisMatched && rule.isGetUnified()) {
        unifiedTokens = unifier.getFinalUnified();
      }
      if (!elem.isUnified()) {
        unifier.reset();
      }
    }
    elemMatcher.addMemberAndGroup(matchToken);
    if (lastReading) {
      thisMatched &= elemMatcher.checkAndGroup(thisMatched);
    }    
    return thisMatched;
  }

}
