/*
 * Copyright (c) 2012, RamsesA <ramsesakama@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

package org.craftingazeroth.plugins.cat;

import org.bukkit.Location;

public class BoundingBox
{
    public final Coordinate3D min;
    public final Coordinate3D max;
        
    public BoundingBox(int x0, int y0, int z0, int x1, int y1, int z1)
    {
        int minx = Math.min(x1, x0);
        int miny = Math.min(y1, y0);
        int minz = Math.min(z1, z0);
        int maxx = Math.max(x1, x0);
        int maxy = Math.max(y1, y0);
        int maxz = Math.max(z1, z0);

        min = new Coordinate3D(minx, miny, minz);
        max = new Coordinate3D(maxx, maxy, maxz);
    }

    public BoundingBox(Coordinate3D c0, Coordinate3D c1)
    {
        int minx = Math.min(c0.x, c1.x);
        int miny = Math.min(c0.y, c1.y);
        int minz = Math.min(c0.z, c1.z);
        int maxx = Math.max(c0.x, c1.x);
        int maxy = Math.max(c0.y, c1.y);
        int maxz = Math.max(c0.z, c1.z);

        min = new Coordinate3D(minx, miny, minz);
        max = new Coordinate3D(maxx, maxy, maxz);
    }
    
    public int getWidth()
    {
        return max.x - min.x;
    }

    public int getLength()
    {
        return max.z - min.z;
    }
    
    public int getHeight()
    {
        return max.y - min.y;
    }
    
    public boolean contains(int x, int y, int z)
    {
        return min.x <= x && x <= max.x
            && min.y <= y && y <= max.y
            && min.z <= z && z <= max.z;
    }
    
    public boolean contains(Location loc)
    {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return min.x <= x && x <= max.x
            && min.y <= y && y <= max.y
            && min.z <= z && z <= max.z;
    }

    public boolean contains(BoundingBox box)
    {
        return min.x <= box.min.x && box.max.x <= max.x
            && min.y <= box.min.y && box.max.y <= max.y
            && min.z <= box.min.z && box.max.z <= max.z;
    }
}