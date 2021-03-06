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
package org.languagetool.openoffice;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.UIManager;

import org.jetbrains.annotations.Nullable;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.UserConfig;
import org.languagetool.gui.AboutDialog;
import org.languagetool.gui.Configuration;
import org.languagetool.openoffice.SpellAndGrammarCheckDialog.LtCheckDialog;
import org.languagetool.rules.CategoryId;
import org.languagetool.rules.Rule;
import org.languagetool.rules.TextLevelRule;
import org.languagetool.tools.Tools;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.Locale;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.linguistic2.LinguServiceEvent;
import com.sun.star.linguistic2.LinguServiceEventFlags;
import com.sun.star.linguistic2.ProofreadingResult;
import com.sun.star.linguistic2.XLinguServiceEventListener;
import com.sun.star.linguistic2.XProofreader;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * Class to handle multiple LO documents for checking
 * @since 4.3
 * @author Fred Kruse, Marcin Mi??kowski
 */
public class MultiDocumentsHandler {

  private static final String CONFIG_FILE = "Languagetool.cfg";
  private static final String OLD_CONFIG_FILE = ".languagetool-ooo.cfg";
  private static final String LOG_FILE = "LanguageTool.log";
  // LibreOffice (since 4.2.0) special tag for locale with variant 
  // e.g. language ="qlt" country="ES" variant="ca-ES-valencia":
  private static final String LIBREOFFICE_SPECIAL_LANGUAGE_TAG = "qlt";
  
  private static final ResourceBundle messages = JLanguageTool.getMessageBundle();

  private final List<XLinguServiceEventListener> xEventListeners;

  private boolean docReset = false;

  private static boolean debugMode = false;   //  should be false except for testing
  
  private SwJLanguageTool langTool = null;
  private Language docLanguage = null;
  private Language fixedLanguage = null;
  private Language langForShortName;
  private Locale locale;
  private final XEventListener xEventListener;
  private final XProofreader xProofreader;
  private final File configDir;
  private final String configFile;
  private final File oldConfigFile;
  private Configuration config = null;
  private LinguisticServices linguServices = null;
  private SortedTextRules sortedTextRules;
  private Set<String> disabledRulesUI;        //  Rules disabled by context menu or spell dialog
  private final List<Rule> extraRemoteRules;  //  store of rules supported by remote server but not locally
  private LtDictionary dictionary;            //  internal dictionary of LT defined words 
  private LtCheckDialog ltDialog = null;      //  LT spelling and grammar check dialog
  private boolean dialogIsRunning = false;    //  The dialog was started     
  
  private XComponentContext xContext;         //  The context of the document
  private List<SingleDocument> documents;     //  The List of LO documents to be checked
  private XComponent goneContext = null;      //  save component of closed document
  private boolean recheck = true;             //  if true: recheck the whole document at next iteration
  private int docNum;                         //  number of the current document

  private boolean noBackgroundCheck = false;  //  is LT switched off by config
  private boolean useQueue = true;            //  will be overwritten by config

  private String menuDocId = null;            //  Id of document at which context menu was called 
  private TextLevelCheckQueue textLevelQueue = null; // Queue to check text level rules
  
  private boolean testMode = false;


  MultiDocumentsHandler(XComponentContext xContext, XProofreader xProofreader, XEventListener xEventListener) {
    this.xContext = xContext;
    this.xEventListener = xEventListener;
    this.xProofreader = xProofreader;
    xEventListeners = new ArrayList<>();
    configFile = CONFIG_FILE;
    File homeDir = getHomeDir();
    configDir = OfficeTools.getLOConfigDir();
    String configDirName = configDir == null ? "." : configDir.toString();
    oldConfigFile = homeDir == null ? null : new File(homeDir, OLD_CONFIG_FILE);
    MessageHandler.init(configDirName, LOG_FILE);
    documents = new ArrayList<>();
    disabledRulesUI = new HashSet<>();
    extraRemoteRules = new ArrayList<>();
    dictionary = new LtDictionary();
  }
  
  /**
   * Runs the grammar checker on paragraph text.
   *
   * @param docID document ID
   * @param paraText paragraph text
   * @param locale Locale the text Locale
   * @param startOfSentencePos start of sentence position
   * @param nSuggestedBehindEndOfSentencePosition end of sentence position
   * @return ProofreadingResult containing the results of the check.
   */
  public final ProofreadingResult doProofreading(String docID,
      String paraText, Locale locale, int startOfSentencePos,
      int nSuggestedBehindEndOfSentencePosition,
      PropertyValue[] propertyValues) {
    ProofreadingResult paRes = new ProofreadingResult();
    paRes.nStartOfSentencePosition = startOfSentencePos;
    paRes.nStartOfNextSentencePosition = nSuggestedBehindEndOfSentencePosition;
    paRes.nBehindEndOfSentencePosition = paRes.nStartOfNextSentencePosition;
    paRes.xProofreader = xProofreader;
    paRes.aLocale = locale;
    paRes.aDocumentIdentifier = docID;
    paRes.aText = paraText;
    paRes.aProperties = propertyValues;
    try {
      paRes = getCheckResults(paraText, locale, paRes, propertyValues, docReset);
      docReset = false;
    } catch (Throwable t) {
      MessageHandler.showError(t);
    }
    return paRes;
  }

