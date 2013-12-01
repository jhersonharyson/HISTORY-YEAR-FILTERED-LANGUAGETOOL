/* LanguageTool, a natural language style checker 
 * Copyright (C) 2009 Marcin Miłkowski (http://www.languagetool.org)
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
package org.languagetool.tools;

import junit.framework.TestCase;
import org.languagetool.JLanguageTool;
import org.languagetool.commandline.CommandLineTools;
import org.languagetool.language.English;
import org.languagetool.language.Polish;
import org.languagetool.rules.bitext.BitextRule;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class ToolsTest extends TestCase {

  private ByteArrayOutputStream out;
  private PrintStream stdout;
  private PrintStream stderr;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.stdout = System.out;
    this.stderr = System.err;
    this.out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();      
    System.setOut(new PrintStream(this.out));
    System.setErr(new PrintStream(err));
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    System.setOut(this.stdout);
    System.setErr(this.stderr);
  }
  
  public void testBitextCheck() throws IOException, ParserConfigurationException, SAXException {
    final English english = new English();
    final JLanguageTool srcTool = new JLanguageTool(english);
    final Polish polish = new Polish();
    final JLanguageTool trgTool = new JLanguageTool(polish);
    trgTool.activateDefaultPatternRules();
    
    final List<BitextRule> rules = Tools.getBitextRules(english, polish);
    
    int matches = CommandLineTools.checkBitext(
        "This is a perfectly good sentence.",
        "To jest całkowicie prawidłowe zdanie.", srcTool, trgTool, rules,
        false, StringTools.XmlPrintMode.NORMAL_XML);
    String output = new String(this.out.toByteArray());
    assertTrue(output.indexOf("Time:") == 0);
    assertEquals(0, matches);

    matches = CommandLineTools.checkBitext(
        "This is not actual.", 
        "To nie jest aktualne.", 
        srcTool, trgTool, 
        rules, false, StringTools.XmlPrintMode.NORMAL_XML);        
    output = new String(this.out.toByteArray());
    assertTrue(output.contains("Rule ID: ACTUAL"));
    assertEquals(1, matches);
  }
}
