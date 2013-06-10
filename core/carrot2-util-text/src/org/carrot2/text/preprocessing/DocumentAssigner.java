
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2012, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package org.carrot2.text.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.carrot2.core.attribute.Processing;
import org.carrot2.text.analysis.TokenTypeUtils;
import org.carrot2.text.preprocessing.PreprocessingContext.AllLabels;
import org.carrot2.util.attribute.Attribute;
import org.carrot2.util.attribute.AttributeLevel;
import org.carrot2.util.attribute.Bindable;
import org.carrot2.util.attribute.DefaultGroups;
import org.carrot2.util.attribute.Group;
import org.carrot2.util.attribute.Input;
import org.carrot2.util.attribute.Label;
import org.carrot2.util.attribute.Level;
import org.carrot2.util.attribute.constraint.IntRange;

import com.carrotsearch.hppc.BitSet;
import com.carrotsearch.hppc.IntArrayList;
import com.google.common.collect.Lists;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

/**
 * Assigns document to label candidates. For each label candidate from
 * {@link AllLabels#featureIndex} an {@link BitSet} with the assigned documents is
 * constructed. The assignment algorithm is rather simple: in order to be assigned to a
 * label, a document must contain at least one occurrence of each non-stop word from the
 * label.
 * <p>
 * This class saves the following results to the {@link PreprocessingContext} :
 * <ul>
 * <li>{@link AllLabels#documentIndices}</li>
 * </ul>
 * <p>
 * This class requires that {@link Tokenizer}, {@link CaseNormalizer},
 * {@link StopListMarker}, {@link PhraseExtractor} and {@link LabelFilterProcessor} be
 * invoked first.
 */
@Bindable(prefix = "DocumentAssigner")
public class DocumentAssigner
{
    /**
     * Only exact phrase assignments. Assign only documents that contain the label in its
     * original form, including the order of words. Enabling this option will cause less
     * documents to be put in clusters, which result in higher precision of assignment,
     * but also a larger "Other Topics" group. Disabling this option will cause more
     * documents to be put in clusters, which will make the "Other Topics" cluster
     * smaller, but also lower the precision of cluster-document assignments.
     */
	static {
		System.setProperty("wordnet.database.dir", "C:\\Program Files (x86)\\WordNet\\2.1\\dict");
	}

	private static final WordNetDatabase wordnet = WordNetDatabase.getFileInstance();

	
    @Input
    @Processing
    @Attribute
    @Label("Exact phrase assignment")
    @Level(AttributeLevel.MEDIUM)
    @Group(DefaultGroups.PREPROCESSING)
    public boolean exactPhraseAssignment = false;

    /**
     * Determines the minimum number of documents in each cluster.
     */
    @Input
    @Processing
    @Attribute
    @IntRange(min = 1, max = 100)
    @Label("Minimum cluster size")
    @Level(AttributeLevel.MEDIUM)
    @Group(DefaultGroups.PREPROCESSING)
    public int minClusterSize = 2;

