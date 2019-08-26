package com.github.schnupperstudium.robots.client.ai.hidden;

import com.github.schnupperstudium.robots.ai.action.EntityAction;
import com.github.schnupperstudium.robots.client.AbstractAI;
import com.github.schnupperstudium.robots.client.RobotsClient;
import com.github.schnupperstudium.robots.entity.Facing;
import com.github.schnupperstudium.robots.entity.item.Star;
import com.github.schnupperstudium.robots.world.Tile;

public final class DistanceAI extends AbstractAI {

	private final DistanceScalingMap map;
	
	public DistanceAI(RobotsClient client, long gameId, long entityUUID) {
		super(client, gameId, entityUUID);
		this.map = new DistanceScalingMap();
		openMapView();
	}
	
	@Override
	public EntityAction makeTurn() {
		if (hasStar(getBeneathTile()))
			return EntityAction.pickUpItem();
		
		this.map.updateMap(getVision());
		updateMap(this.map);
		
		final Tile target = this.map.getNextTile(getBeneathTile());
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
