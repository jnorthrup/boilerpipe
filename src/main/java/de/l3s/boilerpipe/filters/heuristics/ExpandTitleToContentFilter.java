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

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.labels.DefaultLabels;

import java.util.List;

/**
 * Marks all {@link TextBlock}s "content" which are between the headline and the part that
 * has already been marked content, if they are marked {@link DefaultLabels#MIGHT_BE_CONTENT}.
 * 
 * This filter is quite specific to the news domain.
 * 
 * @author Christian Kohlschütter
 */
public class ExpandTitleToContentFilter implements BoilerpipeFilter {

    public boolean process(TextDocument doc) {
        int i = 0;
        int title = -1;
        int contentStart = -1;
        List<TextBlock> textBlocks = doc.getTextBlocks();
        for (int i2 = 0, textBlocksSize = textBlocks.size(); i2 < textBlocksSize; i2++) {
            TextBlock tb = textBlocks.get(i2);
            if (-1 == contentStart && tb.hasLabel(DefaultLabels.TITLE)) {
                title = i;
                contentStart = -1;
            }
            if (-1 == contentStart && tb.isContent()) {
                contentStart = i;
            }

            i++;
        }

        if (contentStart > title && -1 != title) {
            boolean changes = false;
            List<TextBlock> subList = doc.getTextBlocks().subList(title, contentStart);
            for (int i1 = 0, subListSize = subList.size(); i1 < subListSize; i1++) {
                TextBlock tb = subList.get(i1);
                if (tb.hasLabel(DefaultLabels.MIGHT_BE_CONTENT)) {
                    changes = tb.setIsContent(true) || changes;
                }
            }
            return changes;
        }
        return false;
    }

}
