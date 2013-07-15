package org.carrot2.clustering.lingo;

import org.carrot2.text.vsm.ITermWeighting;

public class ExtendedClusterBuilder extends ClusterBuilder {

	@Override
	void buildLabels(LingoProcessingContext context,
			ITermWeighting termWeighting) {
		labelAssigner = new ExclusiveDefinitionLabelAssigner1();
		labelAssigner.assignLabels(context, null, null, null);
	}
}
