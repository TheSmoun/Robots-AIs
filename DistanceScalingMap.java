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
	private Bounds bounds;
	
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
		this.mapTiles = new MapTile[0][0];
		this.bounds = new Bounds();
	}
	
	@Override
	public int getMinX() {
		return this.bounds.x.min;
	}

	@Override
	public int getMaxX() {
		return this.bounds.x.max;
	}

	@Override
	public int getMinY() {
		return this.bounds.y.min;
	}

	@Override
	public int getMaxY() {
		return this.bounds.y.max;
	}

	@Override
	public int getWidth() {
		return this.bounds.width();
	}

	@Override
	public int getHeight() {
		return this.bounds.height();
	}

	@Override
	public MapTile getTile(final int x, final int y) {
		if (this.bounds.contains(x, y))
			return this.mapTiles[x - this.getMinX()][y - this.getMinY()];
		else
			return new MapTile(x, y);
	}

	public void updateMap(final List<Tile> tiles) {
		if (tiles == null || tiles.isEmpty()) {
			return;
		}
		
		final int minX = tiles.stream().mapToInt(Tile::getX).min().getAsInt();
		final int minY = tiles.stream().mapToInt(Tile::getY).min().getAsInt();
		final int maxX = tiles.stream().mapToInt(Tile::getX).max().getAsInt();
		final int maxY = tiles.stream().mapToInt(Tile::getY).max().getAsInt();
		
		if (!this.bounds.contains(minX, minY) || !this.bounds.contains(maxX, maxY)) {
			final Bounds newBounds = this.bounds.expanded(minX, minY, maxX, maxY);
			final MapTile[][] newTiles = new MapTile[newBounds.width()][newBounds.height()];
			
			for (int i = 0; i < newBounds.width(); i++) {
				final int x = i + newBounds.x.min;
				for (int j = 0; j < newBounds.height(); j++) {
					newTiles[i][j] = this.getTile(x, j + newBounds.y.min);
				}
			}
			this.mapTiles = newTiles;
			this.bounds = newBounds;
		}
		
		for (final Tile tile : tiles) {
			this.getTile(tile.getX(), tile.getY()).update(tile);
		}
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
	 * Represents the bounds of the {@link DistanceScalingMap map}.
	 * 
	 * @author Simon Grossmann
	 * @since 27 Aug 2019
	 */
	private static final class Bounds {
		
		/**
		 * The horizontal range.
		 */
		public Range x = new Range();
		
		/**
		 * The vertical range.
		 */
		public Range y = new Range();
		
		/**
		 * The width of the bounds.
		 * 
		 * @return The width of the bounds.
		 */
		public int width() {
			return this.x.size();
		}
		
		/**
		 * The height of the bounds.
		 * 
		 * @return The height of the bounds.
		 */
		public int height() {
			return this.y.size();
		}
		
		/**
		 * Checks whether the given coordinates are within the {@link Bounds}.
		 * 
		 * @param x The x coordinate to check.
		 * @param y The y coordinate to check.
		 * @return <code>True</code> if the coordinates are within the bounds, <code>false</code> otherwise.
		 * 
		 * @see Range#contains(int)
		 */
		public boolean contains(final int x, final int y) {
			return this.x.contains(x) && this.y.contains(y);
		}
		
		/**
		 * Expands the {@link Bounds} so that the given minimum and maximum values
		 * are within the bounds and returns the new, expanded.
		 * 
		 * @param minX The minimum x value.
		 * @param minY The minimum y value.
		 * @param maxX The maximum x value.
		 * @param maxY The maximum y value.
		 * @return The expanded bounds.
		 * 
		 * @see Range#expand(int, int)
		 */
		public Bounds expanded(final int minX, final int minY, final int maxX, final int maxY) {
			final Bounds bounds = new Bounds();
			bounds.x = this.x.expanded(minX, maxX);
			bounds.y = this.y.expanded(minY, maxY);
			return bounds;
		}

		@Override
		public String toString() {
			return "Bounds [x=" + x + ", y=" + y + "]";
		}
	}
	
	/**
	 * Represents range of values. This is used for minimum and maximum
	 * values within the {@link DistanceScalingMap}.
	 * 
	 * @author Simon Grossmann
	 * @since 27 Aug 2019
	 */
	private static final class Range {
		
		/**
		 * The minimum of the {@link Range}.
		 */
		public int min = Integer.MAX_VALUE;
		
		/**
		 * The maximum of the {@link Range}.
		 */
		public int max = Integer.MIN_VALUE;
		
		/**
		 * Returns the size of this {@link Range}.
		 * The size is equivalent to the size of the closed interval
		 * containing both, the minimum and maximum values.
		 * 
		 * @return The size of this range.
		 */
		public int size() {
			return this.max != Integer.MIN_VALUE ? this.max - this.min + 1 : 0;
		}
		
		/**
		 * Checks whether the given value is within the {@link Range}.
		 * 
		 * @param value The value to check.
		 * @return <code>True</code> if the value is within the bounds, <code>false</code> otherwise.
		 */
		public boolean contains(final int value) {
			return value >= this.min && value <= this.max;
		}
		
		/**
		 * Expands this {@link Range} so that the given minimum and maximum
		 * values are within it's range and returns the new, expanded range.
		 * 
		 * @param min The minimum value.
		 * @param max The maximum value.
		 * @return The expanded range.
		 */
		public Range expanded(final int min, final int max) {
			final Range range = new Range();
			range.min = Math.min(this.min, min);
			range.max = Math.max(this.max, max);
			return range;
		}

		@Override
		public String toString() {
			return "Range [min=" + min + ", max=" + max + "]";
		}
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
