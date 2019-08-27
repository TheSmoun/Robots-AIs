/*
 * KIT Schnupperstudium Robots AIs
 * Copyright (C) 2019  Simon Grossmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.schnupperstudium.robots.client.ai.hidden;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.github.schnupperstudium.robots.entity.Facing;
import com.github.schnupperstudium.robots.entity.item.BlueKey;
import com.github.schnupperstudium.robots.entity.item.Cookie;
import com.github.schnupperstudium.robots.entity.item.GreenKey;
import com.github.schnupperstudium.robots.entity.item.LaserCharge;
import com.github.schnupperstudium.robots.entity.item.RedKey;
import com.github.schnupperstudium.robots.entity.item.Star;
import com.github.schnupperstudium.robots.entity.item.YellowKey;
import com.github.schnupperstudium.robots.world.Map;
import com.github.schnupperstudium.robots.world.Material;
import com.github.schnupperstudium.robots.world.Tile;

public class DistanceScalingMap implements Map {
	
	private static final java.util.Map<Class<?>, Integer> ITEM_VALUES = new HashMap<>();
	
	private static final int COOKIE_VALUE = 1;
	private static final int KEY_VALUE = 10;
	private static final int CHARGE_VALUE = 5;
	private static final int STAR_VALUE = 100;
	private static final int UNDEFINED_VALUE = STAR_VALUE * 100;
	
	private MapTile[][] mapTiles;
	private int width;
	private int height;
	
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;
	
	static {
		ITEM_VALUES.put(Cookie.class, COOKIE_VALUE);
		ITEM_VALUES.put(BlueKey.class, KEY_VALUE);
		ITEM_VALUES.put(GreenKey.class, KEY_VALUE);
		ITEM_VALUES.put(RedKey.class, KEY_VALUE);
		ITEM_VALUES.put(YellowKey.class, KEY_VALUE);
		ITEM_VALUES.put(LaserCharge.class, CHARGE_VALUE);
		ITEM_VALUES.put(Star.class, STAR_VALUE);
	}
	
	public DistanceScalingMap() {
		this.width = 0;
		this.height = 0;
		this.minX = Integer.MAX_VALUE;
		this.maxX = Integer.MIN_VALUE;
		this.minY = Integer.MAX_VALUE;
		this.maxY = Integer.MIN_VALUE;
		this.mapTiles = new MapTile[this.width][this.height];
		for (int x = 0; x < this.width; x++) {
			for (int y = 0; y < this.height; y++) {
				this.mapTiles[x][y] = new MapTile(x, y);
			}
		}
	}
	
	public void updateMap(List<Tile> tiles) {
		if (tiles == null || tiles.isEmpty())
			return;
		
		final int vMinX = tiles.stream().mapToInt(Tile::getX).min().getAsInt();
		final int vMinY = tiles.stream().mapToInt(Tile::getY).min().getAsInt();
		final int vMaxX = tiles.stream().mapToInt(Tile::getX).max().getAsInt();
		final int vMaxY = tiles.stream().mapToInt(Tile::getY).max().getAsInt();
		
		if (!this.isWithinMap(vMinX, vMinY) || !this.isWithinMap(vMaxX, vMaxY)) 
			this.resize(Math.min(this.minX, vMinX), Math.min(this.minY, vMinY), Math.max(this.maxX, vMaxX + 1), Math.max(this.maxY, vMaxY + 1));
		
		tiles.forEach(this::updateTile);
	}
	
	private void resize(int nMinX, int nMinY, int nMaxX, int nMaxY) {
		int nWidth = nMaxX - nMinX;
		int nHeight = nMaxY - nMinY;
		MapTile[][] nTiles = new MapTile[nWidth][nHeight];
		for (int i = 0; i < nWidth; i++) {
			int x = i + nMinX;
			for (int j = 0; j < nHeight; j++) {
				int y = j + nMinY;
				if (isWithinMap(x, y)) {
					nTiles[i][j] = this.mapTiles[x - this.minX][y - this.minY];
				} else
					nTiles[i][j] = new MapTile(x, y);
			}
		}
		
		this.mapTiles = nTiles;
		this.minX = nMinX;
		this.minY = nMinY;
		this.maxX = nMaxX;
		this.maxY = nMaxY;
		this.width = nWidth;
		this.height = nHeight;
	}
	
	private void updateTile(final Tile tile) {
		final int x = tile.getX();
		final int y = tile.getY();
		this.mapTiles[x - this.minX][y - this.minY].update(tile);
	}
	
	@Override
	public int getWidth() {
		return this.width;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public MapTile getTile(final int x, final int y) {
		if (this.isWithinMap(x, y))
			return this.mapTiles[x - this.minX][y - this.minY];
		else
			return new MapTile(x, y);
	}
	
	public int getValue(final Tile tile) {
		return this.getValue(tile.getX(), tile.getY());
	}
	
	public int getValue(final int x, final int y) {
		return this.getTile(x, y).value;
	}
	
	public void setValue(final Tile tile, final int v) {
		this.setValue(tile.getX(), tile.getY(), v);
	}
	
	public void setValue(int x, int y, int v) {
		this.getTile(x, y).value = v;
	}
	
	public void clearValues() {
		for (int i = 0; i < this.mapTiles.length; i++) {
			for (int j = 0; j < this.mapTiles[i].length; j++) {
				this.mapTiles[i][j].value = 0;
			}
		}
	}
	
	@Override
	public int getMinX() {
		return this.minX;
	}
	
	@Override
	public int getMaxX() {
		return this.maxX;
	}
	
	@Override
	public int getMinY() {
		return this.minY;
	}
	
	@Override
	public int getMaxY() {
		return this.maxY;
	}
	
	public boolean isWithinMap(final int x, final int y) {
		return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY;
	}
	
	public Tile getNextTile(final Tile beneathTile) {
		this.clearValues();
		setValue(beneathTile, Integer.MAX_VALUE);
		
		final Queue<Tile> queue = new LinkedList<>();
		for (int i = 0; i < this.mapTiles.length; i++) {
			for (int j = 0; j < this.mapTiles[i].length; j++) {
				final Tile tile = this.mapTiles[i][j];
				if (tile.hasItem() && tile.getItem() instanceof Star) {
					setValue(tile, STAR_VALUE);
					queue.add(tile);
				} else if (tile.getMaterial() == Material.UNDEFINED) {
					setValue(tile, UNDEFINED_VALUE);
					queue.add(tile);
				} else if (i == 0 || i == mapTiles.length - 1
						|| j == 0 || j == mapTiles[i].length - 1) {
					setValue(tile, UNDEFINED_VALUE);
					queue.add(tile);
				}
			}
		}
		
		while (!queue.isEmpty()) {
			final Tile tile = queue.poll();
			final int value = getValue(tile);
			if (value == 0)
				continue;
			
			for (final Facing facing : Facing.values()) {
				final Tile neighbor = getNeighborTile(tile, facing);
				if (!neighbor.canVisit())
					continue;
				
				final int neighborValue = getValue(neighbor);
				if (neighborValue == Integer.MAX_VALUE)
					continue;

				if (neighborValue == 0) {
					setValue(neighbor, value + 1);
					queue.add(neighbor);
				} else {
					final int newValue = Math.min(neighborValue, value + 1);
					if (neighborValue > newValue) {
						setValue(tile, newValue);
						queue.add(neighbor);
					}
				}
			}
		}
		
		final LinkedList<Tile> path = new LinkedList<>();
		Tile tile = beneathTile;
		do {
			final Collection<Tile> neighbors = getNeighbors(tile);
			tile = neighbors.stream().min((t1, t2) -> {
				return Integer.compare(getValue(t1), getValue(t2));
			}).orElse(null);
			if (tile != null) {
				path.add(tile);
			}
		} while (tile != null || path.isEmpty());
		
		return path.isEmpty() ? null : path.poll();
	}
	
	private Tile getNeighborTile(final Tile source, final Facing facing) {
		return getTile(source.getX() + facing.dx, source.getY() + facing.dy);
	}
	
	private Collection<Tile> getNeighbors(final Tile tile) {
		if (tile == null)
			return new ArrayList<>();
		
		final int value = getValue(tile);
		if (value == 0)
			return new ArrayList<>();
		
		final List<Tile> result = new ArrayList<>();
		for (final Facing facing : Facing.values()) {
			final Tile neighbor = getNeighborTile(tile, facing);
			final int neighborValue = getValue(neighbor);
			if (neighborValue > 0 && neighborValue < value)
				result.add(neighbor);
		}
		
		return result;
	}
	
	/**
	 * Represents a {@link Tile} with the addition of weights that is
	 * being used internally in the path finding algorithm to move the
	 * robot across the map.
	 * 
	 * @author Simon Grossmann
	 * @since 27 Aug 2019
	 */
	private static final class MapTile extends Tile {
		
		/**
		 * The value of the {@link MapTile tile}. This value is used as weight for
		 * this tile.
		 */
		public int value;
		
		/**
		 * Creates a new undefined {@link MapTile tile} to represent
		 * the given position.
		 * 
		 * @param x The x coordinate of the tile.
		 * @param y The y coordinate of the tile.
		 */
		public MapTile(final int x, final int y) {
			super(null, x, y, Material.UNDEFINED);
			this.value = UNDEFINED_VALUE;
		}
		
		/**
		 * Updates the properties of this {@link MapTile tile}
		 * to mirror the given {@link Tile tile} on the map.
		 * <br>
		 * This updates only properties necessary for the path finding,
		 * which are the following:
		 * <ul>
		 * <li>the material of the tile</li>
		 * <li>whether there is a visitor which would block the tile</li>
		 * <li>the item on the tile</li>
		 * </ul>
		 * 
		 * @param tile The tile on the map.
		 */
		public void update(final Tile tile) {
			this.setMaterial(tile.getMaterial());
			this.setVisitor(tile.getVisitor());
			this.setItem(tile.getItem());
		}
	}
}
