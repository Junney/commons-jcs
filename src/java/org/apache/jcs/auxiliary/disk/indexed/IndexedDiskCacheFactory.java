package org.apache.jcs.auxiliary.disk.indexed;


/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jcs.auxiliary.AuxiliaryCache;
import org.apache.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.jcs.auxiliary.AuxiliaryCacheFactory;
import org.apache.jcs.engine.control.CompositeCache;

/**
 * @version 1.0
 */
public class IndexedDiskCacheFactory implements AuxiliaryCacheFactory
{
    private final static Log log =
        LogFactory.getLog( IndexedDiskCacheFactory.class );

    private String name;

    /**
     * Get an instance of the IndexDiskCacheManager for the attributes and then 
     * get an IndexedDiskCache from the manager.
     * <p>
     * The manager is a singleton.
     * <p>
     * One disk cache is returned per region fromt he maanger.
     *
     * @param iaca The auxiliary attributes.
     * @param cache The CacheHub
     * @return AuxiliaryCache
     */
    public AuxiliaryCache createCache( AuxiliaryCacheAttributes iaca,
                                       CompositeCache cache )
    {
        IndexedDiskCacheAttributes idca = ( IndexedDiskCacheAttributes ) iaca;
        if ( log.isDebugEnabled() )
        {
            log.debug( "Creating DiskCache for attributes = " + idca );
        }
        IndexedDiskCacheManager dcm = IndexedDiskCacheManager.getInstance( idca );
        return dcm.getCache( idca );
    }

    /**
     * Gets the name attribute of the DiskCacheFactory object
     *
     * @return The name value
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Sets the name attribute of the DiskCacheFactory object
     *
     * @param name The new name value
     */
    public void setName( String name )
    {
        this.name = name;
    }
}
