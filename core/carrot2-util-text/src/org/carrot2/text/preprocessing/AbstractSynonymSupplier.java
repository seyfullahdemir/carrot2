package org.carrot2.text.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class AbstractSynonymSupplier implements ISynonymSupplier {

	
	@Override
    public List<Integer> getIndicesOfStemsAllSynonymStemsWithItself(Map<String, Integer> stemImageStemIndexMap, char[] wordString, int ownIndex, PreprocessingContext context){
    	List<String> imagesOfStemsAllSynonymStemsWithItself = getImagesOfStemsAllSynonymStems(wordString, context);
				
    	List<Integer> indicesOfStemsAllSynonymStemsWithItself = new ArrayList<Integer>();
    	indicesOfStemsAllSynonymStemsWithItself.add(ownIndex);
		for (String stemImage : imagesOfStemsAllSynonymStemsWithItself) {
			Integer stemIndex = stemImageStemIndexMap.get(stemImage);
			//if the stem really contained in the document list
			if(stemIndex != null){				
				indicesOfStemsAllSynonymStemsWithItself.add(stemIndex);
			}
		}
		
		return indicesOfStemsAllSynonymStemsWithItself;
    }


}
