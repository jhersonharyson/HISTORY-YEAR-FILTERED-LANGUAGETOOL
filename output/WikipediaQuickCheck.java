/* LanguageTool, a natural language style checker 
 * Copyright (C) 2011 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.dev.wikipedia;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.MultiThreadedJLanguageTool;
import org.languagetool.language.German;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.tools.ContextTools;
import org.languagetool.tools.StringTools;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Check a Wikipedia page (without spell check), fetching the page via the MediaWiki API.
 */
public class WikipediaQuickCheck {

  private static final Pattern WIKIPEDIA_URL_REGEX = Pattern.compile("https?://(..)\\.wikipedia\\.org/wiki/(.*)"); 
  private static final Pattern SECURE_WIKIPEDIA_URL_REGEX = Pattern.compile("https://secure\\.wikimedia\\.org/wikipedia/(..)/wiki/(.*)");
  private static final int CONTEXT_SIZE = 25;

  private List<String> disabledRuleIds = new ArrayList<>();

  public String getMediaWikiContent(URL wikipediaUrl) throws IOException {
    final Language lang = getLanguage(wikipediaUrl);
    final String pageTitle = getPageTitle(wikipediaUrl);
    final String apiUrl = "http://" + lang.getShortName() + ".wikipedia.org/w/api.php?titles=" 
            + pageTitle + "&action=query&prop=revisions&rvprop=content|timestamp&format=xml";
    return getContent(new URL(apiUrl));
  }

  public Language getLanguage(URL url) {
    final Matcher matcher = getUrlMatcher(url.toString());
    return Language.getLanguageForShortName(matcher.group(1));
  }

  public String getPageTitle(URL url) {
    final Matcher matcher = getUrlMatcher(url.toString());
    return matcher.group(2);
  }
  
  private Matcher getUrlMatcher(String url) {
    final Matcher matcher1 = WIKIPEDIA_URL_REGEX.matcher(url);
    final Matcher matcher2 = SECURE_WIKIPEDIA_URL_REGEX.matcher(url);
    if (matcher1.matches()) {
      return matcher1;
    } else if (matcher2.matches()) {
      return matcher2;
    }
    throw new RuntimeException("URL does not seem to be a valid Wikipedia URL: " + url);
  }

  public void setDisabledRuleIds(List<String> ruleIds) {
    disabledRuleIds = ruleIds;
  }
  
  public List<String> getDisabledRuleIds() {
    return disabledRuleIds;
  }

  public MarkupAwareWikipediaResult checkPage(URL url) throws IOException, PageNotFoundException {
    validateWikipediaUrl(url);
    final WikipediaQuickCheck check = new WikipediaQuickCheck();
    final String xml = check.getMediaWikiContent(url);
    final MediaWikiContent wikiContent = getRevisionContent(xml);
    if (wikiContent.getContent().trim().isEmpty() || wikiContent.getContent().toLowerCase().contains("#redirect")) {
      throw new PageNotFoundException("No content found at " + url);
    }
    return checkWikipediaMarkup(url, wikiContent, getLanguage(url));
  }

  MarkupAwareWikipediaResult checkWikipediaMarkup(URL url, MediaWikiContent wikiContent, Language language) throws IOException {
    final SwebleWikipediaTextFilter filter = new SwebleWikipediaTextFilter();
    final PlainTextMapping mapping = filter.filter(wikiContent.getContent());
    final JLanguageTool langTool = getLanguageTool(language);
    final List<AppliedRuleMatch> appliedMatches = new ArrayList<>();
    final List<RuleMatch> matches = langTool.check(mapping.getPlainText());
    int internalErrors = 0;
    for (RuleMatch match : matches) {
      final SuggestionReplacer replacer = new SuggestionReplacer(mapping, wikiContent.getContent());
      try {
        final List<RuleMatchApplication> ruleMatchApplications = replacer.applySuggestionsToOriginalText(match);
        appliedMatches.add(new AppliedRuleMatch(match, ruleMatchApplications));
      } catch (Exception e) {
        System.err.println("Failed to apply suggestion for rule match '" + match + "' for URL " + url + ": " + e.toString());
        internalErrors++;
      }
    }
    return new MarkupAwareWikipediaResult(wikiContent, appliedMatches, internalErrors);
  }

  public WikipediaQuickCheckResult checkPage(String plainText, Language lang) throws IOException {
    final JLanguageTool langTool = getLanguageTool(lang);
    final List<RuleMatch> ruleMatches = langTool.check(plainText);
    return new WikipediaQuickCheckResult(plainText, ruleMatches, lang.getShortName());
  }

  public void validateWikipediaUrl(URL wikipediaUrl) {
    // will throw exception if URL is not valid:
    getUrlMatcher(wikipediaUrl.toString());
  }

  /**
   * @param completeWikiContent the Mediawiki syntax as it comes from the API, including surrounding XML
   */
  public String getPlainText(String completeWikiContent) {
    final MediaWikiContent wikiContent = getRevisionContent(completeWikiContent);
    final String cleanedWikiContent = removeInterLanguageLinks(wikiContent.getContent());
    final TextMapFilter filter = new SwebleWikipediaTextFilter();
    return filter.filter(cleanedWikiContent).getPlainText();
  }

