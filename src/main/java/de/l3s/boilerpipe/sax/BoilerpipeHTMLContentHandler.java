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
package de.l3s.boilerpipe.sax;

import java.util.*;
import java.util.regex.Pattern;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastTable;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.labels.LabelAction;
import de.l3s.boilerpipe.util.UnicodeTokenizer;

/**
 * A simple SAX {@link ContentHandler}, used by {@link BoilerpipeSAXInput}. Can
 * be used by different parser implementations, e.g. NekoHTML and TagSoup.
 * 
 * @author Christian Kohlschütter
 */
public class BoilerpipeHTMLContentHandler implements ContentHandler {

	private final Map<String, TagAction> tagActions;
	private String title;

	static final String ANCHOR_TEXT_START = "$\ue00a<";
	static final String ANCHOR_TEXT_END = ">\ue00a$";

	TextBuilder tokenBuffer =TextBuilder.newInstance();
	TextBuilder textBuffer = TextBuilder.newInstance();

	int inBody;
	int inAnchor;
	int inIgnorableElement;

	int tagLevel;
	int blockTagLevel = -1;

	boolean sbLastWasWhitespace;
	private int textElementIdx;

	private final List<TextBlock> textBlocks = FastTable.newInstance();

	private String lastStartTag;
	@SuppressWarnings("unused")
	private String lastEndTag;
	@SuppressWarnings("unused")
	private Event lastEvent;

	private int offsetBlocks;
	private BitSet currentContainedTextElements = new BitSet();

	private boolean flush;
	boolean inAnchorText;

	FastList<FastList<LabelAction>> labelStacks = FastList.newInstance();
    FastList<Integer> fontSizeStack = FastList.newInstance();

	/**
	 * Recycles this instance.
	 */
	public void recycle() {
		tokenBuffer.setLength(0);
		textBuffer.setLength(0);

		inBody = 0;
		inAnchor = 0;
		inIgnorableElement = 0;
		sbLastWasWhitespace = false;
		textElementIdx = 0;

		textBlocks.clear();

		lastStartTag = null;
		lastEndTag = null;
		lastEvent = null;

		offsetBlocks = 0;
		currentContainedTextElements.clear();

		flush = false;
		inAnchorText = false;
	}

	/**
	 * Constructs a {@link BoilerpipeHTMLContentHandler} using the
	 * {@link DefaultTagActionMap}.
	 */
	public BoilerpipeHTMLContentHandler() {
		this(DefaultTagActionMap.INSTANCE);
	}

	/**
	 * Constructs a {@link BoilerpipeHTMLContentHandler} using the given
	 * {@link TagActionMap}.
	 * 
	 * @param tagActions
	 *            The {@link TagActionMap} to use, e.g.
	 *            {@link DefaultTagActionMap}.
	 */
	public BoilerpipeHTMLContentHandler(TagActionMap tagActions) {
		this.tagActions = tagActions;
	}

	// @Override
	public void endDocument() throws SAXException {
		flushBlock();
	}

