package org.carrot2.clustering.lingo;

import org.apache.mahout.math.function.Functions;
import org.apache.mahout.math.matrix.DoubleMatrix2D;
import org.carrot2.matrix.MatrixUtils;

import com.carrotsearch.hppc.BitSet;
import com.carrotsearch.hppc.IntIntOpenHashMap;

public class ExclusiveDefinitionLabelAssigner1 implements ILabelAssigner {

	@Override
	public void assignLabels(LingoProcessingContext context,
			DoubleMatrix2D stemCos, IntIntOpenHashMap filteredRowToStemIndex,
			DoubleMatrix2D phraseCos) {
		
		DoubleMatrix2D termDocumentMatrix = context.vsmContext.termDocumentMatrix;
		DoubleMatrix2D termAbstractConceptMatrix = context.reducedVsmContext.baseMatrix;
		DoubleMatrix2D coefficientMatrix = context.reducedVsmContext.coefficientMatrix;
		BitSet[] labelDocumentIndices = context.preprocessingContext.allLabels.documentIndices;
		final int [] labelsFeatureIndex = context.preprocessingContext.allLabels.featureIndex;
		 
		BitSet[] abstractConceptDocumentIndices = new BitSet[termAbstractConceptMatrix.columns()];
        final int [] clusterLabelFeatureIndex = new int [termAbstractConceptMatrix.columns()];
        double [] clusterLabelScore = new double [termAbstractConceptMatrix.columns()];

		DoubleMatrix2D abstractConceptTermMatrix = termAbstractConceptMatrix.viewDice();
		

		DoubleMatrix2D abstractConceptdocumentMatrix = coefficientMatrix.viewDice();
		
        int[] includedAbstractConceptIndices = new int[abstractConceptdocumentMatrix.columns()];
		double[] includedAbstractConceptScores = new double[abstractConceptdocumentMatrix.columns()];
		
		for (int i = 0; i < abstractConceptDocumentIndices.length; i++) {
			abstractConceptDocumentIndices[i] = new BitSet();
		}

		MatrixUtils.maxInColumns(abstractConceptdocumentMatrix, includedAbstractConceptIndices,
				includedAbstractConceptScores, Functions.ABS);
 
		for (int i = 0; i < includedAbstractConceptIndices.length; i++) {			
			abstractConceptDocumentIndices[includedAbstractConceptIndices[i]].set(i);
		}
		
		 
		for (int i = 0; i < abstractConceptDocumentIndices.length; i++) {
			long maxCommonDocuments = -1L;
			int maxLabelIndex = 0;
			
			BitSet abstractConceptsDocuments = abstractConceptDocumentIndices[i];
			for (int j = 0; j < labelDocumentIndices.length; j++) {
				BitSet temp = new BitSet(abstractConceptsDocuments.size());
				temp.clear();
				temp.or(abstractConceptsDocuments);
				BitSet labelsDocuments = labelDocumentIndices[j];
				
				temp.and(labelsDocuments);
				long commonDocumentsCount = temp.cardinality();
				if(commonDocumentsCount > maxCommonDocuments){
					maxLabelIndex = j;
					maxCommonDocuments = commonDocumentsCount;
				}
			}
			clusterLabelFeatureIndex[i] =  labelsFeatureIndex[maxLabelIndex];
			clusterLabelScore[i] = maxCommonDocuments;
		}
		
        context.clusterLabelFeatureIndex = clusterLabelFeatureIndex;
        context.clusterLabelScore = clusterLabelScore;

	}
}