  /**
   * @param completeWikiContent the Mediawiki syntax as it comes from the API, including surrounding XML
   */
  public PlainTextMapping getPlainTextMapping(String completeWikiContent) {
    final MediaWikiContent wikiContent = getRevisionContent(completeWikiContent);
    final SwebleWikipediaTextFilter filter = new SwebleWikipediaTextFilter();
    return filter.filter(wikiContent.getContent());
  }

  // catches most, not all links ("[[pt:Linux]]", but not "[[zh-min-nan:Linux]]"). Might remove some non-interlanguage links.
  String removeInterLanguageLinks(String wikiContent) {
    return wikiContent.replaceAll("\\[\\[[a-z]{2,6}:.*?\\]\\]", "");
  }

  private MediaWikiContent getRevisionContent(String completeWikiContent) {
    final SAXParserFactory factory = SAXParserFactory.newInstance();
    final SAXParser saxParser;
    final RevisionContentHandler handler = new RevisionContentHandler();
    try {
      saxParser = factory.newSAXParser();
      saxParser.parse(new InputSource(new StringReader(completeWikiContent)), handler);
    } catch (Exception e) {
      throw new RuntimeException("Could not parse XML: " + completeWikiContent, e);
    }
    return new MediaWikiContent(handler.getRevisionContent(), handler.getTimestamp());
  }

  private JLanguageTool getLanguageTool(Language lang) throws IOException {
    final JLanguageTool langTool = new MultiThreadedJLanguageTool(lang);
    langTool.activateDefaultPatternRules();
    enableWikipediaRules(langTool);
    for (String disabledRuleId : disabledRuleIds) {
      langTool.disableRule(disabledRuleId);
    }
    disableSpellingRules(langTool);
    return langTool;
  }

  private void enableWikipediaRules(JLanguageTool langTool) {
    List<Rule> allRules = langTool.getAllRules();
    for (Rule rule : allRules) {
      if (rule.getCategory().getName().equals("Wikipedia")) {
        langTool.enableDefaultOffRule(rule.getId());
      }
    }
  }

  private void disableSpellingRules(JLanguageTool languageTool) {
    final List<Rule> allActiveRules = languageTool.getAllActiveRules();
    for (Rule rule : allActiveRules) {
      if (rule.isSpellingRule()) {
        languageTool.disableRule(rule.getId());
      }
    }
  }

  private String getContent(URL wikipediaUrl) throws IOException {
    final InputStream contentStream = (InputStream) wikipediaUrl.getContent();
    return StringTools.streamToString(contentStream, "UTF-8");
  }

  /*public static void mainTest(String[] args) throws IOException {
      final TextFilter filter = new SwebleWikipediaTextFilter();
      final String plainText = filter.filter("hallo\n* eins\n* zwei");
      System.out.println(plainText);
  }*/
    
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.out.println("Usage: " + WikipediaQuickCheck.class.getName() + " <url>");
      System.exit(1);
    }
    final WikipediaQuickCheck check = new WikipediaQuickCheck();
    // URL examples:
    //final String urlString = "http://de.wikipedia.org/wiki/Angela_Merkel";
    //final String urlString = "https://de.wikipedia.org/wiki/Benutzer_Diskussion:Dnaber";
    //final String urlString = "https://secure.wikimedia.org/wikipedia/de/wiki/G%C3%BCtersloh";
    //final String urlString = "https://secure.wikimedia.org/wikipedia/de/wiki/Benutzer_Diskussion:Dnaber";
    final String urlString = args[0];
    final URL url = new URL(urlString);
    final String mediaWikiContent = check.getMediaWikiContent(url);
    final String plainText = check.getPlainText(mediaWikiContent);
    final WikipediaQuickCheckResult checkResult = check.checkPage(plainText, new German());
    final ContextTools contextTools = new ContextTools();
    contextTools.setContextSize(CONTEXT_SIZE);
    for (RuleMatch ruleMatch : checkResult.getRuleMatches()) {
      System.out.println(ruleMatch.getMessage());
      final String context = contextTools.getPlainTextContext(ruleMatch.getFromPos(), ruleMatch.getToPos(), checkResult.getText());
      System.out.println(context);
    }
  }
  
  class RevisionContentHandler extends DefaultHandler {

    private final StringBuilder revisionText = new StringBuilder();

    private String timestamp;
    private boolean inRevision = false;

    @Override
    public void startElement(final String namespaceURI, final String lName,
        final String qName, final Attributes attrs) throws SAXException {
      if ("rev".equals(qName)) {
        timestamp = attrs.getValue("timestamp");
        inRevision = true;
      }
    }

    @Override
    public void endElement(final String namespaceURI, final String sName,
        final String qName) throws SAXException {
      if ("rev".equals(qName)) {
        inRevision = false;
      }
    }
    
    @Override
    public void characters(final char[] buf, final int offset, final int len) {
      final String s = new String(buf, offset, len);
      if (inRevision) {
        revisionText.append(s);
      }
    }

    public String getRevisionContent() {
      return revisionText.toString();
    }

    public String getTimestamp() {
      return timestamp;
    }
  }

}
