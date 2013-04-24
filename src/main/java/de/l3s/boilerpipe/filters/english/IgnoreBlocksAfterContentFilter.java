/**
 * boilerpipe
 *
 * Copyright (c) 2009,2010 Christian Kohlschütter
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
package de.l3s.boilerpipe.filters.english;

import java.util.Iterator;
import java.util.List;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.labels.DefaultLabels;

/**
 * Marks all blocks as "non-content" that occur after blocks that have been
 * marked {@link DefaultLabels#INDICATES_END_OF_TEXT}. These marks are ignored
 * unless a minimum number of words in content blocks occur before this mark (default: 60).
 * This can be used in conjunction with an upstream {@link TerminatingBlocksFinder}.
 * 
 * @author Christian Kohlschütter
 * @see TerminatingBlocksFinder
 */
public class IgnoreBlocksAfterContentFilter extends HeuristicFilterBase implements BoilerpipeFilter {
    private int minNumWords;

    public IgnoreBlocksAfterContentFilter(int minNumWords) {
        this.minNumWords = minNumWords;
    }

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        boolean changes = false;

        int numWords = 0;
        boolean foundEndOfText = false;
        List<TextBlock> textBlocks = doc.getTextBlocks();
        for (int i = 0, textBlocksSize = textBlocks.size(); i < textBlocksSize; i++) {
            TextBlock block = textBlocks.get(i);
            boolean endOfText = block
                    .hasLabel(DefaultLabels.INDICATES_END_OF_TEXT);
            if (block.isContent()) {
                numWords += getNumFullTextWords(block);
            }
            if (endOfText && numWords >= minNumWords) {
                foundEndOfText = true;
            }
            if (foundEndOfText) {
                changes = true;
                block.setIsContent(false);
            }
        }

        return changes;
    }    
}