  /**
   * distribute the check request to the concerned document
   */
  ProofreadingResult getCheckResults(String paraText, Locale locale, ProofreadingResult paRes, 
      PropertyValue[] propertyValues, boolean docReset) {
    
    if (!hasLocale(locale)) {
      return paRes;
    }
    if (!noBackgroundCheck) {
      boolean isSameLanguage = true;
      if (fixedLanguage == null || langForShortName == null) {
        langForShortName = getLanguage(locale);
        isSameLanguage = langForShortName.equals(docLanguage) && langTool != null;
      }
      if (!isSameLanguage || recheck) {
        boolean initDocs = langTool == null || recheck;
        if (!isSameLanguage) {
          docLanguage = langForShortName;
          this.locale = locale;
          extraRemoteRules.clear();
        }
        langTool = initLanguageTool(!isSameLanguage);
        initCheck(langTool, locale);
        if (initDocs) {
          initDocuments();
        }
      }
    }
    docNum = getNumDoc(paRes.aDocumentIdentifier);
    if (noBackgroundCheck) {
      return paRes;
    }
    paRes = documents.get(docNum).getCheckResults(paraText, locale, paRes, propertyValues, docReset, langTool);
    if (langTool.doReset()) {
      // langTool.doReset() == true: if server connection is broken ==> switch to internal check
      MessageHandler.showMessage(messages.getString("loRemoteSwitchToLocal"));
      config.setRemoteCheck(false);
      try {
        config.saveConfiguration(docLanguage);
      } catch (IOException e) {
        MessageHandler.showError(e);
      }
      resetDocument();
    }
    return paRes;
  }

  /**
   *  Get the current used document
   */
  public SingleDocument getCurrentDocument() {
    XComponent xComponent = OfficeTools.getCurrentComponent(xContext);
    for (SingleDocument document : documents) {
      if (xComponent.equals(document.getXComponent())) {
        return document;
      }
    }
    return null;
  }
  
  /**
   *  Set all documents to be checked again
   */
  void setRecheck() {
    recheck = true;
  }
  
  /**
   *  Set XComponentContext
   */
  void setComponentContext(XComponentContext xContext) {
    this.xContext = xContext;
    setRecheck();
  }
  
  /**
   *  Set pointer to LT spell and grammar check dialog
   */
  public void setLtDialog(LtCheckDialog dialog) {
    ltDialog = dialog;
  }
  
  /**
   *  Set Information LT spell and grammar check dialog was started
   */
  public void setLtDialogIsRunning(boolean running) {
    this.dialogIsRunning = running;
  }
  
  /**
   *  Set a document as closed
   */
  private void setContextOfClosedDoc(XComponent context) {
    goneContext = context;
    boolean found = false;
    for (SingleDocument document : documents) {
      if (context.equals(document.getXComponent())) {
        found = true;
        document.dispose();
        if (config.saveLoCache()) {
          document.writeCaches();
        }
        if (useQueue && textLevelQueue != null) {
          MessageHandler.printToLogFile("Interrupt text level queue for document " + document.getDocID());
          textLevelQueue.interruptCheck(document.getDocID());
          MessageHandler.printToLogFile("Interrupt done");
        }
      }
    }
    if (!found) {
      MessageHandler.printToLogFile("Error: Disposed Document not found - Cache not deleted");
    }
  }
  
  /**
   *  Add a rule to disabled rules by context menu or spell dialog
   */
  void addDisabledRule(String ruleId) {
    disabledRulesUI.add(ruleId);
  }
  
  /**
   *  Remove a rule from disabled rules by spell dialog
   */
  void removeDisabledRule(String ruleId) {
    disabledRulesUI.remove(ruleId);
  }
  
  /**
   *  remove all disabled rules by context menu or spell dialog
   */
  void resetDisabledRules() {
    disabledRulesUI = new HashSet<>();
  }
  
  /**
   *  get all disabled rules by context menu or spell dialog
   */
  Set<String> getDisabledRules() {
    return disabledRulesUI;
  }
  
  /**
   *  set disabled rules by context menu or spell dialog
   */
  void setDisabledRules(Set<String> ruleIds) {
    disabledRulesUI = new HashSet<>(ruleIds);
  }
  
  /**
   *  get LanguageTool
   */
  SwJLanguageTool getLanguageTool() {
    if (langTool == null) {
      if (docLanguage == null) {
        docLanguage = getLanguage();
      }
      langTool = initLanguageTool();
    }
    return langTool;
  }
  
  /**
   *  get Configuration
   */
  Configuration getConfiguration() {
    try {
      if (config == null || recheck) {
        if (docLanguage == null) {
          docLanguage = getLanguage();
        }
        initLanguageTool();
      }
    } catch (Throwable t) {
      MessageHandler.showError(t);
    }
    return config;
  }
  
  /**
   * Allow xContext == null for test cases
   */
  void setTestMode(boolean mode) {
    testMode = mode;
    MessageHandler.setTestMode(mode);
  }

  /**
   * proofs if test cases
   */
  boolean isTestMode() {
    return testMode;
  }

  /**
   * Checks the language under the cursor. Used for opening the configuration dialog.
   * @return the language under the visible cursor
   */
  public Language getLanguage() {
    Locale locale = getDocumentLocale();
    if (locale == null) {
      locale = new Locale("en","US","");
    }
    if (!hasLocale(locale)) {
      String message = Tools.i18n(messages, "language_not_supported", locale.Language);
      MessageHandler.showMessage(message);
      return null;
    }
    return getLanguage(locale);
  }
  
