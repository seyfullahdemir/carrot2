package org.carrot2.clustering.lingo;

import org.apache.mahout.math.matrix.DoubleMatrix2D;
import org.carrot2.matrix.MatrixUtils;

import com.carrotsearch.hppc.BitSet;
import com.carrotsearch.hppc.IntIntOpenHashMap;

public class ExclusiveDefinitionLabelAssigner2 implements ILabelAssigner {

	public static final double similarityThreshold = 0.75;
	
	@Override
	public void assignLabels(LingoProcessingContext context,
			DoubleMatrix2D stemCos, IntIntOpenHashMap filteredRowToStemIndex,
			DoubleMatrix2D phraseCos) {
		DoubleMatrix2D termDocumentMatrix = context.vsmContext.termDocumentMatrix;
		DoubleMatrix2D termAbstractConceptMatrix = context.reducedVsmContext.baseMatrix;
		DoubleMatrix2D coefficientMatrix = context.reducedVsmContext.coefficientMatrix;
		
		DoubleMatrix2D acDMatrix2 = coefficientMatrix.viewDice();
		
		BitSet[] labelDocumentIndices = context.preprocessingContext.allLabels.documentIndices;
		final int [] labelsFeatureIndex = context.preprocessingContext.allLabels.featureIndex;
		 
		BitSet[] abstractConceptDocumentIndices = new BitSet[termAbstractConceptMatrix.columns()];
        final int [] clusterLabelFeatureIndex = new int [termAbstractConceptMatrix.columns()];
        double [] clusterLabelScore = new double [termAbstractConceptMatrix.columns()];

		DoubleMatrix2D abstractConceptTermMatrix = termAbstractConceptMatrix.viewDice();
		

		DoubleMatrix2D abstractConceptdocumentMatrix = coefficientMatrix.viewDice();
		
		MatrixUtils.normalizeColumnL2(abstractConceptdocumentMatrix, null);
		
		for (int i = 0; i < abstractConceptDocumentIndices.length; i++) {
			abstractConceptDocumentIndices[i] = new BitSet();
		}

        setAbstractConceptDocumentIndices(abstractConceptDocumentIndices, abstractConceptdocumentMatrix);
	
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

	private void dumpMatrix(DoubleMatrix2D matrix) {
		int birdenBuyukSayisi = 0;
		for (int r = 0; r < matrix.rows(); r++)
        {
            for (int c = 0; c < matrix.columns(); c++)
            {
                final double currentValue = matrix.getQuick(r, c);
                if(currentValue > 1){
                	birdenBuyukSayisi++;
                }
                System.out.print(currentValue + "\t");
            }
            System.out.println();
        }
		System.out.println("Birden büyük eleman sayısı: " + birdenBuyukSayisi);
	}
	
	private void setAbstractConceptDocumentIndices(BitSet[] abstractConceptDocumentIndices, DoubleMatrix2D normalizedAbstractConceptdocumentMatrix) {
		
		for (int r = 0; r < normalizedAbstractConceptdocumentMatrix.rows(); r++)
        {
            for (int c = 0; c < normalizedAbstractConceptdocumentMatrix.columns(); c++)
            {
                final double currentValue = normalizedAbstractConceptdocumentMatrix.getQuick(r, c);
                if(currentValue >= similarityThreshold){
                	abstractConceptDocumentIndices[r].set(c);
                }
            }
        }
	}
}
