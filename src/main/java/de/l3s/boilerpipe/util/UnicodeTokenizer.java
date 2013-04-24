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
package de.l3s.boilerpipe.util;

import java.nio.Buffer;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Tokenizes text according to Unicode word boundaries and strips off non-word
 * characters.
 * 
 * @author Christian Kohlschütter
 */
public class UnicodeTokenizer {
    private static final Pattern PAT_WORD_BOUNDARY = Pattern.compile("\\b");
    private static final Pattern PAT_NOT_WORD_BOUNDARY = Pattern
            .compile("[\u2063]*([\"'\\.,\\!\\@\\-\\:\\;\\$\\?\\(\\)/])[\u2063]*");
                             final static      char[]a=
                                              "\"'.,!@-:;$?()".toCharArray();

    /**
     * Tokenizes the text and returns an array of tokens.
     * 
     *
     * @param text The text
     * @return The tokens
     */
    public static Iterator<CharSequence> tokenize(final CharSequence text) {


        return new Iterator<CharSequence>() {
            CharBuffer buffer = CharBuffer.wrap(PAT_NOT_WORD_BOUNDARY.matcher(PAT_WORD_BOUNDARY.matcher(text).replaceAll("\u2063")).replaceAll("$1").trim());

            @Override
           public boolean hasNext() {

               return buffer!=null&&buffer.hasRemaining() ;
           }

           @Override
           public CharSequence next() {
               buffer.mark();
               boolean first = true;

               while(buffer.hasRemaining()) {

                   switch (buffer.get()) {
                       case '\u2063':
                       case ' ':
                           if (first) {
                               buffer.mark();
                               continue;
                           }else
                           {
                               int i = buffer.position()  ;
                               int limit = buffer.limit();
                               try{

                               return ((CharBuffer) buffer .reset().limit(i-1)).slice();
                               }finally {
                                   buffer.limit(limit).position( i  );
                               }
                           }
                       default:
                            first=false;
                   }
               }

               CharSequence slice=(CharSequence)buffer.reset();
               buffer=null;

               return slice;
           }



           @Override
           public void remove() {

           }
       };

    }
}