  /**
   * Checks the language under the cursor. Used for opening the configuration dialog.
   * @return the locale under the visible cursor
   */
  @Nullable
  public Locale getDocumentLocale() {
    if (xContext == null) {
      return null;
    }
    XComponent xComponent = OfficeTools.getCurrentComponent(xContext);
    if (xComponent == null) {
      return null;
    }
    Locale charLocale;
    XPropertySet xCursorProps;
    try {
      XModel model = UnoRuntime.queryInterface(XModel.class, xComponent);
      if (model == null) {
        return null;
      }
      XTextViewCursorSupplier xViewCursorSupplier =
          UnoRuntime.queryInterface(XTextViewCursorSupplier.class, model.getCurrentController());
      if (xViewCursorSupplier == null) {
        return null;
      }
      XTextViewCursor xCursor = xViewCursorSupplier.getViewCursor();
      if (xCursor == null) {
        return null;
      }
      if (xCursor.isCollapsed()) { // no text selection
        xCursorProps = UnoRuntime.queryInterface(XPropertySet.class, xCursor);
      } else { // text is selected, need to create another cursor
        // as multiple languages can occur here - we care only
        // about character under the cursor, which might be wrong
        // but it applies only to the checking dialog to be removed
        xCursorProps = UnoRuntime.queryInterface(
            XPropertySet.class,
            xCursor.getText().createTextCursorByRange(xCursor.getStart()));
      }

      // The CharLocale and CharLocaleComplex properties may both be set, so we still cannot know
      // whether the text is e.g. Khmer or Tamil (the only "complex text layout (CTL)" languages we support so far).
      // Thus we check the text itself:
      if (new KhmerDetector().isThisLanguage(xCursor.getText().getString())) {
        return new Locale("km", "", "");
      }
      if (new TamilDetector().isThisLanguage(xCursor.getText().getString())) {
        return new Locale("ta","","");
      }
      if (xCursorProps == null) {
        return null;
      }
      Object obj = xCursorProps.getPropertyValue("CharLocale");
      if (obj == null) {
        return null;
      }
      charLocale = (Locale) obj;
    } catch (Throwable t) {
      MessageHandler.showError(t);
      return null;
    }
    return charLocale;
  }

  /**
   * @return true if LT supports the language of a given locale
   * @param locale The Locale to check
   */
  final boolean hasLocale(Locale locale) {
    try {
      for (Language element : Languages.get()) {
        if (locale.Language.equalsIgnoreCase(LIBREOFFICE_SPECIAL_LANGUAGE_TAG)
            && element.getShortCodeWithCountryAndVariant().equals(locale.Variant)) {
          return true;
        }
        if (element.getShortCode().equals(locale.Language)) {
          return true;
        }
      }
    } catch (Throwable t) {
      MessageHandler.showError(t);
    }
    return false;
  }

  /**
   *  Set configuration Values for all documents
   */
  private void setConfigValues(Configuration config, SwJLanguageTool langTool) {
    this.config = config;
    this.langTool = langTool;
    if (textLevelQueue != null && config.getNumParasToCheck() == 0) {
      textLevelQueue.setStop();
      textLevelQueue = null;
    }
    useQueue = noBackgroundCheck || testMode || config.getNumParasToCheck() == 0 ? false : config.useTextLevelQueue();
    for (SingleDocument document : documents) {
      document.setConfigValues(config);
    }
  }

  /**
   * Get language from locale
   */
  public Language getLanguage(Locale locale) {
    try {
      if (locale.Language.equalsIgnoreCase(LIBREOFFICE_SPECIAL_LANGUAGE_TAG)) {
        return Languages.getLanguageForShortCode(locale.Variant);
      } else {
        return Languages.getLanguageForShortCode(locale.Language + "-" + locale.Country);
      }
    } catch (IllegalArgumentException e) {
      return Languages.getLanguageForShortCode(locale.Language);
    }
  }

  /**
   * Get or Create a Number from docID
   * Return -1 if failed
   */
  private int getNumDoc(String docID) {
    if (goneContext != null ) {
      removeDoc(docID);
    }
    for (int i = 0; i < documents.size(); i++) {
      if (documents.get(i).getDocID().equals(docID)) {  //  document exist
        if (!testMode && documents.get(i).getXComponent() == null) {
          XComponent xComponent = OfficeTools.getCurrentComponent(xContext);
          if (xComponent == null) {
            MessageHandler.printToLogFile("Error: Document (ID: " + docID + ") has no XComponent -> Internal space will not be deleted when document disposes");
          } else {
            try {
              xComponent.addEventListener(xEventListener);
            } catch (Throwable t) {
              MessageHandler.printToLogFile("Error: Document (ID: " + docID + ") has no XComponent -> Internal space will not be deleted when document disposes");
              xComponent = null;
            }
            if (xComponent != null) {
              documents.get(i).setXComponent(xContext, xComponent);
              MessageHandler.printToLogFile("Fixed: XComponent set for Document (ID: " + docID + ")");
            }
          }
        }
        return i;
      }
    }
    //  Add new document
    XComponent xComponent = null;
    if (!testMode) {              //  xComponent == null for test cases 
      xComponent = OfficeTools.getCurrentComponent(xContext);
      if (xComponent == null) {
        MessageHandler.printToLogFile("Error: Document (ID: " + docID + ") has no XComponent -> Internal space will not be deleted when document disposes");
      } else {
        try {
          xComponent.addEventListener(xEventListener);
        } catch (Throwable t) {
          MessageHandler.printToLogFile("Error: Document (ID: " + docID + ") has no XComponent -> Internal space will not be deleted when document disposes");
          xComponent = null;
        }
      }
    }
    SingleDocument newDocument = new SingleDocument(xContext, config, docID, xComponent, this);
    documents.add(newDocument);
    if (!testMode) {              //  xComponent == null for test cases 
      newDocument.setLanguage(docLanguage);
      newDocument.setLtMenus(new LanguageToolMenus(xContext, newDocument, config));
    }
    
    if (debugMode) {
      MessageHandler.printToLogFile("Document " + docNum + " created; docID = " + docID);
    }
    return documents.size() - 1;
  }

