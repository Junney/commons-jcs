package org.apache.jcs.auxiliary.lateral;


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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheAttributes;
import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheService;
import org.apache.jcs.engine.CacheConstants;
import org.apache.jcs.engine.behavior.ICache;
import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.behavior.ICacheType;
import org.apache.jcs.engine.behavior.IZombie;

/**
 * Lateral distributor. Returns null on get by default. Net search not implemented.
 *
 */
public class LateralCache implements ICache
{
    private final static Log log =
        LogFactory.getLog( LateralCache.class );
    
    // generalize this, use another interface
    private ILateralCacheAttributes cattr;

    final String cacheName;

    /** either http, socket.udp, or socket.tcp can set in config */
    private ILateralCacheService lateral;

    /**
     * Constructor for the LateralCache object
     *
     * @param cattr
     * @param lateral
     */
    protected LateralCache( ILateralCacheAttributes cattr, ILateralCacheService lateral )
    {
        this.cacheName = cattr.getCacheName();
        this.cattr = cattr;
        this.lateral = lateral;
    }


    /**
     * Constructor for the LateralCache object
     *
     * @param cattr
     */
    protected LateralCache( ILateralCacheAttributes cattr )
    {
        this.cacheName = cattr.getCacheName();
        this.cattr =  cattr ;
    }


    /*
     *  (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "LateralCache: " + cattr.getCacheName();
    }

    /** 
     * Update lateral.
     *  
     * @param ce
     * @throws IOException
     */    
    public void update( ICacheElement ce )
        throws IOException
    {
        try
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "update: lateral = " + lateral + ", " +
                    "LateralCacheInfo.listenerId = " +
                    LateralCacheInfo.listenerId );
            }
            lateral.update( ce, LateralCacheInfo.listenerId );
        }
        catch ( NullPointerException npe )
        {
            log.error( "Failure updating lateral", npe );
            return;
        }
        catch ( Exception ex )
        {
            handleException( ex, "Failed to put " + ce.getKey() + " to " + ce.getCacheName() );
        }
    }
    // end update

    /** 
     * The performace costs are too great.  It is not recommended that you enable lateral
     * gets.
     *  
     * @param key
     * @return
     * @throws IOException
     */
    public ICacheElement get( Serializable key )
        throws IOException
    {
        ICacheElement obj = null;

        if ( this.cattr.getPutOnlyMode() )
        {
            return null;
        }
        try
        {
            obj = lateral.get( cacheName, key );
        }
        catch ( Exception e )
        {
            log.error( e );
            handleException( e, "Failed to get " + key + " from " + this.cattr.getCacheName() );
        }
    return obj;
    }


    /**
     * 
     * @param groupName
     * @return A set og group keys.
     */
    public Set getGroupKeys(String groupName)
    {
        return lateral.getGroupKeys(cacheName, groupName);
    }


    /**
     * Synchronously remove from the remote cache; if failed, replace the remote
     * handle with a zombie.
     * @param key
     * @return
     * @throws IOException
     */
    public boolean remove( Serializable key )
        throws IOException
    {
        log.debug( "removing key:" + key );

        try
        {
            //DeleteLateralCacheMulticaster dlcm = new DeleteLateralCacheMulticaster( cattr.getCacheName(), (String)key, cattr.getLateralCacheAddrs(), cattr.getLateralDeleteServlet()  );
            //dlcm.multicast();
            lateral.remove( cacheName, key, LateralCacheInfo.listenerId );
        }
        catch ( Exception ex )
        {
            handleException( ex, "Failed to remove " + key + " from " + this.cattr.getCacheName() );
        }
        return false;
    }


    /**
     * Synchronously removeAll from the remote cache; if failed, replace the
     * remote handle with a zombie.
     * 
     * @throws IOException
     */
    public void removeAll()
        throws IOException
    {
        try
        {
            //DeleteLateralCacheMulticaster dlcm = new DeleteLateralCacheMulticaster( cattr.getCacheName(), "ALL", cattr.getLateralCacheAddrs(), cattr.getLateralDeleteServlet()  );
            //dlcm.multicast();
            lateral.removeAll( cacheName, LateralCacheInfo.listenerId );
        }
        catch ( Exception ex )
        {
            handleException( ex, "Failed to remove all from " + this.cattr.getCacheName() );
        }
    }


    /** Synchronously dispose the cache. Not sure we want this. 
     * @throws IOException*/
    public void dispose()
        throws IOException
    {
        log.debug( "Disposing of lateral cache" );

        ///* HELP: This section did nothing but generate compilation warnings.
        // TODO: may limit this funcionality. It is dangerous.
        // asmuts -- Added functionality to help with warnings.  I'm not getting any.
        try
        {
            lateral.dispose( this.cattr.getCacheName() );
            // Should remove connection
        }
        catch ( Exception ex )
        {
            log.error( "Couldn't dispose", ex );
            handleException( ex, "Failed to dispose " + this.cattr.getCacheName() );
        }
        //*/
    }

    /**
     * Returns the cache status.
     *
     * @return The status value
     */
    public int getStatus()
    {
        return this.lateral instanceof IZombie ? CacheConstants.STATUS_ERROR : CacheConstants.STATUS_ALIVE;
    }

    /**
     * Returns the current cache size.
     *
     * @return The size value
     */
    public int getSize()
    {
        return 0;
    }

    /**
     * Gets the cacheType attribute of the LateralCache object
     *
     * @return The cacheType value
     */
    public int getCacheType()
    {
        return ICacheType.LATERAL_CACHE;
    }


    /**
     * Gets the cacheName attribute of the LateralCache object
     *
     * @return The cacheName value
     */
    public String getCacheName()
    {
        return cacheName;
    }


    /** Not yet sure what to do here. 
     * @param ex
     * @param msg
     * @throws IOException
     */
    private void handleException( Exception ex, String msg )
        throws IOException
    {

        log.error( "Disabling lateral cache due to error " + msg, ex );

        lateral = new ZombieLateralCacheService();
        // may want to flush if region specifies
        // Notify the cache monitor about the error, and kick off the recovery process.
        LateralCacheMonitor.getInstance().notifyError();

        // could stop the net serach if it is built and try to reconnect?
        if ( ex instanceof IOException )
        {
            throw ( IOException ) ex;
        }
        throw new IOException( ex.getMessage() );
    }


    /**
     * Replaces the current remote cache service handle with the given handle.
     * @param lateral
     */
    public void fixCache( ILateralCacheService lateral )
    {
        this.lateral = lateral;
        return;
    }

  /**
   * getStats
   *
   * @return String
   */
  public String getStats()
  {
    return "";
  }

  /**
   * @return Returns the AuxiliaryCacheAttributes.
   */
  public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
  {
    return cattr;
  }
}
