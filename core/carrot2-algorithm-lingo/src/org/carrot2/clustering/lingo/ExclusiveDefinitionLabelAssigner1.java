package org.carrot2.clustering.lingo;

import org.apache.mahout.math.matrix.DoubleMatrix2D;

import com.carrotsearch.hppc.IntIntOpenHashMap;

public class ExclusiveDefinitionLabelAssigner1 implements ILabelAssigner {

	@Override
	public void assignLabels(LingoProcessingContext context,
			DoubleMatrix2D stemCos, IntIntOpenHashMap filteredRowToStemIndex,
			DoubleMatrix2D phraseCos) {
		DoubleMatrix2D termDocumentMatrix = context.vsmContext.termDocumentMatrix;
		DoubleMatrix2D documentTermMatrix = termDocumentMatrix.viewDice();
		DoubleMatrix2D termAbstractConceptMatrix = context.reducedVsmContext.baseMatrix;
		
		DoubleMatrix2D documentAbstractConceptMatrix = documentTermMatrix.zMult(termAbstractConceptMatrix, null, 1, 0, false, false);
		
		System.out.println(documentAbstractConceptMatrix);
		 
	}
}
