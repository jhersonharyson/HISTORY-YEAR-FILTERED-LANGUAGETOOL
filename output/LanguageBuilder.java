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

import java.io.File;
import java.util.*;

import org.languagetool.Language;
import org.languagetool.rules.Rule;

/**
 * Create a language by specifying the language's XML rule file.
 * 
 * @author Daniel Naber
 */
public final class LanguageBuilder {

  private LanguageBuilder() {
  }

  public static Language makeAdditionalLanguage(final File file) {
    return makeLanguage(file, true);
  }

  /**
   * Takes an XML file named <tt>rules-xx-language.xml</tt>,
   * e.g. <tt>rules-de-German.xml</tt> and builds
   * a Language object for that language.
   */
  private static Language makeLanguage(final File file, final boolean isAdditional) {
    Objects.requireNonNull(file, "file cannot be null");
    if (!file.getName().endsWith(".xml")) {
      throw new RuleFilenameException(file);
    }
    final String[] parts = file.getName().split("-");
    final boolean startsWithRules = parts[0].equals("rules");
    final boolean secondPartHasCorrectLength = parts.length == 3 &&
            (parts[1].length() == "en".length() || parts[1].length() == "ast".length() || parts[1].length() == "en_US".length());
    if (!startsWithRules || !secondPartHasCorrectLength) {
      throw new RuleFilenameException(file);
    }
    //TODO: when the Language already exists, and the XML file is mergeable with
    // other rules (need to add a parameter for this?), subclass the existing language,
    //and adjust the settings if any are set in the rule file default configuration set
    
    final Language newLanguage = new Language() {
      @Override
      public Locale getLocale() {
        return new Locale(getShortName());
      }
      @Override
      public Contributor[] getMaintainers() {
        return null;
      }
      @Override
      public String getShortName() {
        if (parts[1].length() == 2) {
          return parts[1];
        }
        return  parts[1].split("_")[0]; //en as in en_US
      }
      @Override
      public String[] getCountries() {
        if (parts[1].length() == 2) {
          return new String[] {""};
        }
        return new String[] {parts[1].split("_")[1]}; //US as in en_US
      }
      @Override
      public String getName() {
        return parts[2].replace(".xml", "");
      }
      @Override
      public List<Class<? extends Rule>> getRelevantRules() {
        return Collections.emptyList();
      }
      @Override
      public List<String> getRuleFileNames() {
        final List<String> ruleFiles = new ArrayList<>();
        ruleFiles.add(file.getAbsolutePath());
        return ruleFiles;
      }
      @Override
      public boolean isExternal() {
        return isAdditional;
      }
    };
    return newLanguage;
  }
  
}