  /**
   * Delete a document number and all internal space
   */
  private void removeDoc(String docID) {
    for (int i = documents.size() - 1; i >= 0; i--) {
      if (!docID.equals(documents.get(i).getDocID()) && documents.get(i).isDisposed()) {
        if (goneContext != null) {
          XComponent xComponent = documents.get(i).getXComponent();
          if (xComponent != null && !xComponent.equals(goneContext)) {
            goneContext = null;
          }
        }
//        if (debugMode) {
          MessageHandler.printToLogFile("Disposed document " + documents.get(i).getDocID() + " removed");
//        }
        documents.remove(i);
      }
    }
  }
  
  /**
   * Delete the menu listener of a document
   */
  public void removeMenuListener(XComponent xComponent) {
    if (xComponent != null) {
      for (int i = 0; i < documents.size(); i++) {
        XComponent docComponent = documents.get(i).getXComponent();
        if (docComponent != null && xComponent.equals(docComponent)) { //  disposed document found
          documents.get(i).getLtMenu().removeListener();
          if (debugMode) {
            MessageHandler.printToLogFile("Menu listener of document " + documents.get(i).getDocID() + " removed");
          }
          break;
        }
      }
    }
  }

  /**
   * Initialize LanguageTool
   */
  SwJLanguageTool initLanguageTool() {
    return initLanguageTool(null, false);
  }

  SwJLanguageTool initLanguageTool(boolean setService) {
    return initLanguageTool(null, setService);
  }

  SwJLanguageTool initLanguageTool(Language currentLanguage, boolean setService) {
    SwJLanguageTool langTool = null;
    try {
      config = new Configuration(configDir, configFile, oldConfigFile, docLanguage);
      noBackgroundCheck = config.noBackgroundCheck();
      if (linguServices == null) {
        linguServices = new LinguisticServices(xContext);
      }
      linguServices.setNoSynonymsAsSuggestions(config.noSynonymsAsSuggestions() || testMode);
      if (this.langTool == null) {
        OfficeTools.setLogLevel(config.getlogLevel());
        debugMode = OfficeTools.DEBUG_MODE_MD;
      }
      if (currentLanguage == null) {
        fixedLanguage = config.getDefaultLanguage();
        if (fixedLanguage != null) {
          docLanguage = fixedLanguage;
        }
        currentLanguage = docLanguage;
      }
      // not using MultiThreadedSwJLanguageTool here fixes "osl::Thread::Create failed", see https://bugs.documentfoundation.org/show_bug.cgi?id=90740:
      langTool = new SwJLanguageTool(currentLanguage, config.getMotherTongue(),
          new UserConfig(config.getConfigurableValues(), linguServices), config, extraRemoteRules, testMode);
      config.initStyleCategories(langTool.getAllRules());
      /* The next row is only for a single line break marks a paragraph
      docLanguage.getSentenceTokenizer().setSingleLineBreaksMarksParagraph(true);
       */
      File ngramDirectory = config.getNgramDirectory();
      if (ngramDirectory != null) {
        File ngramLangDir = new File(config.getNgramDirectory(), currentLanguage.getShortCode());
        if (ngramLangDir.exists()) {  // user might have ngram data only for some languages and that's okay
          langTool.activateLanguageModelRules(ngramDirectory);
          if (debugMode) {
            MessageHandler.printToLogFile("ngram Model activated for language: " + currentLanguage.getShortCode());
          }
        }
      }
      File word2VecDirectory = config.getWord2VecDirectory();
      if (word2VecDirectory != null) {
        File word2VecLangDir = new File(config.getWord2VecDirectory(), currentLanguage.getShortCode());
        if (word2VecLangDir.exists()) {  // user might have word2vec data only for some languages and that's okay
          langTool.activateWord2VecModelRules(word2VecDirectory);
        }
      }
      for (Rule rule : langTool.getAllActiveOfficeRules()) {
        if (rule.isDictionaryBasedSpellingRule()) {
          langTool.disableRule(rule.getId());
          if (rule.useInOffice()) {
            // set default off so it can be re-enabled by user configuration
            rule.setDefaultOff();
          }
        }
      }
      recheck = false;
      return langTool;
    } catch (Throwable t) {
      MessageHandler.showError(t);
    }
    return langTool;
  }