	// @Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	// @Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		if (!sbLastWasWhitespace) {
			textBuffer.append(' ');
			tokenBuffer.append(' ');
		}
		sbLastWasWhitespace = true;
	}

	// @Override
	public void processingInstruction(String target, String data)
			throws SAXException {
	}

	// @Override
	public void setDocumentLocator(Locator locator) {
	}

	// @Override
	public void skippedEntity(String name) throws SAXException {
	}

	// @Override
	public void startDocument() throws SAXException {
	}

	// @Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}

	// @Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		labelStacks.add(null);

		TagAction ta = tagActions.get(localName);
		if (null != ta) {
			if(ta.changesTagLevel()) {
				tagLevel++;
			}
			flush = ta.start(this, localName, qName, atts) | flush;
		} else {
			tagLevel++;
			flush = true;
		}

		lastEvent = Event.START_TAG;
		lastStartTag = localName;
	}

	// @Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		TagAction ta = tagActions.get(localName);
		if (null != ta) {
			flush = ta.end(this, localName, qName) | flush;
		} else {
			flush = true;
		}
		
		if(null == ta || ta.changesTagLevel()) {
			tagLevel--;
		}
		
		if (flush) {
			flushBlock();
		}

		lastEvent = Event.END_TAG;
		lastEndTag = localName;

		labelStacks.removeLast();
	}

	// @Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		textElementIdx++;

	
		if (flush) {
			flushBlock();
			flush = false;
		}

		if (0 != inIgnorableElement) {
			return;
		}

		char c;
		boolean startWhitespace = false;
		boolean endWhitespace = false;
		if (0 == length) {
			return;
		}

		int end = start + length;
		for (int i = start; i < end; i++) {
			if (Character.isWhitespace(ch[i])) {
				ch[i] = ' ';
			}
		}
		while (start < end) {
			c = ch[start];
			if (' ' == c) {
				startWhitespace = true;
				start++;
				length--;
			} else {
				break;
			}
		}
		while (0 < length) {
			c = ch[start + length - 1];
			if (' ' == c) {
				endWhitespace = true;
				length--;
			} else {
				break;
			}
		}
		if (0 == length) {
			if (startWhitespace || endWhitespace) {
				if (!sbLastWasWhitespace) {
					textBuffer.append(' ');
					tokenBuffer.append(' ');
				}
				sbLastWasWhitespace = true;
			} else {
				sbLastWasWhitespace = false;
			}
			lastEvent = Event.WHITESPACE;
			return;
		}
		if (startWhitespace) {
			if (!sbLastWasWhitespace) {
				textBuffer.append(' ');
				tokenBuffer.append(' ');
			}
		}
		
		if (-1 == blockTagLevel) {
			blockTagLevel = tagLevel;
		}

		textBuffer.append(ch, start, length);
		tokenBuffer.append(ch, start, length);
		if (endWhitespace) {
			textBuffer.append(' ');
			tokenBuffer.append(' ');
		}

		sbLastWasWhitespace = endWhitespace;
		lastEvent = Event.CHARACTERS;

		currentContainedTextElements.set(textElementIdx);
	}

	List<TextBlock> getTextBlocks() {
		return textBlocks;
	}

	public void flushBlock() {
        switch (inBody) {
            case 0:
                if ("TITLE".equalsIgnoreCase(lastStartTag) && 0 == inBody) {
                    setTitle(tokenBuffer.toString().trim());
                }
                textBuffer.setLength(0);
                tokenBuffer.setLength(0);
                return;
        }

		int length = tokenBuffer.length();
		switch (length) {
		case 0:
			return;
		case 1:
			if (sbLastWasWhitespace) {
				textBuffer.setLength(0);
				tokenBuffer.setLength(0);
				return;
			}
		}
        Iterator<CharSequence> tokens = UnicodeTokenizer.tokenize(tokenBuffer);

		int numWords = 0;
		int numLinkedWords = 0;
		int numWrappedLines = 0;
		int currentLineLength = -1; // don't count the first space
		int maxLineLength = 80;
		int numTokens = 0;
		int numWordsCurrentLine = 0;
        while (tokens.hasNext()) {
            CharSequence token = tokens.next();

			if (ANCHOR_TEXT_START.equals(token)) {
				inAnchorText = true;
			} else if (ANCHOR_TEXT_END.equals(token)) {
				inAnchorText = false;
			} else if (isWord(token)) {
				numTokens++;
				numWords++;
				numWordsCurrentLine++;
				if (inAnchorText) {
					numLinkedWords++;
				}
				int tokenLength = token.length();
				currentLineLength += tokenLength + 1;
				if (currentLineLength > maxLineLength) {
					numWrappedLines++;
					currentLineLength = tokenLength;
					numWordsCurrentLine = 1;
				}
			} else {
				numTokens++;
			}
		}
        if (0 != numTokens) {
            int numWordsInWrappedLines;
            switch (numWrappedLines) {
                case 0:
                    numWordsInWrappedLines = numWords;
                    numWrappedLines = 1;
                    break;
                default:
                    numWordsInWrappedLines = numWords - numWordsCurrentLine;
                    break;
            }

            TextBlock tb = new TextBlock(textBuffer.toString().trim(),
                    currentContainedTextElements, numWords, numLinkedWords,
                    numWordsInWrappedLines, numWrappedLines, offsetBlocks);
            currentContainedTextElements = new BitSet();

            offsetBlocks++;

            textBuffer.setLength(0);
            tokenBuffer.setLength(0);

            tb.setTagLevel(blockTagLevel);
            addTextBlock(tb);
            blockTagLevel = -1;
        }
	}

	protected void addTextBlock(TextBlock tb) {

		for (Integer l : fontSizeStack)
            if (null != l) {
                tb.addLabel("font-" + l);
                break;
            }
		for ( List<LabelAction> labelStack : labelStacks)
            if (null != labelStack) for (LabelAction labels : labelStack) if (null != labels) labels.addTo(tb);

		textBlocks.add(tb);
	}

	private static final Pattern PAT_VALID_WORD_CHARACTER = Pattern
			.compile("[\\p{L}\\p{Nd}\\p{Nl}\\p{No}]");

	private static boolean isWord(CharSequence token) {
		return PAT_VALID_WORD_CHARACTER.matcher(token).find();
	}

	static private enum Event {
		START_TAG, END_TAG, CHARACTERS, WHITESPACE
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String s) {
        if (null != s && 0 != s.length()) {
            title = s;
        }
	}

	/**
	 * Returns a {@link TextDocument} containing the extracted {@link TextBlock}
	 * s. NOTE: Only call this after parsing.
	 * 
	 * @return The {@link TextDocument}
	 */
	public TextDocument toTextDocument() {
		// just to be sure
		flushBlock();

		return new TextDocument(getTitle(), getTextBlocks());
	}

	public void addWhitespaceIfNecessary() {
		if (!sbLastWasWhitespace) {
			tokenBuffer.append(' ');
			textBuffer.append(' ');
			sbLastWasWhitespace = true;
		}
	}

	public void addLabelAction(LabelAction la)
			throws IllegalStateException {
		FastList<LabelAction> labelStack = labelStacks.getLast();
		if (null == labelStack) {
			labelStack = FastList.newInstance();
			labelStacks.removeLast();
			labelStacks.add(labelStack);
		}
		labelStack.add(la);
	}
}
