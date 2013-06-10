package org.carrot2.text.preprocessing;

import org.carrot2.text.linguistic.IStemmer;
import org.carrot2.text.util.MutableCharArray;
import org.carrot2.util.CharArrayUtils;

public final class StemUtil {


	public static char[] getStem(final char [] word, PreprocessingContext context){
		
		final IStemmer stemmer = context.language.getStemmer();
		
        final MutableCharArray mutableCharArray = new MutableCharArray(CharArrayUtils.EMPTY_ARRAY);
        char [] buffer = new char [128];
        char [] stemImage;
        
        if (buffer.length < word.length) buffer = new char [word.length];

        final boolean different = CharArrayUtils.toLowerCase(word, buffer);

        mutableCharArray.reset(buffer, 0, word.length);
        final CharSequence stemmed = stemmer.stem(mutableCharArray);
        if (stemmed != null)
        {
            mutableCharArray.reset(stemmed);
            stemImage = context.intern(mutableCharArray);
        }
        else
        {
            // We need to put the original word here, otherwise, we wouldn't be able
            // to compute frequencies for stems.
            if (different)
                stemImage = context.intern(mutableCharArray);
            else
                stemImage = word;
        }

		return stemImage;
				
	}

}