  /**
   * Enable or disable rules as given by configuration file
   */
  void initCheck(SwJLanguageTool langTool, Locale locale) {
    Set<String> disabledRuleIds = config.getDisabledRuleIds();
    if (disabledRuleIds != null) {
      // copy as the config thread may access this as well
      List<String> list = new ArrayList<>(disabledRuleIds);
      for (String id : list) {
        langTool.disableRule(id);
      }
    }
    Set<String> disabledCategories = config.getDisabledCategoryNames();
    if (disabledCategories != null) {
      // copy as the config thread may access this as well
      List<String> list = new ArrayList<>(disabledCategories);
      for (String categoryName : list) {
        langTool.disableCategory(new CategoryId(categoryName));
      }
    }
    Set<String> enabledRuleIds = config.getEnabledRuleIds();
    if (enabledRuleIds != null) {
      // copy as the config thread may access this as well
      List<String> list = new ArrayList<>(enabledRuleIds);
      for (String ruleName : list) {
        langTool.enableRule(ruleName);
      }
    }
    if (disabledRulesUI != null) {
      for (String id : disabledRulesUI) {
        langTool.disableRule(id);
      }
    }
    if (config.useLtDictionary()) {
      if (dictionary.setLtDictionary(xContext, locale, linguServices)) {
        resetCheck();
      }
    } else {
      if (dictionary.removeLtDictionaries(xContext)) {
        resetCheck();
      }
    }
  }
  
  /**
   * Initialize single documents, prepare text level rules and start queue
   */
  void initDocuments() {
    setConfigValues(config, langTool);
    sortedTextRules = new SortedTextRules();
    for (SingleDocument document : documents) {
      document.resetCache();
    }
    if (useQueue && !noBackgroundCheck) {
      if (textLevelQueue == null) {
        textLevelQueue = new TextLevelCheckQueue(this);
      } else {
        textLevelQueue.setReset();
      }
    }
  }
  
  /**
   * Reset document cache of all documents
   */
  void resetDocumentCaches() {
    for (SingleDocument document : documents) {
      document.resetDocumentCache();
    }
  }

  /**
   * Get current locale language
   */
  public Locale getLocale() {
    return locale;
  }
  
  /**
   * Get dictionary access
   */
  public LtDictionary getLtDictionary() {
    return dictionary;
  }

  /**
   * Get list of single documents
   */
  public List<SingleDocument> getDocuments() {
    return documents;
  }

  /**
   * Get text level queue
   */
  public TextLevelCheckQueue getTextLevelCheckQueue() {
    return textLevelQueue;
  }
  
  /**
   * true, if LanguageTool is switched off
   */
  public boolean isSwitchedOff() {
    return noBackgroundCheck;
  }

  /**
   *  Toggle Switch Off / On of LT
   *  return true if toggle was done 
   */
  public boolean toggleSwitchedOff() throws IOException {
    if (docLanguage == null) {
      docLanguage = getLanguage();
    }
    if (config == null) {
      config = new Configuration(configDir, configFile, oldConfigFile, docLanguage);
    }
    noBackgroundCheck = !noBackgroundCheck;
    if (!noBackgroundCheck && textLevelQueue != null) {
      textLevelQueue.setStop();
      textLevelQueue = null;
    }
    recheck = true;
    config.saveNoBackgroundCheck(noBackgroundCheck, docLanguage);
    for (SingleDocument document : documents) {
      document.setConfigValues(config);
    }
    return true;
  }

  /**
   * Set docID used within menu
   */
  public void setMenuDocId(String docId) {
    menuDocId = docId;
  }

  /**
   * Call method ignoreOnce for concerned document 
   */
  public String ignoreOnce() {
    for (SingleDocument document : documents) {
      if (menuDocId.equals(document.getDocID())) {
        return document.ignoreOnce();
      }
    }
    return null;
  }
  
  /**
   * reset ignoreOnce information in all documents
   */
  public void resetIgnoreOnce() {
    for (SingleDocument document : documents) {
      document.resetIgnoreOnce();
    }
  }

  /**
   * Deactivate a rule as requested by the context menu
   */
  public void deactivateRule() {
    for (SingleDocument document : documents) {
      if (menuDocId.equals(document.getDocID())) {
        deactivateRule(document.deactivateRule(), false);
        break;
      }
    }
  }
  
  /**
   * Deactivate a rule by rule iD
   */
  public void deactivateRule(String ruleId, boolean reactivate) {
    if (ruleId != null) {
      try {
        Configuration confg = new Configuration(configDir, configFile, oldConfigFile, docLanguage);
        Set<String> ruleIds = new HashSet<>();
        ruleIds.add(ruleId);
        if (reactivate) {
          confg.removeDisabledRuleIds(ruleIds);
        } else {
          confg.addDisabledRuleIds(ruleIds);
        }
        confg.saveConfiguration(docLanguage);
        if (debugMode) {
          MessageHandler.printToLogFile("Rule Disabled: " + (ruleId == null ? "null" : ruleId));
        }
      } catch (IOException e) {
        MessageHandler.printException(e);
      }
    }
  }
  
  /**
   * Returns a list of different numbers of paragraphs to check for text level rules
   * (currently only -1 for full text check and n for max number for other text level rules)
   */
  public List<Integer> getNumMinToCheckParas() {
    if (sortedTextRules == null) {
      return null;
    }
    return sortedTextRules.minToCheckParagraph;
  }

  /**
   * Test if sorted rules for index exist
   */
  public boolean isSortedRuleForIndex(int index) {
    if (index < 0 || index > 1 || sortedTextRules.textLevelRules.get(index).isEmpty()) {
      return false;
    }
    return true;
  }

