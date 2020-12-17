/**
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Arne Kepp, OpenGeo, Copyright 2009
 */
package org.geowebcache.filter.request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.CompositeBlobStore;
import org.geowebcache.storage.DefaultStorageBroker;
import org.geowebcache.storage.StorageException;

/**
 * A raster filter allows to optimize data loading by avoiding the generation of requests and the
 * caching of empty tiles for tiles that are inside the definition area of the layer but that are
 * known (via external information) to contain no data
 *
 * <p>To conserve memory, the layer bounds are used.
 */
public class ExistingTileFilter extends RequestFilter {
    private static final long serialVersionUID = -4853124825161506008L;

    private Integer zoomStart;
    private Integer zoomStop;
    private Boolean debug;

    private static Log log = LogFactory.getLog(ExistingTileFilter.class);

    public ExistingTileFilter() {}

    /** @return the zoomStart */
    public Integer getZoomStart() {
        return zoomStart;
    }

    /** @param zoomStart the zoomStart to set */
    public void setZoomStart(Integer zoomStart) {
        this.zoomStart = zoomStart;
    }

    /** @return the zoomStop */
    public Integer getZoomStop() {
        return zoomStop;
    }

    /** @param zoomStop the zoomStop to set */
    public void setZoomStop(Integer zoomStop) {
        this.zoomStop = zoomStop;
    }

    /** @return the debug */
    public Boolean getDebug() {
        return debug;
    }

    /** @param debug the debug to set */
    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public void apply(ConveyorTile convTile) throws RequestFilterException {
        long[] idx = convTile.getTileIndex().clone();
        boolean exists = false;

        // Basic bounds test first (TODO: Try without this)
        try {
            convTile.getGridSubset().checkCoverage(idx);
        } catch (OutsideCoverageException oce) {
            throw new BlankTileException(this);
        }

        // Check if filter is not to be applied at this zoom level
        if ((zoomStart != null && idx[2] < zoomStart) || (zoomStop != null && idx[2] > zoomStop)) {
            return;
        }

        // Check if tile exists
        DefaultStorageBroker dsb = (DefaultStorageBroker) convTile.getStorageBroker();
        if (dsb.getBlobStore() instanceof CompositeBlobStore) {
            CompositeBlobStore cbs = (CompositeBlobStore) dsb.getBlobStore();
            try {
                exists = cbs.tileExists(convTile.getStorageObject());
            } catch (StorageException e) {
                e.printStackTrace();
            }
        }

        // Abort this request if the tile doesn't already exist
        if (!exists) {
            long[] xyz = convTile.getTileIndex();
            log.info("Tile [" + xyz[0] + "," + xyz[1] + "," + xyz[2] + "] does not exist.");
            if (debug != null && debug) {
                throw new GreenTileException(this);
            } else {
                throw new BlankTileException(this);
            }
        }
    }

    /** Performs any initialisation necessary */
    public void initialize(TileLayer layer) throws GeoWebCacheException {
        // Nothing to do
    }

    /** Dummy implementation */
    public void update(TileLayer layer, String gridSetId, int zoomStart, int zoomStop)
            throws GeoWebCacheException {
        throw new GeoWebCacheException(
                "(TileLayer layer, String gridSetId, int z) is not appropriate for ExistingTileFilters");
    }

    /** Dummy implementation */
    public boolean update(TileLayer layer, String gridSetId) {
        return false;
    }

    /** Dummy implementation */
    public void update(byte[] filterData, TileLayer layer, String gridSetId, int z)
            throws GeoWebCacheException {
        // Do nothing
    }
}
