package com.github.schnupperstudium.robots.client.ai.hidden;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.github.schnupperstudium.robots.entity.Facing;
import com.github.schnupperstudium.robots.entity.item.Star;
import com.github.schnupperstudium.robots.world.Map;
import com.github.schnupperstudium.robots.world.Material;
import com.github.schnupperstudium.robots.world.Tile;

public class DistanceScalingMap implements Map {
	
	private static final int STAR_VALUE = 1;
	private static final int UNDEFINED_VALUE = 100;
	
	private Tile[][] mapTiles;
	private int[][] values;
	private int width;
	private int height;
	
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;
	
	public DistanceScalingMap() {
		this.width = 0;
		this.height = 0;
		this.minX = Integer.MAX_VALUE;
		this.maxX = Integer.MIN_VALUE;
		this.minY = Integer.MAX_VALUE;
		this.maxY = Integer.MIN_VALUE;
		this.mapTiles = new Tile[width][height];
		this.values = new int[width][height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				this.mapTiles[x][y] = new Tile(null, x, y, Material.UNDEFINED);
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
		
		if (!isWithinMap(vMinX, vMinY) || !isWithinMap(vMaxX, vMaxY)) 
			resizeMap(Math.min(this.minX, vMinX), Math.min(this.minY, vMinY), Math.max(this.maxX, vMaxX + 1), Math.max(this.maxY, vMaxY + 1));
		
		tiles.forEach(this::updateTile);
		
	}
	
	private void resizeMap(int nMinX, int nMinY, int nMaxX, int nMaxY) {
		int nWidth = nMaxX - nMinX;
		int nHeight = nMaxY - nMinY;
		Tile[][] nTiles = new Tile[nWidth][nHeight];
		int[][] nValues = new int[nWidth][nHeight];
		for (int i = 0; i < nWidth; i++) {
			int x = i + nMinX;
			for (int j = 0; j < nHeight; j++) {
				int y = j + nMinY;
				if (isWithinMap(x, y)) {
					nTiles[i][j] = mapTiles[x - minX][y - minY];
					nValues[i][j] = values[x - minX][y - minY];
				} else
					nTiles[i][j] = new Tile(null, x, y, Material.UNDEFINED);
			}
		}
		
		this.mapTiles = nTiles;
		this.values = nValues;
		this.minX = nMinX;
		this.minY = nMinY;
		this.maxX = nMaxX;
		this.maxY = nMaxY;
		this.width = nWidth;
		this.height = nHeight;
	}
	
	private void updateTile(Tile tile) {
		int x = tile.getX();
		int y = tile.getY();
		
		Tile mapTile = mapTiles[x - minX][y - minY];
		mapTile.setMaterial(tile.getMaterial());
		mapTile.setVisitor(tile.getVisitor());
		mapTile.setItem(tile.getItem());
	}
	
	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public Tile getTile(int x, int y) {
		if (isWithinMap(x, y))
			return mapTiles[x - minX][y - minY];
		else
			return new Tile(null, x, y, Material.UNDEFINED);
	}
	
	public int getValue(Tile tile) {
		return getValue(tile.getX(), tile.getY());
	}
	
	public int getValue(int x, int y) {
		if (isWithinMap(x, y))
			return values[x - minX][y - minY];
		else
			return UNDEFINED_VALUE;
	}
	
	public void setValue(Tile tile, int v) {
		setValue(tile.getX(), tile.getY(), v);
	}
	
	public void setValue(int x, int y, int v) {
		if (isWithinMap(x, y))
			values[x - minX][y - minY] = v;
	}
	
	@Override
	public int getMinX() {
		return minX;
	}
	
	@Override
	public int getMaxX() {
		return maxX;
	}
	
	@Override
	public int getMinY() {
		return minY;
	}
	
	@Override
	public int getMaxY() {
		return maxY;
	}
	
	public boolean isWithinMap(int x, int y) {
		return x >= minX && x < maxX && y >= minY && y < maxY;
	}
	
	public Tile getNextTile(Tile beneathTile) {
		this.values = new int[width][height];
		setValue(beneathTile, Integer.MAX_VALUE);
		
		final Queue<Tile> queue = new LinkedList<>();
		for (int i = 0; i < mapTiles.length; i++) {
			for (int j = 0; j < mapTiles[i].length; j++) {
				final Tile tile = mapTiles[i][j];
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
}
