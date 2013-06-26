package org.carrot2.text.preprocessing;

import java.util.ArrayList;
import java.util.List;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class WordnetSynonymSupplier extends AbstractSynonymSupplier {


	static {
		System.setProperty("wordnet.database.dir", "C:\\Program Files (x86)\\WordNet\\2.1\\dict");
	}

	private static final WordNetDatabase wordnet = WordNetDatabase.getFileInstance();

	@Override
    public List<String> getImagesOfStemsAllSynonymStems(final char[] wordString, PreprocessingContext context){
       	Synset[] synsets = wordnet.getSynsets(String.valueOf(wordString));
    	List<String> synonymList = new ArrayList<String>();
    	for (Synset synset : synsets) {
			String[] wordForms = synset.getWordForms();
			for (String synonymWord : wordForms) {
				char[] stem = StemUtil.getStem(synonymWord.toCharArray(), context);
				synonymList.add(String.valueOf(stem));
			}
		}
		return synonymList;
    }


}
