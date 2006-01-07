
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2006, Dawid Weiss, Stanisław Osiński.
 * Portions (C) Contributors listed in "carrot2.CONTRIBUTORS" file.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.cs.put.poznan.pl/dweiss/carrot2.LICENSE
 */
package com.dawidweiss.carrot.remote.controller.cache;


import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.dom4j.io.SAXReader;

import com.dawidweiss.carrot.controller.carrot2.xmlbinding.Query;
import com.dawidweiss.carrot.util.net.URLEncoding;


public class ZIPCachedQuery
    extends AbstractFileCachedQuery
{
    private static Logger log = Logger.getLogger(ZIPCachedQuery.class);
    private long dataOffset;

    public ZIPCachedQuery(File cacheFile, CachedQuery q)
        throws IOException
    {
        super(cacheFile, q);
    }


    public ZIPCachedQuery(File cacheFile)
        throws IOException
    {
        super(cacheFile);
    }

    protected void dumpDataToFile(InputStream dataStream)
        throws IOException
    {
        OutputStream os = null;

        try
        {
            os = new GZIPOutputStream(new FileOutputStream(this.file));

            byte [] encoded = URLEncoding.encode(this.getComponentId().getBytes("UTF-8"));
            os.write(encoded);
            dataOffset += encoded.length;

            // field separator
            os.write(0);
            dataOffset += 1;

            Query q = this.getQuery();
            StringWriter sw = new StringWriter();

            try
            {
                q.marshal(sw);
            }
            catch (Exception e)
            {
                throw new IOException("Cannot marshall query from cached file.");
            }

            encoded = URLEncoding.encode(sw.getBuffer().toString().getBytes("UTF-8"));
            os.write(encoded);
            dataOffset += encoded.length;

            // field separator
            os.write(0);
            dataOffset += 1;

            Map optionalParams = this.getOptionalParams();

            if (optionalParams != null)
            {
                for (Iterator i = optionalParams.keySet().iterator(); i.hasNext();)
                {
                    String key = (String) i.next();
                    String value = (String) optionalParams.get(key);

                    encoded = URLEncoding.encode(key.getBytes("UTF-8"));
                    os.write(encoded);
                    dataOffset += encoded.length;

                    if (value != null)
                    {
                        os.write('=');
                        dataOffset += 1;
                        encoded = URLEncoding.encode(value.getBytes("UTF-8"));
                        os.write(encoded);
                        dataOffset += encoded.length;
                    }

                    if (i.hasNext())
                    {
                        os.write('&');
                    }

                    dataOffset += 1;
                }
            }

            // field separator
            os.write(0);
            dataOffset += 1;

            // copy data.
            byte [] buffer = new byte[8000];

            do
            {
                int bcount = dataStream.read(buffer);

                if (bcount == -1)
                {
                    break;
                }

                os.write(buffer, 0, bcount);
            }
            while (true);
        }
        finally
        {
            if (os != null)
            {
                try
                {
                    os.close();
                }
                catch (IOException e)
                {
                    log.error("Cannot close cache file: " + file.getAbsolutePath());
                }
            }

            if (dataStream != null)
            {
                try
                {
                    dataStream.close();
                }
                catch (IOException e)
                {
                    log.error("Cannot close stream of the cache being copied.");
                }
            }
        }
    }


    protected void loadDataFromFile()
        throws IOException
    {
        InputStream is = null;

        try
        {
            is = new BufferedInputStream(new GZIPInputStream(
                        new FileInputStream(this.file)));

            int dataOffset = 0;
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            while (true)
            {
                int b = is.read();

                if (b == 0)
                {
                    // field separator reached.
                    if (this.componentId == null)
                    {
                        byte [] encoded = os.toByteArray();
                        dataOffset += encoded.length;
                        dataOffset += 1; // include the separator field
                        componentId = new String(URLEncoding.decode(encoded), "UTF-8");
                    }
                    else if (this.query == null)
                    {
                        byte [] encoded = os.toByteArray();
                        dataOffset += encoded.length;
                        dataOffset += 1; // include the separator field

                        try
                        {
                            this.query = Query.unmarshal(
                                    new SAXReader().read(new StringReader(
                                        new String(URLEncoding.decode(encoded), "UTF-8")
                                    )).getRootElement()
                                );
                        }
                        catch (Exception e)
                        {
                            log.warn("Cannot unmarshall cached query.", e);
                            throw new IOException("Cannot unmarshall cached query.");
                        }
                    }
                    else if (this.optionalParams == null)
                    {
                        byte [] encoded = os.toByteArray();
                        dataOffset += encoded.length;
                        dataOffset += 1; // include the separator field

                        if (encoded.length > 0)
                        {
							Map optionalParams = new HashMap();
							String urlEncoded = new String(URLEncoding
									.decode(encoded), "UTF-8");
							StringTokenizer tokenizer = new StringTokenizer(
									urlEncoded, "&", false);

							while (tokenizer.hasMoreTokens()) {
                                String param = tokenizer.nextToken();
                                if (param.indexOf('=') < 0) {
	                                // If no '=', consider the parameter empty.
	                                optionalParams.put(param, "");
                                } else {
									String key = param.substring(0, param.indexOf('='));
									String value = param.substring(param.indexOf('=')+1);
									optionalParams.put(key, value);
								}
							}
							this.optionalParams = optionalParams;
                        }
                        else
                        {
                            this.optionalParams = null;
                        }

                        this.dataOffset = dataOffset;

                        return;
                    }

                    os.reset();
                }
                else if (b == -1)
                {
                    throw new IOException(
                        "Premature end of cached file."
                        + com.dawidweiss.carrot.remote.controller.util.ExceptionHelper.getCurrentStackTrace()
                    );
                }
                else
                {
                    os.write(b);
                }
            }
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    log.error("Cannot close input cache file: " + file.getAbsolutePath());
                }
            }
        }
    }


    /**
     * Return the stream to input cache data
     */
    public InputStream getData()
        throws java.io.IOException
    {
        InputStream is;
        is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(this.file)));
        is.skip(this.dataOffset);

        return is;
    }
}