    /**
     * Assigns document to label candidates.
     */
    public void assign(PreprocessingContext context)
    {
        final int [] labelsFeatureIndex = context.allLabels.featureIndex;
        final int [][] stemsTfByDocument = context.allStems.tfByDocument;
        final int [] wordsStemIndex = context.allWords.stemIndex;
        final char [][] image = context.allWords.image;
        final short [] wordsTypes = context.allWords.type;
        final int [][] phrasesTfByDocument = context.allPhrases.tfByDocument;
        final int [][] phrasesWordIndices = context.allPhrases.wordIndices;
        final int wordCount = wordsStemIndex.length;
        final int documentCount = context.documents.size();

        final BitSet [] labelsDocumentIndices = new BitSet [labelsFeatureIndex.length];

        
        Map<String, Integer> stemImageStemIndexMap = buildStemImageStemIndexMap(context);
        
        
        for (int i = 0; i < labelsFeatureIndex.length; i++)
        {
            BitSet documentIndices = new BitSet(documentCount);

            final int featureIndex = labelsFeatureIndex[i];
            
            
            //if the label is a word
            if (featureIndex < wordCount)
            {
            	//featureIndexin işaret ettiği wordün steminin tf by document dizisini parametre geçti.
                
            	char[] wordString = image[featureIndex];
             	List<Integer> indicesOfStemsAllSynonymStemsWithItself = getIndicesOfStemsAllSynonymStemsWithItself(stemImageStemIndexMap, wordString, wordsStemIndex[featureIndex], context);
          
             	documentIndices = addTfByDocumentToBitSet(stemsTfByDocument, 
             			indicesOfStemsAllSynonymStemsWithItself, 
             			documentCount);
             	
            }
            else
            {
                final int phraseIndex = featureIndex - wordCount;
                if (exactPhraseAssignment)
                {
                    addTfByDocumentToBitSet(documentIndices,
                        phrasesTfByDocument[phraseIndex]);
                }
                else
                {
                    final int [] wordIndices = phrasesWordIndices[phraseIndex];
                    boolean firstAdded = false;

                    List<BitSet> documentIndicesList = new ArrayList<BitSet>();
                    
                    for (int j = 0; j < wordIndices.length; j++)
                    {
                        final int wordIndex = wordIndices[j];
                        //yalnızca stop words dışı kelimeler incelenir.
                        if (!TokenTypeUtils.isCommon(wordsTypes[wordIndex]))
                        {
                        	char[] wordString = image[wordIndex];
							List<Integer> indicesOfStemsAllSynonymStemsWithItself = getIndicesOfStemsAllSynonymStemsWithItself(stemImageStemIndexMap, wordString, wordsStemIndex[wordIndex], context );
                        	documentIndices = addTfByDocumentToBitSet(stemsTfByDocument, indicesOfStemsAllSynonymStemsWithItself, documentCount);
                        	documentIndicesList.add(documentIndices);
                           
                        }
                    }
                    documentIndices = intersectDocuments(documentIndicesList, documentCount);
                }
            }

            labelsDocumentIndices[i] = documentIndices;
        }

        // Filter out labels that do not meet the minimum cluster size
        if (minClusterSize > 1)
        {
            final IntArrayList newFeatureIndex = new IntArrayList(
                labelsFeatureIndex.length);
            final ArrayList<BitSet> newDocumentIndices = Lists
                .newArrayListWithExpectedSize(labelsFeatureIndex.length);

            for (int i = 0; i < labelsFeatureIndex.length; i++)
            {
                if (labelsDocumentIndices[i].cardinality() >= minClusterSize)
                {
                    newFeatureIndex.add(labelsFeatureIndex[i]);
                    newDocumentIndices.add(labelsDocumentIndices[i]);
                }
            }
            context.allLabels.documentIndices = newDocumentIndices
                .toArray(new BitSet [newDocumentIndices.size()]);
            context.allLabels.featureIndex = newFeatureIndex.toArray();
            LabelFilterProcessor.updateFirstPhraseIndex(context);
        }
        else
        {
            context.allLabels.documentIndices = labelsDocumentIndices;
        }
    }

    //bu tfByDocument arrayi parametre geçilmiş stemi / phrase'i içeren dokümanları set et.
    private static void addTfByDocumentToBitSet(final BitSet documentIndices,
        final int [] tfByDocument)
    {
        for (int j = 0; j < tfByDocument.length / 2; j++)
        {
            documentIndices.set(tfByDocument[j * 2]);
        }
    }
    

    private static BitSet addTfByDocumentToBitSet(final int [][] stemsTfByDocument,
            List<Integer> stemIndices, int documentCount){
    	
    	BitSet documentIndices = new BitSet(documentCount);
    	documentIndices.clear();
    	
    	for (Integer integer : stemIndices) {
			BitSet temp = new BitSet(documentCount);
			addTfByDocumentToBitSet(temp, stemsTfByDocument[integer.intValue()]);
			documentIndices.or(temp);
		}
    	return documentIndices;
    }
    
    private static BitSet intersectDocuments(List<BitSet> listOfDocumentIndicesForOneWord, int documentCount){
		BitSet documentIndices = new BitSet(documentCount);
    	documentIndices.set(0, documentCount-1);
    	
    	for (BitSet bitSet : listOfDocumentIndicesForOneWord) {
			documentIndices.and(bitSet);
		}
    	
    	return documentIndices;
    }
    
    
    private List<Integer> getIndicesOfStemsAllSynonymStemsWithItself(Map<String, Integer> stemImageStemIndexMap, char[] wordString, int ownIndex, PreprocessingContext context){
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

    private List<String> getImagesOfStemsAllSynonymStems(final char[] wordString, PreprocessingContext context){
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
    
    private Map<String, Integer> buildStemImageStemIndexMap(PreprocessingContext context){
		
    	Map<String, Integer> stemImageStemIndexMap = new HashMap<String, Integer>();
    	
    	char[][] image = context.allStems.image;
		int index = 0;
		for (char[] stem : image) {
			stemImageStemIndexMap.put(String.valueOf(stem), index);
			index++;
		}
		
		return stemImageStemIndexMap;
    }
 }
