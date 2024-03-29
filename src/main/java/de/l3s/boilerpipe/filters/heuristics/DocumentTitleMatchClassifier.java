/**
 * boilerpipe
 *
 * Copyright (c) 2009 Christian Kohlschütter
 *
 * The author licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.l3s.boilerpipe.filters.heuristics;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.labels.DefaultLabels;

/**
 * Marks {@link TextBlock}s which contain parts of the HTML
 * {@code &lt;TITLE&gt;} tag, using some heuristics which are quite
 * specific to the news domain.
 *
 * @author Christian Kohlschütter
 */
public class DocumentTitleMatchClassifier implements BoilerpipeFilter {

	private final Set<String> potentialTitles;

	public DocumentTitleMatchClassifier(String title) {
        if (title != null) {

            title = title.replace('\u00a0', ' ');
            title = title.replace("'", "");

            title = title.trim().toLowerCase();

if (title.length() != 0) {
potentialTitles = new HashSet<>();

potentialTitles.add(title);

String p;

p = getLongestPart(title, "[ ]*[\\|»|-][ ]*");
if (p != null) {
potentialTitles.add(p);
}
p = getLongestPart(title, "[ ]*[\\|»|:][ ]*");
if (p != null) {
potentialTitles.add(p);
}
p = getLongestPart(title, "[ ]*[\\|»|:\\(\\)][ ]*");
if (p != null) {
potentialTitles.add(p);
}
p = getLongestPart(title, "[ ]*[\\|»|:\\(\\)\\-][ ]*");
if (p != null) {
potentialTitles.add(p);
}
p = getLongestPart(title, "[ ]*[\\|»|,|:\\(\\)\\-][ ]*");
if (p != null) {
potentialTitles.add(p);
}
p = getLongestPart(title, "[ ]*[\\|»|,|:\\(\\)\\-\u00a0][ ]*");
if (p != null) {
potentialTitles.add(p);
}

addPotentialTitles(potentialTitles, title, "[ ]+[\\|][ ]+", 4);
addPotentialTitles(potentialTitles, title, "[ ]+[\\-][ ]+", 4);

potentialTitles.add(title.replaceFirst(" - [^\\-]+$", ""));
potentialTitles.add(title.replaceFirst("^[^\\-]+ - ", ""));
} else {
potentialTitles = null;
}
} else {
potentialTitles = null;
}
    }

	public Set<String> getPotentialTitles() {
		return potentialTitles;
	}
	
	private void addPotentialTitles(Set<String> potentialTitles, String title, String pattern, int minWords) {
		String[] parts = title.split(pattern);
        if (parts.length != 1) {
            for (String p : parts) {
                if (!p.contains(".com")) {

                    int numWords = p.split("[\b ]+").length;
                    if (numWords >= minWords) {
                        potentialTitles.add(p);
                    }
                }
            }
        }
	}

	private String getLongestPart(String title, String pattern) {
		String[] parts = title.split(pattern);
        if (parts.length != 1) {
            int longestNumWords = 0;
            String longestPart = "";
            for (int i = 0, partsLength = parts.length; i < partsLength; i++) {
                String p = parts[i];
                if (p.contains(".com")) {
                    continue;
                }
                int numWords = p.split("[\b ]+").length;
                if (numWords > longestNumWords || p.length() > longestPart.length()) {
                    longestNumWords = numWords;
                    longestPart = p;
                }
            }
            return longestPart.length() == 0 ? null : longestPart.trim();
        }
        return null;
    }
	
	private static final Pattern PAT_REMOVE_CHARACTERS = Pattern.compile("[\\?\\!\\.\\-\\:]+");

	public boolean process(TextDocument doc)
			throws BoilerpipeProcessingException {
        if (potentialTitles != null) {
            boolean changes = false;

            for (TextBlock tb : doc.getTextBlocks()) {
                String text = tb.getText();

                text = text.replace('\u00a0', ' ');
                text = text.replace("'", "");

                text = text.trim().toLowerCase();

                if (potentialTitles.contains(text)) {
                    tb.addLabel(DefaultLabels.TITLE);
                    changes = true;
                    break;
                }

                text = PAT_REMOVE_CHARACTERS.matcher(text).replaceAll("").trim();
                if (potentialTitles.contains(text)) {
                    tb.addLabel(DefaultLabels.TITLE);
                    changes = true;
                    break;
                }
            }
            return changes;
        }
        return false;
    }

}
