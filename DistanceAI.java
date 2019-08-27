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

import com.github.schnupperstudium.robots.ai.action.EntityAction;
import com.github.schnupperstudium.robots.client.AbstractAI;
import com.github.schnupperstudium.robots.client.RobotsClient;
import com.github.schnupperstudium.robots.entity.Facing;
import com.github.schnupperstudium.robots.entity.item.Star;
import com.github.schnupperstudium.robots.world.Tile;

/**
 * An AI which has the goal to first explore the whole map and then do different
 * actions weighted by different metrics. The AI weights tasks by the following list:
 * <ul>
 * <li>Explore the map</li>
 * <li>Pick up all stars</li>
 * <li>Find and use keys to open doors</li>
 * <li>Find and use laser charges to destroy blocks</li>
 * </ul>
 * To see which task the AI can do next, it uses a {@link DistanceScalingMap} in the
 * background.
 * 
 * @author Simon Grossmann
 * @since 27 Aug 2019
 */
public final class DistanceAI extends AbstractAI {

	private final DistanceScalingMap map;
	
	/**
	 * Creates a new {@link DistanceAI}.
	 * <br>
	 * This constructor is being invoked implicitly by the framework.
	 * 
	 * @param client The {@link RobotsClient client} the AI runs on.
	 * @param gameId The id of the game the AI participates in.
	 * @param entityUUID The unique id of this AI.
	 */
	public DistanceAI(final RobotsClient client, final long gameId, final long entityUUID) {
		super(client, gameId, entityUUID);
		
		// initialize and open the map view
		this.map = new DistanceScalingMap();
		openMapView();
	}
	
	@Override
	public EntityAction makeTurn() {
		if (hasStar(getBeneathTile()))
			return EntityAction.pickUpItem();
		
		this.map.updateMap(getVision());
		updateMap(this.map);
		
		final Tile target = this.map.getNextTile(getX(), getY());
		if (target != null)
			return convertToActions(target);
		
		return EntityAction.noAction();
	}
	
	private boolean hasStar(final Tile tile) {
		return tile.hasItem() && tile.getItem() instanceof Star;
	}
	
	private EntityAction convertToActions(final Tile target) {
		if (target.getX() == getBeneathTile().getX() && target.getY() == getBeneathTile().getY()
				|| Math.abs(target.getX() - getBeneathTile().getX()) > 1
				|| Math.abs(target.getY() - getBeneathTile().getY()) > 1)
			return EntityAction.noAction();
		
		Facing currentFacing = getFacing();
		final Facing targetFacing = getFacing(target);
		if (targetFacing == currentFacing.left()) {
			return EntityAction.turnLeft();
		} else if (currentFacing != targetFacing) {
			return EntityAction.turnRight();
		}
		
		final int x = getBeneathTile().getX();
		final int y = getBeneathTile().getY();
		if (target.getX() != x || target.getY() != y) {
			return EntityAction.moveForward();
		}
		
		return EntityAction.noAction();
	}
	
	private Facing getFacing(final Tile target) {
		final int dx = target.getX() - getBeneathTile().getX();
		final int dy = target.getY() - getBeneathTile().getY();
		return Facing.of(dx, dy);
	}
}
