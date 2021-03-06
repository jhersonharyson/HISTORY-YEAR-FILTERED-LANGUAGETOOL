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
package org.languagetool.rules.de;

import org.junit.Before;
import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.TestTools;
import org.languagetool.language.GermanyGerman;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AgreementRuleTest {

  private AgreementRule rule;
  private JLanguageTool lt;

  @Before
  public void setUp() {
    rule = new AgreementRule(TestTools.getMessages("de"), (GermanyGerman)Languages.getLanguageForShortCode("de-DE"));
    lt = new JLanguageTool(Languages.getLanguageForShortCode("de-DE"));
  }

  @Test
  public void testCompoundMatch() throws IOException {
    assertBad("Das ist die Original Mail", "die Originalmail", "die Original-Mail");
    assertBad("Das ist die neue Original Mail", "die neue Originalmail", "die neue Original-Mail");
    assertBad("Das ist die ganz neue Original Mail", "die ganz neue Originalmail", "die ganz neue Original-Mail");
    assertBad("Doch dieser kleine Magnesium Anteil ist entscheidend.", "dieser kleine Magnesiumanteil", "dieser kleine Magnesium-Anteil");
    assertBad("Doch dieser sehr kleine Magnesium Anteil ist entscheidend.", "dieser sehr kleine Magnesiumanteil", "dieser sehr kleine Magnesium-Anteil");
    assertBad("Die Standard Priorit??t ist 5.", "Die Standardpriorit??t", "Die Standard-Priorit??t");
    assertBad("Die derzeitige Standard Priorit??t ist 5.", "Die derzeitige Standardpriorit??t", "Die derzeitige Standard-Priorit??t");
    assertBad("Ein neuer LanguageTool Account", "Ein neuer LanguageTool-Account");
    assertBad("Danke f??r deine Account Daten", "deine Accountdaten", "deine Account-Daten");
    assertBad("Mit seinem Konkurrent Alistair M??ller", "sein Konkurrent", "seinem Konkurrenten");
    assertBad("Wir gehen ins Fitness Studio", "ins Fitnessstudio", "ins Fitness-Studio");
    assertBad("Wir gehen durchs Fitness Studio", "durchs Fitnessstudio", "durchs Fitness-Studio");
    assertGood("Es gibt ein Sprichwort, dem zufolge der t??gliche Genuss einer Mandel dem Ged??chtnis f??rderlich sei.");
    //assertBad("Die Bad Taste Party von Susi", "Die Bad-Taste-Party");   // not supported yet
    //assertBad("Die Update Liste.", "Die Updateliste");  // not accepted by speller
    List<RuleMatch> matches = lt.check("Er folgt damit dem Tipp des Autoren Michael M??ller.");
    assertThat(matches.size(), is(1));
    assertFalse(matches.get(0).getMessage().contains("zusammengesetztes Nomen"));
  }
  
  @Test
  public void testDetNounRule() throws IOException {
    // correct sentences:
    assertGood("Die Einen sagen dies, die Anderen das.");
    assertGood("So ist es in den USA.");
    assertGood("Das ist der Tisch.");
    assertGood("Das ist das Haus.");
    assertGood("Das ist die Frau.");
    assertGood("Das ist das Auto der Frau.");
    assertGood("Das geh??rt dem Mann.");
    assertGood("Das Auto des Mannes.");
    assertGood("Das interessiert den Mann.");
    assertGood("Das interessiert die M??nner.");
    assertGood("Das Auto von einem Mann.");
    assertGood("Das Auto eines Mannes.");
    assertGood("Des gro??en Mannes.");
    assertGood("Und nach der Nummerierung kommt die ??berschrift.");
    assertGood("Sie wiesen dieselben Verzierungen auf.");
    assertGood("Die erw??hnte Konferenz ist am Samstag.");
    assertGood("Sie erreichten 5 Prozent.");
    assertGood("Sie erreichten mehrere Prozent Zustimmung.");
    assertGood("Die Bestandteile, aus denen Schwefel besteht.");
    assertGood("Ich tat f??r ihn, was kein anderer Autor f??r ihn tat.");
    assertGood("Ich tat f??r ihn, was keine andere Autorin f??r ihn tat.");
    assertGood("Ich tat f??r ihn, was kein anderes Kind f??r ihn tat.");
    assertGood("Ich tat f??r ihn, was dieser andere Autor f??r ihn tat.");
    assertGood("Ich tat f??r ihn, was diese andere Autorin f??r ihn tat.");
    assertGood("Ich tat f??r ihn, was dieses andere Kind f??r ihn tat.");
    assertGood("Ich tat f??r ihn, was jener andere Autor f??r ihn tat.");
    assertGood("Ich tat f??r ihn, was jeder andere Autor f??r ihn tat.");
    assertGood("Ich tat f??r ihn, was jede andere Autorin f??r ihn tat.");
    assertGood("Ich tat f??r ihn, was jedes andere Kind f??r ihn tat.");
    assertGood("Klebe ein Preisschild auf jedes einzelne Produkt.");
    assertGood("Eine Stadt, in der zurzeit eine rege Baut??tigkeit herrscht.");
    assertGood("... wo es zu einer regen Baut??tigkeit kam.");
    assertGood("Mancher ausscheidende Politiker hinterl??sst eine L??cke.");
    assertGood("Kern einer jeden Trag??die ist es, ..");
    assertGood("Das wenige Sekunden alte Baby schrie laut.");
    assertGood("Meistens sind das Frauen, die damit besser umgehen k??nnen.");
    assertGood("Er fragte, ob das Spa?? macht.");
    assertGood("Das viele Geld wird ihr helfen.");
    assertGood("Er verspricht jedem hohe Gewinne.");
    assertGood("Er versprach allen Renditen jenseits von 15 Prozent.");
    assertGood("Sind das Eier aus Bodenhaltung?");
    assertGood("Dir macht doch irgendwas Sorgen.");
    assertGood("Sie fragte, ob das wirklich Kunst sei.");
    assertGood("F??r ihn ist das Alltag.");
    assertGood("F??r die Religi??sen ist das Blasphemie und f??hrt zu Aufst??nden.");
    assertGood("Das Orange ist sch??n.");
    assertGood("Dieses r??tliche Orange gef??llt mir am besten.");
    assertGood("Das ist ein super Tipp.");
    assertGood("Er nahm allen Mut zusammen und ging los.");
    assertGood("Sie kann einem Angst einjagen.");
    assertGood("Damit sollten zum einen neue Energien gef??rdert werden, zum anderen der Sozialbereich.");
    assertGood("Nichts ist mit dieser einen Nacht zu vergleichen.");
    assertGood("dann muss Schule dem Rechnung tragen.");
    assertGood("Das Dach von meinem Auto.");
    assertGood("Das Dach von meinen Autos.");
    assertGood("Da stellt sich die Frage: Ist das Science-Fiction oder moderne Mobilit??t?");
    assertGood("Er hat einen Post ver??ffentlicht.");
    assertGood("Eine l??ckenlose Aufkl??rung s??mtlicher physiologischer Gehirnprozesse");
    assertGood("Sie fragte verwirrt: ???Ist das Zucker????");
    assertGood("Er versuchte sich vorzustellen, was sein Klient f??r ein Mensch sei.");
    assertGood("Sie legen ein Teilst??ck jenes Weges zur??ck, den die Tausenden Juden 1945 auf sich nehmen mussten.");
    assertGood("Aber das ignorierte Herr Grey bewusst.");
    assertGood("Aber das ignorierte Herr M??ller bewusst.");
    assertGood("Ich werde mich zur??cknehmen und mich frischen Ideen zuwenden.");
    assertGood("Das, plus ein eigener Firmenwagen.");
    assertGood("Dieses leise Summen st??rt nicht.");
    assertGood("Die Tiroler K??che");
    assertGood("Was ist denn das f??r ein ungew??hnlicher Name?");
    assertGood("Besonders reizen mich Fahrr??der.");
    assertGood("Und nur, weil mich psychische Erkrankungen aus der Bahn werfen");
    assertGood("Das kostet dich Zinsen.");
    assertGood("Sie hatten keine Chance gegen das kleinere Preu??en.");
    assertGood("Den 2019er Wert hatten sie gesch??tzt.");
    assertGood("Andere formale Systeme, deren Semantiken jeweils...");
    assertGood("Gesetz zur ??nderung des K??ndigungsrechts und anderer arbeitsrechtlicher Vorschriften");
    assertGood("Die dauerhafte Abgrenzung des sp??ter Niedersachsen genannten Gebietes von Westfalen begann im 12. Jahrhundert.");
    assertGood("Lieber jemanden, der einem Tipps gibt.");
    assertGood("Jainas ist sogar der Genuss jeglicher tierischer Nahrungsmittel strengstens untersagt.");
    assertGood("Es sind jegliche tierische Nahrungsmittel untersagt.");
    assertGood("Das reicht bis weit ins heutige Hessen.");
    assertGood("Die Customer Journey.");
    assertGood("F??r dich geh??rt Radfahren zum perfekten Urlaub dazu?");
    assertGood(":D:D Leute, bitte!");
    assertGood("Es gen??gt, wenn ein Mann sein eigenes Gesch??ft versteht und sich nicht in das anderer Leute einmischt.");
    assertGood("Ich habe das einige Male versucht.");
    assertGood("Und keine M??rchen erz??hlst, die dem anderen Hoffnungen machen k??nnen.");
    assertGood("Um diese K??rpergr????en zu erreichen, war das Wachstum der Vertreter der Gattung Dinornis offenbar gegen??ber dem anderer Moa-Gattungen beschleunigt");
    assertGood("Der Sch??del entspricht in den Proportionen dem anderer Vulpes-Arten, besitzt aber sehr gro??e Paukenh??hlen, ein typisches Merkmal von W??stenbewohnern.");
    assertGood("Deuterium l??sst sich aufgrund des gro??en Massenunterschieds leichter anreichern als die Isotope der anderer Elemente wie z. B. Uran.");
    assertGood("Unklar ist, ob er zwischen der Atemseele des Menschen und der anderer Lebewesen unterschied.");
    assertGood("Die Liechtensteiner Grenze ist im Verh??ltnis zu der anderer L??nder kurz, da Liechtenstein ein eher kleines Land ist.");
    assertGood("Picassos Kunstwerke werden h??ufiger gestohlen als die anderer K??nstler.");
    assertGood("Schreibe einen Artikel ??ber deine Erfahrungen im Ausland oder die anderer Leute in deinem Land.");
    assertGood("Die Bev??lkerungen Chinas und Indiens lassen die anderer Staaten als Zwerge erscheinen.");
    assertGood("Der eine mag Obst, ein anderer Gem??se, wieder ein anderer mag Fisch; allen kann man es nicht recht machen.");
    assertGood("Mittels eines Bootloaders und zugeh??riger Software kann nach jedem Anstecken des Adapters eine andere Firmware-Varianten geladen werden");
    assertGood("Wenn sie eine andere Gr????e ben??tigen, teilen uns ihre speziellen W??nsche mit und wir unterbreiten ihnen ein Angebot ??ber Preis und Lieferung.");
    assertGood("Dabei wird in einer Vakuumkammer eine einige Mikrometer dicke CVD-Diamantschicht auf den Substraten abgeschieden.");
    assertGood("1916 versuchte Gilbert Newton Lewis, die chemische Bindung durch Wechselwirkung der Elektronen eines Atoms mit einem anderen Atomen zu erkl??ren.");
    assertGood("Vom einen Ende der Stra??e zum anderen.");
    assertGood("Er war m??de vom vielen Laufen.");
    assertGood("Sind das echte Diamanten?");
    assertGood("Es wurde eine Verordnung erlassen, der zufolge jeder Haushalt Energie einsparen muss.");
    assertGood("Im Jahr 1922 verlieh ihm K??nig George V. den erblichen Titel eines Baronet. ");
    assertGood("... der zu dieser Zeit aber ohnehin schon allen Einfluss verloren hatte.");
    assertGood("Ein Geschenk, das Ma??st??be setzt");

    assertGood("Wir machen das Januar.");
    assertGood("Wir teilen das Morgen mit.");
    assertGood("Wir pr??sentierten das vorletzten Sonnabend.");
    assertGood("Ich release das Vormittags.");
    assertGood("Sie aktualisieren das Montags.");
    assertGood("Kannst du das Mittags machen?");
    assertGood("K??nnen Sie das n??chsten Monat erledigen?");
    assertGood("K??nnen Sie das auch n??chsten Monat erledigen?");

    assertGood("Das Dach meines Autos.");
    assertGood("Das Dach meiner Autos.");

    assertGood("Das Dach meines gro??en Autos.");
    assertGood("Das Dach meiner gro??en Autos.");

    assertGood("Dann schlug er so kr??ftig wie er konnte mit den Schwingen.");
    assertGood("Also wenn wir Gl??ck haben, ...");
    assertGood("Wenn wir Pech haben, ...");
    assertGood("Ledorn ??ffnete eines der an ihr vorhandenen F??cher.");
    assertGood("Auf der einen Seite endlose D??nen");
    assertGood("In seinem Maul hielt er einen blutigen Fleischklumpen.");
    assertGood("Gleichzeitig dachte er intensiv an Nebelschwaden, aus denen Wolken ja bestanden.");
    assertGood("Warum stellte der blo?? immer wieder dieselben Fragen?");
    assertGood("Bei der Hinreise.");
    assertGood("Schlie??lich tauchten in einem Waldst??ck unter ihnen Schienen auf.");

    assertGood("Das Wahlrecht, das Frauen damals zugesprochen bekamen.");
    assertGood("Es war Karl, dessen Leiche Donnerstag gefunden wurde.");

    assertGood("Erst recht ich Arbeiter.");
    assertGood("Erst recht wir Arbeiter.");
    assertGood("Erst recht wir flei??igen Arbeiter.");

    assertGood("Dann lud er Freunde ein.");
    assertGood("Dann lud sie Freunde ein.");
    assertGood("Aller Kommunikation liegt dies zugrunde.");
    assertGood("Pragmatisch w??hlt man solche Formeln als Axiome.");
    assertGood("Der eine Polizist rief dem anderen zu...");
    assertGood("Das eine Kind rief dem anderen zu...");
    assertGood("Er wollte seine Interessen wahrnehmen.");

    assertGood("... wo Krieg den Unschuldigen Leid und Tod bringt.");
    assertGood("Der Abschuss eines Papageien.");

    assertGood("Die Beibehaltung des Art. 1 ist geplant.");
    assertGood("Die Verschiebung des bisherigen Art. 1 ist geplant.");

    assertGood("In diesem Fall hatte das Vorteile.");
    assertGood("So hat das Konsequenzen.");

    assertGood("Ein f??r viele wichtiges Anliegen.");
    assertGood("Das weckte bei vielen ungute Erinnerungen.");
    assertGood("Etwas, das einem Angst macht.");
    assertGood("Einem geschenkten Gaul schaut man nicht ins Maul.");

    assertGood("Das erfordert K??nnen.");
    assertGood("Ist das Kunst?");
    assertGood("Ist das Kunst oder Abfall?");
    assertGood("Die Zeitdauer, w??hrend der Wissen n??tzlich bleibt, wird k??rzer.");
    assertGood("Es sollte nicht viele solcher Bilder geben");
    assertGood("In den 80er Jahren.");
    assertGood("Hast du etwas das Carina machen kann?");
    assertGood("Ein Artikel in den Ruhr Nachrichten.");
    assertGood("Ich wollte nur allen Hallo sagen.");
    assertGood("Ich habe deshalb allen Freund*innen Bescheid gegeben.");   // Gendersternchen, https://github.com/languagetool-org/languagetool/issues/2417
    assertGood("Ich habe deshalb allen Freund_innen Bescheid gegeben.");
    assertGood("Ich habe deshalb allen Freund:innen Bescheid gegeben.");
    assertGood("Sein*e Mitarbeiter*in ist davon auch betroffen.");
    assertGood("Jede*r Mitarbeiter*in ist davon betroffen.");
    assertGood("Alle Professor*innen");
    assertGood("Gleichzeitig w??nscht sich Ihr frostresistenter Mitbewohner einige Grad weniger im eigenen Zimmer?");
    assertGood("Ein Trainer, der zum einen Fu??ballspiele sehr gut lesen und analysieren kann");
    assertGood("Eine Massengrenze, bis zu der Lithium nachgewiesen werden kann.");
    assertGood("Bei uns im Krankenhaus betrifft das Operationss??le.");
    assertGood("Macht dir das Freude?");
    assertGood("Das macht jedem Angst.");

    // relative clauses:
    assertGood("Das Recht, das Frauen einger??umt wird.");
    assertGood("Der Mann, in dem quadratische Fische schwammen.");
    assertGood("Der Mann, durch den quadratische Fische schwammen.");
    assertGood("Gutenberg, der quadratische Mann.");
    assertGood("Die gr????te Stuttgarter Gr??nanlage ist der Friedhof.");
    assertGood("Die meisten Lebensmittel enthalten das.");  // Lebensmittel has NOG as gender in Morphy
    // TODO: Find agreement errors in relative clauses
    assertBad("Gutenberg, die Genie.");
    //assertBad("Gutenberg, die gr????te Genie.");
    //assertBad("Gutenberg, die gr????te Genie aller Zeiten.");
    assertGood("Die w??rmsten Monate sind August und September, die k??ltesten Januar und Februar.");
    // some of these used to cause false alarms:
    assertGood("Das M??nchener Fest.");
    assertGood("Das M??nchner Fest.");
    assertGood("Die Planung des M??nchener Festes.");
    assertGood("Das Berliner Wetter.");
    assertGood("Den Berliner Arbeitern ist das egal.");
    assertGood("Das Haus des Berliner Arbeiters.");
    assertGood("Es geh??rt dem Berliner Arbeiter.");
    assertGood("Das Stuttgarter Auto.");
    assertGood("Das Bielefelder Radio.");
    assertGood("Das G??tersloher Radio.");
    assertGood("Das wirklich Wichtige kommt jetzt erst.");
    assertGood("Besonders wenn wir Wermut oder Absinth trinken.");
    assertGood("Ich w??nsche dir alles Gute.");
    assertGood("Es ist nicht bekannt, mit welchem Alter Kinder diese F??higkeit erlernen.");
    assertGood("Dieser ist nun in den Ortungsbereich des einen Roboters gefahren.");
    assertGood("Wenn dies gro??en Erfolg hat, werden wir es weiter f??rdern.");
    assertGood("Die Ereignisse dieses einen Jahres waren sehr schlimm.");
    assertGood("Er musste einen Hochwasser f??hrenden Fluss nach dem anderen ??berqueren.");
    assertGood("Darf ich Ihren F??ller f??r ein paar Minuten ausleihen?");
    assertGood("Bringen Sie diesen Gep??ckaufkleber an Ihrem Gep??ck an.");
    assertGood("Extras, die den Wert Ihres Autos erh??hen.");
    assertGood("Er hat einen 34-j??hrigen Sohn.");
    assertGood("Die Polizei erwischte die Diebin, weil diese Ausweis und Visitenkarte hinterlie??.");
    assertGood("Dieses Vers??umnis soll vertuscht worden sein - es wurde Anzeige erstattet.");
    assertGood("Die Firmen - nicht nur die ausl??ndischen, auch die katalanischen - treibt diese Frage um.");
    // TODO: assertGood("Der Obst und Getr??nke f??hrende Fachmarkt.");
    assertGood("Stell dich dem Leben l??chelnd!");
    assertGood("Die Messe wird auf das vor der Stadt liegende Ausstellungsgel??nde verlegt.");
    assertGood("Sie sind ein den Frieden liebendes Volk.");
    assertGood("Zum Teil sind das Krebsvorstufen.");
    assertGood("Er sagt, dass das Rache bedeutet.");
    assertGood("Wenn das K??he sind, bin ich ein Elefant.");
    assertGood("Karl sagte, dass sie niemandem Bescheid gegeben habe.");
    assertGood("Es blieb nur dieser eine Satz.");
    assertGood("Oder ist das Mathematikern vorbehalten?");
    assertGood("Wenn hier einer Fragen stellt, dann ich.");
    assertGood("Wenn einer Katzen mag, dann meine Schwester.");
    assertGood("Ergibt das Sinn?");
    assertGood("Sie ist ??ber die Ma??en sch??n.");
    assertGood("Ich vertraue ganz auf die Meinen.");
    assertGood("Was n??tzt einem Gesundheit, wenn man sonst ein Idiot ist?");
    assertGood("Auch das hatte sein Gutes.");
    assertGood("Auch wenn es sein Gutes hatte, war es doch traurig.");
    assertGood("Er wollte doch nur jemandem Gutes tun.");
    assertGood("und das erst Jahrhunderte sp??tere Auftauchen der Legende");
    assertGood("Texas und New Mexico, beides spanische Kolonien, sind...");
    assertGood("Unser Hund vergr??bt seine Knochen im Garten.");
    assertGood("Ob das Mehrwert bringt?");
    assertGood("Warum das Sinn macht?");
    assertGood("Das h??ngt davon ab, ob die Deutsch sprechen");
    assertGood("Die meisten Coaches wissen nichts.");
    assertGood("Die Pr??sent AG.");
    assertGood("In New York war er der Titelheld in Richard III. und spielte den Mark Anton in Julius C??sar.");
    assertGood("Vielen Dank f??rs Bescheid geben.");
    assertGood("Welche Display Ads?");
    assertGood("Das letzte Mal war das Anfang der 90er Jahre des vergangenen Jahrhunderts");
    assertGood("Der vom Rat der Justizminister gefasste Beschluss zur Aufnahme von Vertriebenen...");
    assertGood("Der letzte Woche vom Rat der Justizminister gefasste Beschluss zur Aufnahme von Vertriebenen...");
    assertGood("Was war sie nur f??r eine dumme Person!");
    assertGood("Was war ich f??r ein Idiot!");
    assertGood("Was f??r ein Idiot!");
    assertGood("Was f??r eine bl??de Kuh!");
    assertGood("Was ist sie nur f??r eine bl??de Kuh!");
    assertGood("Wie viele Paar Stiefel brauche ich eigentlich?");
    assertGood("Dieses versuchten Mathematiker 400 Jahre lang vergeblich zu beweisen.");
    //assertGood("Bei dem Papierabz??ge von Digitalbildern bestellt werden.");
    assertGood("Gem??lde informieren uns ??ber das Leben von den vergangenen Jahrhunderten???");
    assertGood("Die Partei, die bei den vorangegangenen Wahlen noch seine Politik unterst??tzt hatte.");
    assertGood("Bei Zunahme der aufgel??sten Mineralstoffe, bei denen...");
    assertGood("Je mehr Muskelspindeln in einem Muskel vorhanden sind, desto feiner k??nnen die mit diesem verbundenen Bewegungen abgestimmt werden.");
    assertGood("Diese datentechnischen Operationen werden durch Computerprogramme ausgel??st, d. h. ??ber entsprechende, in diesen enthaltene Befehle (als Teil eines implementierten Algorithmus') vorgegeben.");
    assertGood("Aus diesen resultierten Konflikte wie der Bauernkrieg und der Pf??lzische Erbfolgekrieg.");
    assertGood("Die Staatshandlungen einer Mikronation und von dieser herausgegebene Ausweise, Urkunden und Dokumente gelten im Rechtsverkehr als unwirksam");
    assertGood("Auf der Hohen See und auf den mit dieser verbundenen Gew??ssern gelten die internationalen Kollisionsverh??tungsregeln.");
    assertGood("Art. 11 Abs. 2 GGV setzt dem bestimmte Arten der au??ergemeinschaftlichen Zug??nglichmachung gleich");
    assertGood("Grunds??tzlich sind die Heilungschancen von M??nnern mit Brustkrebs nicht schlechter als die betroffener Frauen.");
    assertGood("In diesem Viertel bin ich aufgewachsen.");
    assertGood("Im November wurde auf dem Gel??nde der Wettbewerb ausgetragen.");
    assertGood("Er ist Eigent??mer des gleichnamigen Schemas und stellt dieses interessierten Dom??nen zur Verf??gung.");
    assertGood("Dort finden sie viele Informationen rund um die Themen Schwangerschaft, Geburt, Stillen, Babys und Kinder.");
    assertGood("Die Galerie zu den Bildern findet sich hier.");
    assertGood("Ganz im Gegensatz zu den Bl??ttern des Brombeerstrauches.");
    assertGood("Er erz??hlte von den Leuten und den Dingen, die er auf seiner Reise gesehen hatte.");
    assertGood("Diese Partnerschaft wurde 1989 nach dem Massaker auf dem Platz des Himmlischen Friedens eingefroren.");
    assertGood("Die Feuergefahr hingegen war f??r f??r die Londoner Teil des Alltags.");
    assertGood("Was ist, wenn ein Projekt bei den Berliner Type Awards mit einem Diplom ausgezeichnet wird?");
    assertGood("Was ist mit dem Liechtensteiner Kulturleben los?");
    // incorrect sentences:
    assertBad("Ein Buch mit einem ganz ??hnlichem Titel.");
    assertBad("Meiner Chef raucht.");
    assertBad("Er hat eine 34-j??hrigen Sohn.");
    assertBad("Es sind die Tisch.", "dem Tisch", "den Tisch", "der Tisch", "die Tische");
    assertBad("Es sind das Tisch.", "dem Tisch", "den Tisch", "der Tisch");
    assertBad("Es sind die Haus.", "das Haus", "dem Haus", "die H??user");
    assertBad("Es sind der Haus.", "das Haus", "dem Haus", "der H??user");
    assertBad("Es sind das Frau.", "der Frau", "die Frau");
    assertBad("Das Auto des Mann.", "dem Mann", "den Mann", "der Mann", "des Mannes", "des Manns");
    assertBad("Das interessiert das Mann.", "dem Mann", "den Mann", "der Mann");
    assertBad("Das interessiert die Mann.", "dem Mann", "den Mann", "der Mann", "die M??nner");
    assertBad("Das Auto ein Mannes.", "ein Mann", "eines Mannes");
    assertBad("Das Auto einem Mannes.", "einem Mann", "einem Manne", "eines Mannes");
    assertBad("Das Auto einer Mannes.", "eines Mannes");
    assertBad("Das Auto einen Mannes.", "einen Mann", "eines Mannes");
    assertBad("Die Galerie zu den Bilder findet sich hier.");
    assertBad("Ganz im Gegensatz zu den Bl??tter des Brombeerstrauches.");
    //assertBad("Das erw??hnt Auto bog nach rechts ab.");    // TODO
    assertGood("Das erlaubt Forschern, neue Versuche durchzuf??hren.");
    assertGood("Dies erm??glicht Forschern, neue Versuche durchzuf??hren.");
    assertGood("Je l??nger zugewartet wird, desto schwieriger d??rfte es werden, die Jungtiere von den Elterntieren zu unterscheiden.");
    assertGood("Er schrieb ein von 1237 bis 1358 reichendes Geschichtswerk, dessen Schwerpunkt auf den Ereignissen in der Lombardei liegt.");
    assertBad("Die erw??hnt Konferenz ist am Samstag.");
    assertBad("Die erw??hntes Konferenz ist am Samstag.");
    assertBad("Die erw??hnten Konferenz ist am Samstag.");
    assertBad("Die erw??hnter Konferenz ist am Samstag.");
    assertBad("Die erw??hntem Konferenz ist am Samstag.");
    assertBad("Die gemessen Werte werden in die l??nderspezifische H??he ??ber dem Meeresspiegel umgerechnet.");
    assertBad("Dar??ber hinaus haben wir das berechtigte Interessen, diese Daten zu verarbeiten.");
    assertBad("Eine Amnestie kann den Hingerichteten nicht das Leben und dem heimgesuchten Familien nicht das Gl??ck zur??ckgeben.");
    //assertBad("Zu den gef??hrdete V??geln Malis geh??ren der Strau??, Gro??trappen und Perlhuhn.");
    //assertBad("Zu den gef??hrdete V??gel Malis geh??ren der Strau??, Gro??trappen und Perlhuhn.");
    assertBad("Z. B. therapeutisches Klonen, um aus den gewonnen Zellen in vitro Ersatzorgane f??r den Patienten zu erzeugen");
    //assertBad("Gem??lde informieren uns ??ber das Leben von den vergangenen Jahrhunderte???");
    assertBad("Die Partei, die bei den vorangegangen Wahlen noch seine Politik unterst??tzt hatte.");
    assertBad("Bei Zunahme der aufgel??sten Mineralstoffen, bei denen...");
    assertBad("Durch die gro??e Vielfalt der verschiedene Linien ist f??r jeden Anspruch die richtige Brille im Portfolio.");
    assertBad("In diesen Viertel bin ich aufgewachsen.");
    assertBad("Im November wurde auf den Gel??nde der Wettbewerb ausgetragen.");
    assertBad("Dort finden sie Testberichte und viele Informationen rund um das Themen Schwangerschaft, Geburt, Stillen, Babys und Kinder.");
    assertBad("Je l??nger zugewartet wird, desto schwieriger d??rfte es werden, die Jungtiere von den Elterntiere zu unterscheiden.");
    assertBad("Er schrieb ein von 1237 bis 1358 reichendes Geschichtswerk, dessen Schwerpunkt auf den Ereignisse in der Lombardei liegt.");
    assertBad("Des gro??er Mannes.");
    assertBad("Er erz??hlte von den Leute und den Dingen, die er gesehen hatte.");
    assertBad("Diese Partnerschaft wurde 1989 nach den Massaker auf dem Platz des Himmlischen Friedens eingefroren.");

    assertBad("Das Dach von meine Auto.", "mein Auto", "meine Autos", "meinem Auto");
    assertBad("Das Dach von meinen Auto.", "mein Auto", "meinem Auto", "meinen Autos");

    assertBad("Das Dach mein Autos.", "mein Auto", "meine Autos", "meinen Autos", "meiner Autos", "meines Autos");
    assertBad("Das Dach meinem Autos.", "meine Autos", "meinem Auto", "meinen Autos", "meiner Autos", "meines Autos");

    assertBad("Das Dach meinem gro??en Autos.");
    assertBad("Das Dach mein gro??en Autos.");

    assertBad("Das Klientel der Partei.", "Der Klientel", "Die Klientel");  // gender used to be wrong in Morphy data
    assertGood("Die Klientel der Partei.");

    assertBad("Der Haus ist gro??", "Das Haus", "Dem Haus", "Der H??user");
    assertBad("Aber der Haus ist gro??", "das Haus", "dem Haus", "der H??user");

    assertBad("Ich habe einen Feder gefunden.", "eine Feder", "einer Feder");

    assertGood("Wenn die Gott zugeschriebenen Eigenschaften stimmen, dann...");
    assertGood("Dieses Gr??nkern genannte Getreide ist aber nicht backbar.");
    assertGood("Au??erdem unterst??tzt mich Herr M??ller beim abheften");
    assertGood("Au??erdem unterst??tzt mich Frau M??ller beim abheften");
    assertBad("Der Zustand meiner Gehirns.");

    assertBad("Lebensmittel sind da, um den menschliche K??rper zu ern??hren.");
    assertBad("Geld ist da, um den menschliche ??berleben sicherzustellen.");
    assertBad("Sie hatte das kleinen Kaninchen.");
    assertBad("Frau M??ller hat das wichtigen Dokument gefunden.");
    assertBad("Ich gebe dir ein kleine Kaninchen.");
    assertBad("Ich gebe dir ein kleinen Kaninchen.");
    assertBad("Ich gebe dir ein kleinem Kaninchen.");
    assertBad("Ich gebe dir ein kleiner Kaninchen.");
    //assertBad("Ich gebe dir ein klein Kaninchen.");  // already detected by MEIN_KLEIN_HAUS
    assertGood("Ich gebe dir ein kleines Kaninchen.");

    assertBad("Ich gebe dir das kleinen Kaninchen.");
    assertBad("Ich gebe dir das kleinem Kaninchen.");
    assertBad("Ich gebe dir das kleiner Kaninchen.");
    assertBadWithNoSuggestion("Gepr??gt ist der Platz durch einen 142 Meter hoher Obelisken");
    //assertBad("Ich gebe dir das kleines Kaninchen.");  // already detected by ART_ADJ_SOL
    //assertBad("Ich gebe dir das klein Kaninchen.");  // already detected by MEIN_KLEIN_HAUS
    assertGood("Ich gebe dir das kleine Kaninchen.");
    assertGood("Die Top 3 der Umfrage");
    assertGood("Dein Vorschlag befindet sich unter meinen Top 5.");
    assertGood("Unter diesen rief das gro??en Unmut hervor.");
    assertGood("Bei mir l??ste das Panik aus.");
    assertGood("Sie k??nnen das machen in dem sie die CAD.pdf ??ffnen.");
    assertGood("Ich mache eine Ausbildung zur Junior Digital Marketing Managerin.");

    assertGood("Dann wird das Konsequenzen haben.");
    assertGood("Dann hat das Konsequenzen.");
    assertGood("Sollte das Konsequenzen nach sich ziehen?");
    assertGood("Der Echo Show von Amazon");
    assertGood("Die BVG kommen immer zu sp??t.");
    assertGood("In der Fr??he der Nacht.");
    assertGood("Der TV Steinfurt.");
    assertGood("Ein ID 3 von Volkswagen.");
    assertGood("Der ID.3 von Volkswagen.");
    assertGood("Der ID3 von Volkswagen.");

    assertBad("Hier steht Ihre Text.");
    assertBad("Hier steht ihre Text.");

    assertBad("Ich wei?? nicht mehr, was unser langweiligen Thema war.");
    assertGood("Aber mein Wissen ??ber die Antike ist ausbauf??hig.");
    assertBad("Er ging ins K??che.");
    assertBad("Er ging ans Luft.");
    assertBad("Eine Niereninsuffizienz f??hrt zur St??rungen des Wasserhaushalts.");
    assertBad("Er stieg durchs Fensters.");
    assertBad("Ich habe heute ein Krankenwagen gesehen.");
    // TODO: not yet detected:
    //assertBad("Erst recht wir flei??iges Arbeiter.");
    //assertBad("Erst recht ich flei??iges Arbeiter.");
    //assertBad("Das Dach meine gro??en Autos.");
    //assertBad("Das Dach meinen gro??en Autos.");
    //assertBad("Das Dach meine Autos.");
    //assertBad("Es ist das Haus dem Mann.");
    //assertBad("Das interessiert der M??nner.");
    //assertBad("Das interessiert der Mann.");
    //assertBad("Das geh??rt den Mann."); // detected by DEN_DEM
    //assertBad("Es sind der Frau.");
  }

  @Test
  public void testVieleWenige() throws IOException {
    assertGood("Zusammenschluss mehrerer d??rflicher Siedlungen an einer Furt");
    assertGood("F??r einige markante Szenen");
    assertGood("F??r einige markante Szenen baute Hitchcock ein Schloss.");
    assertGood("Haben Sie viele gl??ckliche Erfahrungen in Ihrer Kindheit gemacht?");
    assertGood("Es gibt viele gute Sachen auf der Welt.");
    assertGood("Viele englische W??rter haben lateinischen Ursprung");
    assertGood("Ein Bericht ??ber Fruchtsaft, einige ??hnliche Erzeugnisse und Fruchtnektar");
    assertGood("Der Typ, der seit einiger Zeit immer wieder hierher kommt.");
    assertGood("Jede Schnittmenge abz??hlbar vieler offener Mengen");
    assertGood("Es kam zur Fusion der genannten und noch einiger weiterer Unternehmen.");
    assertGood("Zu dieser Fragestellung gibt es viele unterschiedliche Meinungen.");
  }

  @Test
  public void testDetNounRuleErrorMessages() throws IOException {
    // check detailed error messages:
    assertBadWithMessage("Das Fahrrads.", "des Kasus");
    assertBadWithMessage("Der Fahrrad.", "des Genus");
    assertBadWithMessage("Das Fahrr??der.", "des Numerus");
    assertBadWithMessage("Die Tischen sind eckig.", "des Kasus");
    assertBadWithMessage("Die Tischen sind eckig.", "und Genus");
  }

  @Test
  public void testRegression() throws IOException {
    JLanguageTool lt = new JLanguageTool(Languages.getLanguageForShortCode("de-DE"));
    // used to be not detected > 1.0.1:
    String str = "Und so.\r\nDie Bier.";
    List<RuleMatch> matches = lt.check(str);
    assertEquals(1, matches.size());
  }

  @Test
  public void testDetAdjNounRule() throws IOException {
    // correct sentences:
    assertGood("Das ist der riesige Tisch.");
    assertGood("Der riesige Tisch ist gro??.");
    assertGood("Die Kanten der der riesigen Tische.");
    assertGood("Den riesigen Tisch mag er.");
    assertGood("Es mag den riesigen Tisch.");
    assertGood("Die Kante des riesigen Tisches.");
    assertGood("Dem riesigen Tisch fehlt was.");
    assertGood("Die riesigen Tische sind gro??.");
    assertGood("Der riesigen Tische wegen.");
    assertGood("An der roten Ampel.");
    assertGood("Dann hat das nat??rlich Nachteile.");
    assertGood("Ihre erste Nr. 1");
    assertGood("Wir bedanken uns bei allen Teams.");
    assertGood("Als Heinrich versuchte, seinen Kandidaten f??r den Mail??nder Bischofssitz durchzusetzen, reagierte der Papst sofort.");
    assertGood("Den neuen Finanzierungsweg wollen sie daher Hand in Hand mit dem Leser gehen.");
    assertGood("Lieber den Spatz in der Hand...");
    assertGood("Wir wollen sein ein einzig Volk von Br??dern");
    assertGood("Eine Zeitreise durch die 68er Revolte");
    assertGood("Ich besitze ein Modell aus der 300er Reihe.");

    // incorrect sentences:
    assertBad("Es sind die riesigen Tisch.");
    //assertBad("Dort, die riesigen Tischs!");    // TODO: error not detected because of comma
    assertBad("Als die riesigen Tischs kamen.");
    assertBad("Als die riesigen Tisches kamen.");
    assertBad("Der riesigen Tisch und so.");
    assertBad("An der roter Ampel.");
    assertBad("An der rote Ampel.");
    assertBad("An der rotes Ampel.");
    assertBad("An der rotem Ampel.");
    assertBad("Er hatte ihn aus dem 1,4 Meter tiefem Wasser gezogen.");
    assertBad("Er hatte ihn aus dem 1,4 Meter tiefem Wasser gezogen.");
    assertBad("Er hatte eine sehr schweren Infektion.");
    assertBad("Ein fast 5 Meter hohem Haus.");
    assertBad("Ein f??nf Meter hohem Haus.");
    assertBad("Es wurden Karavellen eingesetzt, da diese f??r die flachen Gew??ssern geeignet waren.");
    assertBad("Wir bedanken uns bei allem Teams.");
    assertBad("Dabei geht es um das altbekannte Frage der Dynamiken der Eigenbildung..");
    assertBad("Den neue Finanzierungsweg wollen sie daher Hand in Hand mit dem Leser gehen.");
    assertBad("Den neuen Finanzierungsweg wollen sie daher Hand in Hand mit dem Lesern gehen.");
    //assertBad("An der rot Ampel.");
  }

  private void assertGood(String s) throws IOException {
    RuleMatch[] matches = rule.match(lt.getAnalyzedSentence(s));
    assertEquals("Found unexpected match in sentence '" + s + "': " + Arrays.toString(matches), 0, matches.length);
  }

  private void assertBad(String s, String... expectedSuggestions) throws IOException {
    RuleMatch[] matches = rule.match(lt.getAnalyzedSentence(s));
    assertEquals("Did not find one match in sentence '" + s + "'", 1, matches.length);
    if (expectedSuggestions.length > 0) {
      RuleMatch match = matches[0];
      List<String> suggestions = match.getSuggestedReplacements();
      assertThat(suggestions, is(Arrays.asList(expectedSuggestions)));
    }
  }

  private void assertBadWithNoSuggestion(String s) throws IOException {
    RuleMatch[] matches = rule.match(lt.getAnalyzedSentence(s));
    assertEquals("Did not find one match in sentence '" + s + "'", 1, matches.length);
    RuleMatch match = matches[0];
    List<String> suggestions = match.getSuggestedReplacements();
    if (suggestions.size() != 0) {
      fail("Expected 0 suggestions for: " + s + ", got: " + suggestions);
    }
  }

  private void assertBadWithMessage(String s, String expectedErrorSubstring) throws IOException {
    assertEquals(1, rule.match(lt.getAnalyzedSentence(s)).length);
    String errorMessage = rule.match(lt.getAnalyzedSentence(s))[0].getMessage();
    assertTrue("Got error '" + errorMessage + "', expected substring '" + expectedErrorSubstring + "'",
            errorMessage.contains(expectedErrorSubstring));
  }

}
