
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2006, Dawid Weiss, Stanisław Osiński.
 * Portions (C) Contributors listed in "carrot2.CONTRIBUTORS" file.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package com.dawidweiss.carrot.input.localcache;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import com.dawidweiss.carrot.core.local.*;
import com.dawidweiss.carrot.core.local.clustering.RawDocument;
import com.dawidweiss.carrot.core.local.impl.DocumentsConsumerOutputComponent;

/**
 * Tests some aspects of the cache input component.
 * 
 * @author Dawid Weiss
 * @version $Revision$
 */
public class RemoteCacheAccessLocalInputComponentTest extends TestCase {

	public RemoteCacheAccessLocalInputComponentTest() {
		super();
	}

	public RemoteCacheAccessLocalInputComponentTest(String arg0) {
		super(arg0);
	}

	protected LocalControllerBase setUpController(LocalComponentFactory inputFactory) throws Exception {
		LocalControllerBase controller;
		
        // Some output component
        LocalComponentFactory outputFactory = new LocalComponentFactory()
        {
            public LocalComponent getInstance()
            {
                return new DocumentsConsumerOutputComponent();
            }
        };

        // Register with the controller
        controller = new LocalControllerBase();
        controller.addLocalComponentFactory("output", outputFactory);

        controller.addLocalComponentFactory("input", inputFactory);

        // Create and register the process
        LocalProcessBase process = new LocalProcessBase();
        process.setInput("input");
        process.setOutput("output");
        controller.addProcess("testprocess", process);
        
        return controller;
	}
	

	public void testDirectFileQuery() throws Exception {
        // ODP input component factory
        LocalComponentFactory inputFactory = new LocalComponentFactory()
        {
            public LocalComponent getInstance()
            {
                return new RemoteCacheAccessLocalInputComponent();
            }
        };

        LocalControllerBase controller = setUpController( inputFactory );

        String query = "file:cached" + File.separator + "sample-cached.gz";
        List results = (List) controller.query("testprocess", query, new HashMap()).getQueryResult();

        // the results should contain some documents.
        assertTrue( results.size() > 0);
	}


	public void testStoreDumpQuery() throws Exception {
        // ODP input component factory
        LocalComponentFactory inputFactory = new LocalComponentFactory()
        {
        	private CachedQueriesStore store = new CachedQueriesStore(new File("cached"));
        	
            public LocalComponent getInstance()
            {
                return new RemoteCacheAccessLocalInputComponent(store);
            }
        };

        LocalControllerBase controller = setUpController( inputFactory );

        String query = "dump:";
        List results = (List) controller.query("testprocess", query, new HashMap()).getQueryResult();

        // the results should contain some documents.
        assertEquals( 4, results.size());
	}

	public void testStoreRawQuery() throws Exception {
        // ODP input component factory
        LocalComponentFactory inputFactory = new LocalComponentFactory()
        {
        	private CachedQueriesStore store = new CachedQueriesStore(new File("cached"));
        	
            public LocalComponent getInstance()
            {
                return new RemoteCacheAccessLocalInputComponent(store);
            }
        };

        LocalControllerBase controller = setUpController( inputFactory );

        String query = "salsa";
        List results = (List) controller.query("testprocess", query, new HashMap()).getQueryResult();
        assertTrue( results.size() > 0);
        
        query = "bank zachodni wbk";
        results = (List) controller.query("testprocess", query, new HashMap()).getQueryResult();
        assertTrue( results.size() > 0);
	}

	public void testStoreRawQueryWithComponentId() throws Exception {
        // ODP input component factory
        LocalComponentFactory inputFactory = new LocalComponentFactory()
        {
        	private CachedQueriesStore store = new CachedQueriesStore(new File("cached"));
        	
            public LocalComponent getInstance()
            {
                return new RemoteCacheAccessLocalInputComponent(store);
            }
        };

        LocalControllerBase controller = setUpController( inputFactory );

        String query = "salsa component:carrot2.input.snippet-reader.alltheweb";
        List results = (List) controller.query("testprocess", query, new HashMap()).getQueryResult();
        assertTrue( results.size() > 0);

        query = "component:carrot2.input.snippet-reader.alltheweb salsa";
        List resultsAlltheweb = (List) controller.query("testprocess", query, new HashMap()).getQueryResult();
        assertTrue( resultsAlltheweb.size() > 0);

        query = "component:carrot2.input.snippet-reader.google salsa";
        List resultsGoogle = (List) controller.query("testprocess", query, new HashMap()).getQueryResult();
        assertTrue( resultsGoogle.size() > 0);

        assertFalse(
        		((RawDocument) resultsGoogle.get(0)).getUrl().equals( 
				((RawDocument) resultsAlltheweb.get(0)).getUrl()));

	}
	
}