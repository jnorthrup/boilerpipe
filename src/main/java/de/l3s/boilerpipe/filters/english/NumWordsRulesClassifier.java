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
package de.l3s.boilerpipe.filters.english;

import java.util.List;
import java.util.ListIterator;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;

import static de.l3s.boilerpipe.document.TextBlock.EMPTY_START;

/**
 * Classifies {@link TextBlock}s as content/not-content through rules that have
 * been determined using the C4.8 machine learning algorithm, as described in
 * the paper "Boilerplate Detection using Shallow Text Features" (WSDM 2010),
 * particularly using number of words per block and link density per block.
 * 
 * @author Christian Kohlschütter
 */
public class NumWordsRulesClassifier implements BoilerpipeFilter {

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        List<TextBlock> textBlocks = doc.getTextBlocks();
        boolean hasChanges = false;

        ListIterator<TextBlock> it = textBlocks.listIterator();
        if (it.hasNext()) {
            TextBlock prevBlock = EMPTY_START;
            TextBlock currentBlock = it.next();
            TextBlock nextBlock = it.hasNext() ? it.next() : EMPTY_START;

            hasChanges = classify(prevBlock, currentBlock, nextBlock) || hasChanges;

            if (nextBlock != EMPTY_START) {
                while (it.hasNext()) {
                    prevBlock = currentBlock;
                    currentBlock = nextBlock;
                    nextBlock = it.next();
                    hasChanges = classify(prevBlock, currentBlock, nextBlock) || hasChanges;
                }
                prevBlock = currentBlock;
                currentBlock = nextBlock;
                nextBlock = EMPTY_START;
                hasChanges = classify(prevBlock, currentBlock, nextBlock) || hasChanges;
            }

            return hasChanges;
        }
        return false;
    }

    protected boolean classify(TextBlock prev, TextBlock curr,
            TextBlock next) {

        int numWords = next.getNumWords();
        int numWords1 = curr.getNumWords();
        float linkDensity = curr.getLinkDensity();
        return curr.setIsContent(linkDensity <= 0.333333 && (prev.getLinkDensity() <= 0.555556 ? numWords1 > 16 || numWords > 15 || prev.getNumWords() > 4 : numWords1 > 40 || numWords > 17));
    }

}