  /**
   * activate all rules stored under a given index related to the list of getNumMinToCheckParas
   * deactivate all other text level rules
   */
  public void activateTextRulesByIndex(int index, SwJLanguageTool langTool) {
    sortedTextRules.activateTextRulesByIndex(index, langTool);
  }

  /**
   * reactivate all text level rules
   */
  public void reactivateTextRules(SwJLanguageTool langTool) {
    sortedTextRules.reactivateTextRules(langTool);;
  }

  /**
   * class to store all text level rules sorted by the minimum to check paragraphs
   * (currently only full text check and all other text level rules)
   */
  class SortedTextRules { 
    List<Integer> minToCheckParagraph;
    List<List<String>> textLevelRules;

    SortedTextRules () {
      minToCheckParagraph = new ArrayList<>();
      textLevelRules = new ArrayList<>();
      minToCheckParagraph.add(0);
      textLevelRules.add(new ArrayList<>());
      if (useQueue && config.getNumParasToCheck() != 0) {
        minToCheckParagraph.add(config.getNumParasToCheck());
      } else {
        minToCheckParagraph.add(-1);
      }
      textLevelRules.add(new ArrayList<>());
      List<Rule> rules = langTool.getAllActiveOfficeRules();
      for (Rule rule : rules) {
        if (rule instanceof TextLevelRule && !langTool.getDisabledRules().contains(rule.getId()) 
            && !disabledRulesUI.contains(rule.getId())) {
          insertRule(((TextLevelRule) rule).minToCheckParagraph(), rule.getId());
        }
      }
      if (debugMode) {
        MessageHandler.printToLogFile("Number different minToCheckParagraph: " + minToCheckParagraph.size());
        for ( int i = 0; i < minToCheckParagraph.size(); i++) {
          MessageHandler.printToLogFile("minToCheckParagraph: " + minToCheckParagraph.get(i));
          for (int j = 0; j < textLevelRules.get(i).size(); j++) {
            MessageHandler.printToLogFile("RuleId: " + textLevelRules.get(i).get(j));
          }
        }
      }
    }

    /**
     * Insert a rule to list of text level rules
     */
    private void insertRule (int minPara, String ruleId) {
      if (useQueue) {
        if (minPara == 0) {
          textLevelRules.get(0).add(ruleId);
        } else {
          textLevelRules.get(1).add(ruleId);
        }
      } else {
        if (minPara < 0) {
          textLevelRules.get(1).add(ruleId);
        } else {
          if (minPara > minToCheckParagraph.get(0)) {
            minToCheckParagraph.set(0, minPara);
          }
          textLevelRules.get(0).add(ruleId);
        }
      }
    }

    /**
     * Get the minimum of paragraphs that should be checked
     */
    public List<Integer> getMinToCheckParas() {
      return minToCheckParagraph;
    }

    /**
     * Activate the text level rules for a specified cache 
     */
    public void activateTextRulesByIndex(int nCache, SwJLanguageTool langTool) {
      for (int i = 0; i < textLevelRules.size(); i++) {
        if (i == nCache) {
          for (String ruleId : textLevelRules.get(i)) {
            langTool.enableRule(ruleId);
          }
        } else {
          for (String ruleId : textLevelRules.get(i)) {
            langTool.disableRule(ruleId);
          }
        }
      }
    }

    /**
     * Reactivate the text level rules which was deactivated for a specified cache 
     */
    public void reactivateTextRules(SwJLanguageTool langTool) {
      for (List<String> textRules : textLevelRules) {
        for (String ruleId : textRules) {
          langTool.enableRule(ruleId);
        }
      }
    }

  }
  
  /**
   * We leave spell checking to OpenOffice/LibreOffice.
   * @return false
   */
  public final boolean isSpellChecker() {
    return false;
  }
  
  /**
   * Returns xContext
   */
  public XComponentContext getContext() {
    return xContext;
  }

  /**
   * Runs LT options dialog box.
   */
  public void runOptionsDialog() {
    try {
      Configuration config = getConfiguration();
      Language lang = config.getDefaultLanguage();
      if (lang == null) {
        lang = getLanguage();
      }
      if (lang == null) {
        return;
      }
      SwJLanguageTool lTool = langTool;
      if (!lang.equals(docLanguage)) {
        docLanguage = lang;
        lTool = initLanguageTool();
        initCheck(lTool, LinguisticServices.getLocale(lang));
        config = this.config;
      }
      ConfigThread configThread = new ConfigThread(lang, config, lTool, this);
      configThread.start();
    } catch (Throwable t) {
      MessageHandler.showError(t);
    }
  }

