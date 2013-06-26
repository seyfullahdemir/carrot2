package org.carrot2.text.preprocessing;

import java.util.List;
import java.util.Map;

public interface ISynonymSupplier {

	List<String> getImagesOfStemsAllSynonymStems(char[] wordString, PreprocessingContext context);

	List<Integer> getIndicesOfStemsAllSynonymStemsWithItself(
			Map<String, Integer> stemImageStemIndexMap, char[] wordString,
			int ownIndex, PreprocessingContext context);

	
}
