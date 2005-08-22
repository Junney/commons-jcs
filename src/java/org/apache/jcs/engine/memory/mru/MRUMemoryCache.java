package org.apache.jcs.engine.memory.mru;

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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jcs.engine.CacheConstants;
import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.control.CompositeCache;
import org.apache.jcs.engine.control.group.GroupAttrName;
import org.apache.jcs.engine.control.group.GroupId;
import org.apache.jcs.engine.memory.AbstractMemoryCache;
import org.apache.jcs.engine.stats.StatElement;
import org.apache.jcs.engine.stats.Stats;
import org.apache.jcs.engine.stats.behavior.IStatElement;
import org.apache.jcs.engine.stats.behavior.IStats;

/**
 * A SLOW AS HELL reference management system. The most recently used items move
 * to the front of the list and get spooled to disk if the cache hub is
 * configured to use a disk cache.
 * 
 * @version $Id$
 */
public class MRUMemoryCache
    extends AbstractMemoryCache
{
    private final static Log log = LogFactory.getLog( MRUMemoryCache.class );

    int hitCnt = 0;

    int missCnt = 0;

    int putCnt = 0;

    /**
     * Object to lock on the Field
     */
    protected int[] lockMe = new int[0];

    /**
     * MRU list.
     */
    protected LinkedList mrulist = new LinkedList();

    /**
     * For post reflection creation initialization
     * 
     * @param hub
     */
    public synchronized void initialize( CompositeCache hub )
    {
        super.initialize( hub );
        log.info( "initialized MRUMemoryCache for " + cacheName );
    }

    /**
     * Puts an item to the cache.
     * 
     * @param ce
     * @exception IOException
     */
    public void update( ICacheElement ce )
        throws IOException
    {
        putCnt++;

        Serializable key = ce.getKey();
        ce.getElementAttributes().setLastAccessTimeNow();

        // need a more fine grained locking here
        boolean replace = false;
        if ( map.containsKey( key ) )
        {
            replace = true;
        }
        synchronized ( lockMe )
        {
            map.put( key, ce );
            if ( replace )
            {
                // the slowest method I've ever seen
                mrulist.remove( key );
            }
            mrulist.addFirst( key );
        }

        // save a microsecond on the second call.
        int size = map.size();
        // need to spool at a certain percentage synchronously
        if ( size < this.cattr.getMaxObjects() )
        {
            return;
        }
        // SPOOL LAST -- need to make this a grouping in a queue

        log.debug( "In RAM overflow" );

        // write the last item to disk.
        try
        {

            // PUSH 5 TO DISK TO MINIMIZE THE TYPICAL
            int chunkSizeCorrected = Math.min( size, chunkSize );

            if ( log.isDebugEnabled() )
            {
                log.debug( "update: About to spool to disk cache, map.size() = " + size
                    + ", this.cattr.getMaxObjects() = " + this.cattr.getMaxObjects() + ", chunkSizeCorrected = "
                    + chunkSizeCorrected );
            }

            // The spool will put them in a disk event queue, so there is no
            // need to pre-queue the queuing. This would be a bit wasteful
            // and wouldn't save much time in this synchronous call.
            for ( int i = 0; i < chunkSizeCorrected; i++ )
            {
                // Might want to rename this "overflow" incase the hub
                // wants to do something else.
                Serializable last = (Serializable) mrulist.getLast();
                ICacheElement ceL = (ICacheElement) map.get( last );
                cache.spoolToDisk( ceL );

                // need a more fine grained locking here
                synchronized ( map )
                {
                    map.remove( last );
                    mrulist.remove( last );
                }
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "update: After spool,  map.size() = " + size + ", this.cattr.getMaxObjects() = "
                    + this.cattr.getMaxObjects() + ", chunkSizeCorrected = " + chunkSizeCorrected );
            }

        }
        catch ( Exception ex )
        {
            // impossible case.
            ex.printStackTrace();
            throw new IllegalStateException( ex.getMessage() );
        }
    }

    /**
     * Get an item from the cache without affecting its last access time or
     * position.
     * 
     * @return Element mathinh key if found, or null
     * @param key
     *            Identifies item to find
     * @exception IOException
     */
    public ICacheElement getQuiet( Serializable key )
        throws IOException
    {
        ICacheElement ce = null;

        try
        {

            ce = (ICacheElement) map.get( key );
            if ( ce != null )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( cacheName + ": MRUMemoryCache quiet hit for " + key );
                }

            }
            else
            {
                log.debug( cacheName + ": MRUMemoryCache quiet miss for " + key );
            }

        }
        catch ( Exception e )
        {
            log.error( e );
        }

        return ce;
    }

    /**
     * Description of the Method
     * 
     * @return
     * @param key
     * @exception IOException
     */
    public ICacheElement get( Serializable key )
        throws IOException
    {
        ICacheElement ce = null;
        boolean found = false;

        try
        {

            if ( log.isDebugEnabled() )
            {
                log.debug( "get> key=" + key );
                log.debug( "get> key=" + key.toString() );
            }

            ce = (ICacheElement) map.get( key );
            if ( log.isDebugEnabled() )
            {
                log.debug( "ce =" + ce );
            }

            if ( ce == null )
            {

            }
            else
            {
                found = true;
                ce.getElementAttributes().setLastAccessTimeNow();
                hitCnt++;
                if ( log.isDebugEnabled() )
                {
                    log.debug( cacheName + " -- RAM-HIT for " + key );
                }
            }

        }
        catch ( Exception e )
        {
            log.error( e );
        }

        try
        {

            if ( !found )
            {
                // Item not found in cache.
                missCnt++;
                if ( log.isDebugEnabled() )
                {
                    log.debug( cacheName + " -- MISS for " + key );
                }
                return null;
            }
        }
        catch ( Exception e )
        {
            log.error( "Error handling miss", e );
            return null;
        }

        try
        {
            synchronized ( lockMe )
            {
                mrulist.remove( ce.getKey() );
                mrulist.addFirst( ce.getKey() );
            }
        }
        catch ( Exception e )
        {
            log.error( "Error making first", e );
            return null;
        }

        return ce;
    }

    // end get

    /**
     * Removes an item from the cache.
     * 
     * @return
     * @param key
     * @exception IOException
     */
    public boolean remove( Serializable key )
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "remove> key=" + key );
            //+, nonLocal="+nonLocal);
        }

        //p("remove> key="+key+", nonLocal="+nonLocal);
        boolean removed = false;

        // handle partial removal
        if ( key instanceof String && key.toString().endsWith( CacheConstants.NAME_COMPONENT_DELIMITER ) )
        {
            // remove all keys of the same name hierarchy.
            synchronized ( map )
            {
                for ( Iterator itr = map.entrySet().iterator(); itr.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) itr.next();
                    Object k = entry.getKey();
                    if ( k instanceof String && k.toString().startsWith( key.toString() ) )
                    {
                        itr.remove();
                        Serializable keyR = (ICacheElement) entry.getKey();
                        map.remove( keyR );
                        mrulist.remove( keyR );
                        removed = true;
                    }
                }
            }
        }
        else if ( key instanceof GroupId )
        {
            // remove all keys of the same name hierarchy.
            synchronized ( map )
            {
                for ( Iterator itr = map.entrySet().iterator(); itr.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) itr.next();
                    Object k = entry.getKey();

                    if ( k instanceof GroupAttrName && ( (GroupAttrName) k ).groupId.equals( key ) )
                    {
                        itr.remove();
                        mrulist.remove( k );
                        removed = true;
                    }
                }
            }
        }
        else
        {
            // remove single item.
            if ( map.containsKey( key ) )
            {
                synchronized ( lockMe )
                {

                    map.remove( key );
                    mrulist.remove( key );
                }
                removed = true;
            }
        }
        // end else not hierarchical removal
        return removed;
    }

    /**
     * Get an Array of the keys for all elements in the memory cache
     * 
     * @return Object[]
     */
    public Object[] getKeyArray()
    {
        synchronized ( lockMe )
        {
            // may need to lock to map here?
            return map.keySet().toArray();
        }
    }

    /**
     * Dump the cache map for debugging.
     */
    public void dumpMap()
    {
        log.debug( "dumpingMap" );
        for ( Iterator itr = map.entrySet().iterator(); itr.hasNext(); )
        {
            //for ( Iterator itr = memCache.getIterator(); itr.hasNext();) {
            Map.Entry e = (Map.Entry) itr.next();
            ICacheElement ce = (ICacheElement) e.getValue();
            log.debug( "dumpMap> key=" + e.getKey() + ", val=" + ce.getVal() );
        }
    }

    /**
     * Dump the cache entries from first to list for debugging.
     */
    public void dumpCacheEntries()
    {
        log.debug( "dumpingCacheEntries" );
        ListIterator li = mrulist.listIterator();
        while ( li.hasNext() )
        {
            Serializable key = (Serializable) li.next();
            log.debug( "dumpCacheEntries> key=" + key + ", val=" + ( (ICacheElement) map.get( key ) ).getVal() );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jcs.engine.memory.MemoryCache#getStatistics()
     */
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "MRU Memory Cache" );

        ArrayList elems = new ArrayList();

        IStatElement se = null;

        se = new StatElement();
        se.setName( "List Size" );
        se.setData( "" + mrulist.size() );
        elems.add( se );

        se = new StatElement();
        se.setName( "Map Size" );
        se.setData( "" + map.size() );
        elems.add( se );

        se = new StatElement();
        se.setName( "Put Count" );
        se.setData( "" + putCnt );
        elems.add( se );

        se = new StatElement();
        se.setName( "Hit Count" );
        se.setData( "" + hitCnt );
        elems.add( se );

        se = new StatElement();
        se.setName( "Miss Count" );
        se.setData( "" + missCnt );
        elems.add( se );

        // get an array and put them in the Stats object
        IStatElement[] ses = (IStatElement[]) elems.toArray( new StatElement[0] );
        stats.setStatElements( ses );

        return stats;
    }
}