  /**
   * @return An array of Locales supported by LT
   */
  public final static Locale[] getLocales() {
    try {
      List<Locale> locales = new ArrayList<>();
      Locale locale = null;
      for (Language lang : Languages.get()) {
        if (lang.getCountries().length == 0) {
          if (lang.getDefaultLanguageVariant() != null) {
            if (lang.getDefaultLanguageVariant().getVariant() != null) {
              locale = new Locale(lang.getDefaultLanguageVariant().getShortCode(),
                  lang.getDefaultLanguageVariant().getCountries()[0], lang.getDefaultLanguageVariant().getVariant());
            } else {
              locale = new Locale(lang.getDefaultLanguageVariant().getShortCode(),
                  lang.getDefaultLanguageVariant().getCountries()[0], "");
            }
          }
          else if (lang.getVariant() != null) {  // e.g. Esperanto
            locale =new Locale(LIBREOFFICE_SPECIAL_LANGUAGE_TAG, "", lang.getShortCodeWithCountryAndVariant());
          } else {
            locale = new Locale(lang.getShortCode(), "", "");
          }
        } else {
          for (String country : lang.getCountries()) {
            if (lang.getVariant() != null) {
              locale = new Locale(LIBREOFFICE_SPECIAL_LANGUAGE_TAG, country, lang.getShortCodeWithCountryAndVariant());
            } else {
              locale = new Locale(lang.getShortCode(), country, "");
            }
          }
        }
        if (locales != null && !OfficeTools.containsLocale(locales, locale)) {
          locales.add(locale);
        }
      }
      return locales.toArray(new Locale[0]);
    } catch (Throwable t) {
      MessageHandler.showError(t);
      return new Locale[0];
    }
  }

  /**
   * Add a listener that allow re-checking the document after changing the
   * options in the configuration dialog box.
   * 
   * @param eventListener the listener to be added
   * @return true if listener is non-null and has been added, false otherwise
   */
  public final boolean addLinguServiceEventListener(XLinguServiceEventListener eventListener) {
    if (eventListener == null) {
      return false;
    }
    xEventListeners.add(eventListener);
    return true;
  }

  /**
   * Remove a listener from the event listeners list.
   * 
   * @param eventListener the listener to be removed
   * @return true if listener is non-null and has been removed, false otherwise
   */
  public final boolean removeLinguServiceEventListener(XLinguServiceEventListener eventListener) {
    if (eventListener == null) {
      return false;
    }
    if (xEventListeners.contains(eventListener)) {
      xEventListeners.remove(eventListener);
      return true;
    }
    return false;
  }

  /**
   * Inform listener that the document should be rechecked for grammar and style check.
   */
  public boolean resetCheck() {
    return resetCheck(LinguServiceEventFlags.PROOFREAD_AGAIN);
  }

