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
import com.github.schnupperstudium.robots.entity.item.LaserCharge;
import com.github.schnupperstudium.robots.world.Tile;

/**
 * An AI which has the goal to first find all stars in the map and then do different
 * actions weighted by different metrics. The AI weights tasks by the following list:
 * <ul>
 * <li>Pick up all stars</li>
 * <li>Find and use keys to open doors</li>
 * <li>Find and use laser charges to destroy blocks</li>
 * <li>Explore the map</li>
 * <li>Use teleporters to reach other areas of the map</li>
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
		this.map = new DistanceScalingMap(this);
		this.openMapView();
	}
	
	@Override
	public EntityAction makeTurn() {
		if (this.getBeneathTile().hasItem())
			return EntityAction.pickUpItem();
		
		this.map.updateMap(this.getVision());
		this.updateMap(this.map);
		
		final Tile target = this.map.getNextTile(this.getX(), this.getY());
		if (target != null)
			return this.convertToActions(target);
		
		return EntityAction.noAction();
	}
	
	private EntityAction convertToActions(final Tile target) {
		if (target.getX() == this.getX() && target.getY() == this.getY()
				|| Math.abs(target.getX() - this.getX()) > 1
				|| Math.abs(target.getY() - this.getY()) > 1)
			return EntityAction.noAction();
		
		final Facing currentFacing = this.getFacing();
		final Facing targetFacing = this.getFacing(target);
		if (targetFacing == currentFacing.left()) {
			return EntityAction.turnLeft();
		} else if (currentFacing != targetFacing) {
			return EntityAction.turnRight();
		}
		
		if (target.getX() != this.getX() || target.getY() != this.getY()) {
			if (this.map.canShootBoulder(target)) {
				return EntityAction.useItem(this.getInventory().findItem(LaserCharge.ITEM_NAME));
			} else if (this.map.hasKeyForGate(target)) {
				return EntityAction.useItem(this.getInventory()
						.findItem(DistanceScalingMap.GATE_KEYS.get(target.getMaterial())));
			} else {
				if (this.map.canUseTeleporter(target)) {
					this.map.useTeleporter(this.getBeneathTile(), target);
				}
				
				return EntityAction.moveForward();
			}
		}
		
		return EntityAction.noAction();
	}
	
	private Facing getFacing(final Tile target) {
		final int dx = target.getX() - this.getX();
		final int dy = target.getY() - this.getY();
		return Facing.of(dx, dy);
	}
}