  /**
   * Inform listener that the doc should be rechecked for a special event flag.
   */
  public boolean resetCheck(short eventFlag) {
    if (!xEventListeners.isEmpty()) {
      for (XLinguServiceEventListener xEvLis : xEventListeners) {
        if (xEvLis != null) {
          LinguServiceEvent xEvent = new LinguServiceEvent();
          xEvent.nEvent = eventFlag;
          xEvLis.processLinguServiceEvent(xEvent);
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Configuration has be changed
   */
  void resetConfiguration() {
    linguServices = null;
    noBackgroundCheck = false;
    resetDocument();
  }

  /**
   * Inform listener (grammar checking iterator) that options have changed and
   * the doc should be rechecked.
   */
  void resetDocument() {
    setRecheck();
    resetCheck();
  }

  /**
   * Triggers the events from LT menu
   */
  public void trigger(String sEvent) {
    try {
      if (!testDocLanguage(true)) {
        return;
      }
      if ("configure".equals(sEvent)) {
        runOptionsDialog();
      } else if ("about".equals(sEvent)) {
        AboutDialogThread aboutThread = new AboutDialogThread(messages);
        aboutThread.start();
      } else if ("switchOff".equals(sEvent)) {
        if (toggleSwitchedOff()) {
          resetCheck(); 
        }
      } else if ("ignoreOnce".equals(sEvent)) {
        ignoreOnce();
      } else if ("deactivateRule".equals(sEvent)) {
        deactivateRule();
        resetDocument();
      } else if ("checkDialog".equals(sEvent) || "checkAgainDialog".equals(sEvent)) {
        if (ltDialog != null) {
          ltDialog.closeDialog();
        } else if (dialogIsRunning) {
          return;
        }
        setLtDialogIsRunning(true);
        SpellAndGrammarCheckDialog checkDialog = new SpellAndGrammarCheckDialog(xContext, this, docLanguage);
        if ("checkAgainDialog".equals(sEvent)) {
          SingleDocument document = getCurrentDocument();
          if (document != null) {
            XComponent currentComponent = document.getXComponent();
            if (currentComponent != null) {
              DocumentCursorTools docCursor = new DocumentCursorTools(currentComponent);
              ViewCursorTools viewCursor = new ViewCursorTools(xContext);
              checkDialog.setTextViewCursor(0, 0, viewCursor, docCursor);
            }
          }
          resetDocumentCaches();
          resetDocument();
        }
        if (debugMode) {
          MessageHandler.printToLogFile("Start Spell And Grammar Check Dialog");
        }
        checkDialog.start();
      } else if ("nextError".equals(sEvent)) {
        SpellAndGrammarCheckDialog checkDialog = new SpellAndGrammarCheckDialog(xContext, this, docLanguage);
        checkDialog.nextError();
      } else if ("remoteHint".equals(sEvent)) {
        if (getConfiguration().useOtherServer()) {
          MessageHandler.showMessage(MessageFormat.format(messages.getString("loRemoteInfoOtherServer"), 
              getConfiguration().getServerUrl()));
        } else {
          MessageHandler.showMessage(messages.getString("loRemoteInfoDefaultServer"));
        }
      } else {
        MessageHandler.printToLogFile("Sorry, don't know what to do, sEvent = " + sEvent);
      }
    } catch (Throwable e) {
      MessageHandler.showError(e);
    }
  }
  
  /**
   * Test the language of the document
   * switch the check to LT if possible and language is supported
   */
  boolean testDocLanguage(boolean showMessage) {
    if (docLanguage == null) {
      if (linguServices == null) {
        linguServices = new LinguisticServices(xContext);
      }
      if (!linguServices.spellCheckerIsActive()) {
        if (showMessage) {
          MessageHandler.showMessage("LinguisticServices failed! LanguageTool can not be started!");
        } else {
          MessageHandler.printToLogFile("LinguisticServices failed! LanguageTool can not be started!");
        }
        return false;
      }
      Locale locale = getDocumentLocale();
      try {
        int n = 0;
        while (locale == null && n < 100) {
          Thread.sleep(500);
          if (debugMode) {
            MessageHandler.printToLogFile("Try to get locale: n = " + n);
          }
          locale = getDocumentLocale();
          n++;
        }
      } catch (InterruptedException e) {
        MessageHandler.showError(e);
      }
      if (locale == null) {
        if (showMessage) {
          MessageHandler.showMessage("No Local! LanguageTool can not be started!");
        } else {
          MessageHandler.printToLogFile("No Local! LanguageTool can not be started!");
        }
        return false;
      } else if (!hasLocale(locale)) {
        String message = Tools.i18n(messages, "language_not_supported", locale.Language);
        MessageHandler.showMessage(message);
        return false;
      }
      if (debugMode) {
        MessageHandler.printToLogFile("locale: " + locale.Language + "-" + locale.Country);
      }
      if (!linguServices.setLtAsGrammarService(xContext, locale)) {
        if (showMessage) {
          MessageHandler.showMessage("LinguisticServices failed! LanguageTool can not be started!");
        } else {
          MessageHandler.printToLogFile("LinguisticServices failed! LanguageTool can not be started!");
        }
        return false;
      }
      resetCheck();
      return false;
    }
    return true;
  }

  /**
   * Test if the needed java version is installed
   */
  public boolean javaVersionOkay() {
    String version = System.getProperty("java.version");
    if (version != null
        && (version.startsWith("1.0") || version.startsWith("1.1")
            || version.startsWith("1.2") || version.startsWith("1.3")
            || version.startsWith("1.4") || version.startsWith("1.5")
            || version.startsWith("1.6") || version.startsWith("1.7"))) {
      MessageHandler.showMessage("Error: LanguageTool requires Java 8 or later. Current version: " + version);
      return false;
    }
    try {
      // do not set look and feel for on Mac OS X as it causes the following error:
      // soffice[2149:2703] Apple AWT Java VM was loaded on first thread -- can't start AWT.
      if (!System.getProperty("os.name").contains("OS X")) {
         // Cross-Platform Look And Feel @since 3.7
         if (System.getProperty("os.name").contains("Linux")) {
           UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
         }
         else {
           UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         }
      }
    } catch (Exception ignored) {
      // Well, what can we do...
    }
    return true;
  }

  /**
   * Get home directory of the user from system
   */
  private File getHomeDir() {
    String homeDir = System.getProperty("user.home");
    if (homeDir == null) {
      MessageHandler.showError(new RuntimeException("Could not get home directory"));
      return null;
    }
    return new File(homeDir);
  }

  /**
   * class to run the about dialog
   */
  private static class AboutDialogThread extends Thread {

    private final ResourceBundle messages;

    AboutDialogThread(ResourceBundle messages) {
      this.messages = messages;
    }

    @Override
    public void run() {
      // Note: null can cause the dialog to appear on the wrong screen in a
      // multi-monitor setup, but we just don't have a proper java.awt.Component
      // here which we could use instead:
      AboutDialog about = new AboutDialog(messages, null);
      about.show();
    }
  }

  /**
   * Called when "Ignore" is selected e.g. in the context menu for an error.
   */
  public void ignoreRule(String ruleId, Locale locale) {
    /* TODO: config should be locale-dependent */
    addDisabledRule(ruleId);
    setRecheck();
  }

  /**
   * Called on rechecking the document - resets the ignore status for rules that
   * was set in the spelling dialog box or in the context menu.
   * 
   * The rules disabled in the config dialog box are left as intact.
   */
  public void resetIgnoreRules() {
    resetDisabledRules();
    setRecheck();
    resetIgnoreOnce();
    docReset = true;
  }

  /**
   * Get the displayed service name for LT
   */
  public String getServiceDisplayName(Locale locale) {
    return "LanguageTool";
  }

  /**
   * remove internal stored text if document disposes
   */
  public void disposing(EventObject source) {
    //  the data of document will be removed by next call of getNumDocID
    //  to finish checking thread without crashing
    XComponent goneContext = UnoRuntime.queryInterface(XComponent.class, source.Source);
    if (goneContext == null) {
      MessageHandler.printToLogFile("xComponent of closed document is null");
    } else {
      setContextOfClosedDoc(goneContext);
      goneContext.removeEventListener(xEventListener);
    }
  }

  /** class to start a separate thread to switch grammar check to LT
   * Experimental currently not used 
   */
  @SuppressWarnings("unused")
  private class LtHelper extends Thread {
    @Override
    public void run() {
      try {
        Thread.sleep(3000);
        testDocLanguage(false);
      } catch (InterruptedException e) {
        MessageHandler.showError(e);
      }

    }
  }

}
