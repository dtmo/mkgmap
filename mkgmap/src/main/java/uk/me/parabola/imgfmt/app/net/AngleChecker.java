/*
 * Copyright (C) 2015
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 or
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */
package uk.me.parabola.imgfmt.app.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import uk.me.parabola.log.Logger;
import uk.me.parabola.util.EnhancedProperties;
import uk.me.parabola.mkgmap.reader.osm.FakeIdGenerator;

/**
 * Find sharp angles at junctions. The Garmin routing algorithm doesn't
 * like to route on roads building a sharp angle. It adds a time penalty
 * from 30 to 150 seconds and often prefers small detours instead.
 * The penalty depends on the road speed and the vehicle, for pedestrian
 * mode it is zero, for bicycles it is rather small, for cars it is high.
 * The sharp angles typically don't exist in the real world, they are
 * caused by the simplifications done by mappers.
 *
 * Maps created for cyclists typically "abuse" the car routing for racing
 * bikes, but in this scenario the time penalties are much too high,
 * and detours are likely.
 *
 * This method tries to modify the initial heading values of the arcs
 * which are used to calculate the angles. Where possible, the values are
 * changed so that angles appear larger.
 *
 * @author Gerd Petermann
 *
 * Somehow these penalties are also applied to "shortest" routing,
 * often resulting in something that is definitely not the shortest
 * route.
 *
 * Possibly a more significant problem is the very low cost Garmin
 * gives to a minor road crossing a more major road - much less than
 * the "against driving-side" turn onto the major road.  If there is a
 * small block/triangle of roads on the other side, the routing
 * algorithm might prefer crossing the major road then multiple
 * driving-side turns rather than the correct single turn.
 *
 * I haven't found a way of preventing this.
 *
 * Ticker Berkin
 *
 * This code has two other functions:
 *
 * When increasing a sharp angle, try to trigger better turn-instructions by the
 * choice of which arcs to adjust.
 *
 * Use compactDir format (4bits) rather than 8bits for initial heading representaton
 * when this will give a space saving and not compromise the turn-instructions.
 */
public class AngleChecker {
	private static final Logger log = Logger.getLogger(AngleChecker.class);

	private boolean ignoreSharpAngles;
	private boolean cycleMap;

	// Generally it is safe to use compactDirs when there are no arcs in consecutive
	// 22.5 degree sectors and this is guaranteed if the minumum angle is >= 45 degrees.
	private static final float COMPACT_DIR_DEGREES = 45+1; // +bit for 360>256 conversion rounding
	// If this is >= above, then compactDirs will be used unless other, non-vehicle access angles are sharp
	private static final float SHARP_DEGREES = COMPACT_DIR_DEGREES;
	// Experimentation found no benefit in increasing angle beyond 2 "sectors"
	private static final float MIN_ANGLE = 23f; // don't reduce angles to less than this (1 sector + bit)
	private static final float STRAIGHT_EPSILON = 3f; // for angle.straightEnough()

	// helper class to collect multiple arcs with (nearly) the same initial headings
	private class ArcGroup {
		float initialHeading;
		int isOneWayTrueCount;
		int isForwardTrueCount;
		int maxRoadSpeed;
		int maxRoadClass;
		byte orAccessMask;
		List<RouteArc> arcs = new ArrayList<>();

		public void addArc(RouteArc arc) {
			arcs.add(arc);
			if (arc.getRoadDef().isOneway())
				isOneWayTrueCount++;
			if (arc.isForward())
				isForwardTrueCount++;
			if (arc.getRoadDef().getRoadSpeed() > maxRoadSpeed)
				maxRoadSpeed = arc.getRoadDef().getRoadSpeed();
			if (arc.getRoadDef().getRoadClass() > maxRoadClass)
				maxRoadClass = arc.getRoadDef().getRoadClass();
			orAccessMask |= arc.getRoadDef().getAccess();
		}

		public float getInitialHeading() {
			return initialHeading;
		}

		public boolean isOneway() {
			// ??? ignore for pedestrian only group members
			return isOneWayTrueCount == arcs.size();
		}

		public boolean isForward() {
			return isForwardTrueCount == arcs.size();
		}

		public boolean sameWay(ArcGroup other) {
			for (RouteArc thisArc : arcs)
				for (RouteArc otherArc : other.arcs) {
					// same roadDefs have same wayIds. WayIds are unique, so this is OK
					if (thisArc.getRoadDef() == otherArc.getRoadDef())
						return true;
				}
			return false;
		}

		public boolean sameName(ArcGroup other) {
			for (RouteArc thisArc : arcs) {
				String thisName = thisArc.getRoadDef().getName();
				// RoadDefs also have 4 labels, but not bothering to check them
				if (thisName != null)
					for (RouteArc otherArc : other.arcs)
						if (thisName.equals(otherArc.getRoadDef().getName()))
							return true;
			}
			return false;
		}

		public void modInitialHeading(float modIH) {
			initialHeading += modIH;
			if (initialHeading >= 180)
				initialHeading -= 360;
			else if (initialHeading < -180)
				initialHeading += 360;
			log.info("modInitialHeading arc from", arcs.get(0).getInitialHeading(), "by", modIH, "to", initialHeading);

			for (RouteArc arc : arcs) {
				arc.modInitialHeading(modIH);
			}
		}

		public String toString() {
			return arcs.get(0).toString();
		}
	}

	public void config(EnhancedProperties props) {
		// undocumented option - usually used for debugging only
		ignoreSharpAngles = props.getProperty("ignore-sharp-angles", false);
		cycleMap = props.getProperty("cycle-map", false);
	}

	public void check(Map<Integer, RouteNode> nodes) {
		if (!ignoreSharpAngles){
/*
I don't understand the original setting and logic for sharpAnglesCheckMask.

If it is a cycle-map, sharpAnglesCheckMask is set to all but ACCESS...FOOT,
so that, if the common access of the arcs is FOOT only, the sharp angle won't
be increased as if for vehicles, but there is logic to do this anyway.

If not a cycle-map, it was set to ACCESS...BIKE,
so that, if BIKE access isn't common to both arcs, the sharp angle won't
be increased, so motor vehicles, allowed to turn here, might suffer
a time penalty.

I've left the cycle-map value untouched but changed the other value so that
angle expansion is handled as normal

Ticker Berkin 23-Aug-2024

			byte sharpAnglesCheckMask = cycleMap ? (byte) (0xff & ~AccessTagsAndBits.FOOT) : AccessTagsAndBits.BIKE;
*/
			byte sharpAnglesCheckMask = cycleMap ? (byte) (0xff & ~AccessTagsAndBits.FOOT) : (byte) 0xff;
			for (RouteNode node : nodes.values()){
				fixSharpAngles(node, sharpAnglesCheckMask);
			}
		}
	}

	public void fixSharpAngles(RouteNode node, byte sharpAnglesCheckMask) {

		List<RouteArc> arcs = node.getArcs();
		if (arcs.size() <= 1) // nothing to do - maybe an edge. Will use 8bit dirs if there is an arc
			return;
		// RouteNode default is setUseCompactDirs(false);
		node.setUseCompactDirs(true);  // assume algo will do enough to make this OK
		if (arcs.size() == 2) { // common case where two roads join but isn't a junction
			doSimpleJoin(node, arcs);
			return;
		}

		// Combine arcs with nearly the same initial heading.

		// first get direct arcs leaving the node
		List<RouteArc> directArcs = new ArrayList<>();
		for (RouteArc arc : arcs) {
			if (arc.isDirect())
				directArcs.add(arc);
			else
				// AngleChecker runs before addArcsToMajorRoads so there shouldn't be any indirect arcs yet.
				// If this changes, extra care needs to be taken check they are positioned correctly in the list
				// of arcs or that their heading is kept consistent with changes to their direct base arc.
				log.warn("Unexpected indirect arc", arc, "from", node);
		}
		if (directArcs.size() <= 1)
			return; // should not happen

		// sort the arcs by initial heading
		directArcs.sort((ra1,ra2) -> {
			int d = Float.compare(ra1.getInitialHeading(), ra2.getInitialHeading());
			if (d != 0)
				return d;
			d = Integer.compare(ra1.getPointsHash(), ra2.getPointsHash());
			if (d != 0)
				return d;
			return Long.compare(ra1.getRoadDef().getId() , ra2.getRoadDef().getId());
		});

		// now combine into groups
		// also calculate minimum angle between arcs for quick decision if more needs to be done
		List<ArcGroup> arcGroups = new ArrayList<>();
		Iterator<RouteArc> iter = directArcs.listIterator();
		RouteArc arc1 = iter.next();
		boolean addArc1 = false;
		float smallestAngle = 180;
		while (iter.hasNext() || addArc1) {
			ArcGroup ag = new ArcGroup();
			ag.initialHeading = arc1.getInitialHeading();
			ag.addArc(arc1);
			arcGroups.add(ag);
			addArc1 = false;
			while (iter.hasNext()) {
				RouteArc arc2 = iter.next();
				float angleBetween = arc2.getInitialHeading() - ag.initialHeading;
				if (angleBetween < 1) {
					if (arc1.getDest() != arc2.getDest() && arc1.getRoadDef().getId() != arc2.getRoadDef().getId())
						log.warn("sharp angle < 1° at", node.getCoord(), ",maybe duplicated OSM way with bearing", getCompassBearing(arc1.getInitialHeading()));
					ag.addArc(arc2);
				} else {
					if (angleBetween < smallestAngle)
						smallestAngle = angleBetween;
					arc1 = arc2;
					if (!iter.hasNext())
						addArc1 = true;
					break;
				}
			}
		}
		// handle the last > first groups
		int lastInx = arcGroups.size()-1;
		if (lastInx > 0) {
			float angleBetween = arcGroups.get(0).initialHeading - arcGroups.get(lastInx).initialHeading + 360;
			if (angleBetween < 1) {
				for (RouteArc arc : arcGroups.get(lastInx).arcs)
					arcGroups.get(0).addArc(arc);
				arcGroups.remove(lastInx);
			} else if (angleBetween < smallestAngle) {
				smallestAngle = angleBetween;
			}
		}

		if (smallestAngle >= SHARP_DEGREES && arcGroups.size() == arcs.size()) // show diags if real groups
			return;

		if (log.isInfoEnabled()) {
			log.info("fixSharpAngles at", node.getCoord(),
					 "mask", String.format("0x%02x", sharpAnglesCheckMask),
					 "nArcs", arcs.size(),
//					 "nDirectArcs", directArcs.size(),  // already warned
					 arcGroups.size() < arcs.size() ? "nGroups " + arcGroups.size() : "",
					 "smallestAngle", smallestAngle);
			float lastHeading = directArcs.get(directArcs.size()-1).getInitialHeading() - 360;
			for (RouteArc arc : directArcs) {
				RoadDef rd = arc.getRoadDef();
				log.info("Arc", /*arc,*/
						 "heading", arc.getInitialHeading(), getCompassBearing(arc.getInitialHeading()),
						 "angToPrev", arc.getInitialHeading() - lastHeading,
						 "class", rd.getRoadClass(),
						 "speed", rd.getRoadSpeed(),
//						 "rdDef", System.identityHashCode(rd),
						 "way", rd.getId(), "isFake", FakeIdGenerator.isFakeId(rd.getId()),
						 "name", rd.getName(),
						 "access", String.format("0x%02x", rd.getAccess()),
						 "oneway", rd.isOneway(),
						 "forward", arc.isForward(),
						 "paved", rd.paved()
						 );
				lastHeading = arc.getInitialHeading();
				}
		}

		if (arcGroups.size() == 1) { // 1 set of close but possibly different headings, use 8bit dirs
			node.setUseCompactDirs(false);
			return;
		} else if (smallestAngle >= SHARP_DEGREES)
			return;

		final int n = arcGroups.size();
		// scan the angles and see what needs attention
		// Note: This algorithm won't spot and fix a sharp angle where there is a 'no-access' arc between
		// but, because no-access angles are widend (if possible) to 23 degrees, there should
		// be 46 degrees between the two roads either side

		class AngleAttr {
			float angle;
			float minAngle;

			private boolean straightEnough() {
				return angle > 180-STRAIGHT_EPSILON && angle < 180+STRAIGHT_EPSILON;
			}

		}

		boolean someNeedIncrease = false;
		AngleAttr[] angles = new AngleAttr[n];
		for (int i = 0; i < n; i++){
			ArcGroup ag1 = arcGroups.get(i);
			ArcGroup ag2 = arcGroups.get(i+1 < n ? i+1 : 0);
			AngleAttr aa = new AngleAttr();
			angles[i] = aa;
			aa.angle = ag2.getInitialHeading() - ag1.getInitialHeading();
			if (i+1 >= n)
				aa.angle += 360;

//			int sumSpeeds = ag1.maxRoadSpeed + ag2.maxRoadSpeed;
			// the Garmin algorithm sees rounded values, so the thresholds are probably
			// near 22.5 (0x10), 45(0x20), 67.5 (0x30), 90, 112.5 (0x40)

			// the following code doesn't seem to improve anything, I leave it as comment
			// for further experiments.
//			if (cycleMap){
//				if (sumSpeeds >= 14)
//					maskedMinAngle = 0x80;
//				if (sumSpeeds >= 12)
//					maskedMinAngle = 0x70;
//				if (sumSpeeds >= 10)
//					maskedMinAngle = 0x60;
//				if (sumSpeeds >= 8)
//					maskedMinAngle = 0x50;
//				else if (sumSpeeds >= 6)
//					maskedMinAngle = 0x40;
//				else if (sumSpeeds >= 4)
//					maskedMinAngle = 0x30;
//			}
			// With changes to switch compactDirs and working in degrees rather than masked sectors,
			// the above variables are wrong, but the idea holds

			aa.minAngle = MIN_ANGLE;
			String minimalReason = null;
			byte pathAccessMask = (byte) (ag1.orAccessMask & ag2.orAccessMask);
			if (pathAccessMask == 0)
				minimalReason = "no common vehicle allowed on both arcs";
			else if (pathAccessMask == AccessTagsAndBits.FOOT)
				minimalReason = "only pedestrians - sharp angle not a problem";
			else if ((pathAccessMask & sharpAnglesCheckMask) == 0)  // see comment in check(...) above
				minimalReason = "because it can not be used by bike";
			else if (Math.min(ag1.maxRoadSpeed, ag2.maxRoadSpeed) == 0)
				minimalReason = "eg service/parking where sharp angle probably indicates shouldn't turn here";
			else if (ag1.isOneway() && ag2.isOneway()) {
				if (!ag1.isForward() && !ag2.isForward()) {
					minimalReason = "two one-ways merge";
					if (aa.angle < MIN_ANGLE)
						aa.minAngle = aa.angle; // don't expand. compactDir format no problem
				} else if (ag1.isForward() && ag2.isForward()) {
					minimalReason = "two one-ways split";
					if (aa.angle < MIN_ANGLE && n == 3 && ag1.maxRoadClass == ag2.maxRoadClass && ag1.sameName(ag2)) {
						// maybe this is a major road split and don't want to encourage a turn-instruction
						ArcGroup ag3 = arcGroups.get(i+2 < n ? i+2 : i+2-n);
						if (ag3.isOneway() && ag3.maxRoadClass == ag1.maxRoadClass) {
							minimalReason = "major road split";
							aa.minAngle = aa.angle; // don't expand
							node.setUseCompactDirs(false); // use 8bit dirs
						}
					}
				} /* else if (n == 3) {
					// both arcs are one-ways in opposite directions (ie continuous)
					// could check that the one-way directions correspond to the driving side
					minimalReason = "because it seems to be a flare road";
					// it could also be a sharp turn, so best to let it expand. Flare angles
					// are typically 30-45 degrees so expanding to SHARP_DEGREES won't be significant
				} */
			} /* else if (ag1.sameWay(ag2)) {
				 // I don't think this is a good reason to ignore the sharp angle
				 // however all the cases I've found are where there is a flair road with
				 // an extra track that stops the above test working
				minimalReason = "because both arcs belong to the same road";
			} */
			if (minimalReason != null) {
				if (log.isInfoEnabled()) {
					log.info(aa.angle < aa.minAngle ? "minAngleSharp" : "minAngleOK", aa.angle,
							 "minAngle", aa.minAngle,
							 "reason", minimalReason);
				}
			} else
				aa.minAngle = SHARP_DEGREES;
			if (aa.angle < aa.minAngle)
				someNeedIncrease = true;
		}

		if (!someNeedIncrease)
			return;

		// go through the angles again and try to increase any that are less than minAngle
		for (int i = 0; i < n; i++){
			AngleAttr aa = angles[i];
			float wantedIncrement = aa.minAngle - aa.angle;
			if (wantedIncrement <= 0)
				continue;
			float oldAngle = aa.angle;
			ArcGroup ag1 = arcGroups.get(i);
			ArcGroup ag2 = arcGroups.get(i+1 < n ? i+1 : 0);

			// XXX restrictions ?
			AngleAttr predAA = angles[i == 0 ? n - 1 : i - 1];
			AngleAttr nextAA = angles[i >= n - 1 ? 0 : i + 1];

			// we can increase the angle by changing the heading values of one or both arcs
			// How much we can encroach on the adjacent angles
			float deltaPred = predAA.angle - predAA.minAngle;
			float deltaNext = nextAA.angle - nextAA.minAngle;

			if (log.isInfoEnabled()) {
				log.info("Angle", aa.angle,
						 "between", ag1.getInitialHeading(),
						 "and", ag2.getInitialHeading(),
						 "minAngle", aa.minAngle,
						 "wantedInc", wantedIncrement,
						 "deltaPred", deltaPred,
						 "deltaNext", deltaNext);
			}

			if (deltaNext > 0 && deltaPred > 0) { // can take from both
				ArcGroup ag0 = null;
				ArcGroup ag3 = null;
				int chooseWhich = 0; // -ve to take from prev, +ve take from next
				// To influence possible turn-instructions look at the information
				// easily available to prefer altering one arc instead of both.
				// If both are altered this can make turn-instructions even more likely
				// to be wrong because the adjustment is in proportion to the space
				// available.
				// The order of {road_class, sameWay, straightness, roadName, road_speed}
				// clauses can adjusted for priority.
				// Hoping sameWay before road_class fixes more problems than it might cause

				if (chooseWhich == 0) {
					// if the two arcs either side of the sharp angle belong to the same
					// way then change the arc on the other side
					ag0 = arcGroups.get(i > 0 ? i-1 : n-1);
					ag3 = n == 3 ? ag0 : arcGroups.get(i+2 < n ? i+2 : i+2-n);
					if (ag2.sameWay(ag3))
						chooseWhich = -1;
					else if (ag1.sameWay(ag0))
						chooseWhich = +1;
				}
				if (chooseWhich == 0)
					// adjust the lower class road
					chooseWhich = ag1.maxRoadClass - ag2.maxRoadClass;
				if (chooseWhich == 0) {
					// where there is a straight road with a sharp turn-off then just adjust the turn-off
					if (nextAA.straightEnough())
						chooseWhich = -1;
					else if (predAA.straightEnough())
						chooseWhich = +1;
				}
				if (chooseWhich == 0) {
					// NB set ag0 & 3 if move this before sameWay()
					// Way might have changed but could still be the same road by name or ref
					// This is best after straightEnough.
					if (ag2.sameName(ag3))
						chooseWhich = -1;
					else if (ag1.sameName(ag0))
						chooseWhich = +1;
				}
				if (chooseWhich == 0)
					// adjust the slower road
					chooseWhich = ag1.maxRoadSpeed - ag2.maxRoadSpeed;

				if (chooseWhich == 0 ) { // take from both in ratio to available
					deltaNext = Math.min(deltaNext, wantedIncrement * deltaNext / (deltaNext + deltaPred));
					deltaPred = Math.min(deltaPred, wantedIncrement - deltaNext);
				} else if (chooseWhich > 0) { // take as much as poss from next
					if (deltaNext >= wantedIncrement) {
						deltaNext = wantedIncrement;
						deltaPred = 0;
					} else {
						deltaPred = Math.min(deltaPred, wantedIncrement - deltaNext);
					}
				} else { // take as much as possible from pred
					if (deltaPred >= wantedIncrement) {
						deltaPred = wantedIncrement;
						deltaNext = 0;
					} else {
						deltaNext = Math.min(deltaNext, wantedIncrement - deltaPred);
					}
				}
			} else if (deltaNext > 0) {
				if (deltaNext > wantedIncrement)
					deltaNext = wantedIncrement;
			} else {
				if (deltaPred > wantedIncrement)
					deltaPred = wantedIncrement;
			}

			if (deltaNext > 0) { // can take some from the following angle
				log.info("increasing arc with heading", getCompassBearing(ag2.getInitialHeading()), "by", deltaNext);
				ag2.modInitialHeading(deltaNext);
				aa.angle += deltaNext;
				nextAA.angle -= deltaNext;
				wantedIncrement -= deltaNext;
			}
			if (deltaPred > 0) { // and some from the previous angle
				log.info("decreasing arc with heading", getCompassBearing(ag1.getInitialHeading()), "by", deltaPred);
				ag1.modInitialHeading(-deltaPred);
				aa.angle += deltaPred;
				predAA.angle -= deltaPred;
				wantedIncrement -= deltaPred;
			}

			if (wantedIncrement > 0) {
				node.setUseCompactDirs(false);  // angle might be wide enough in 8bit dirs
				if (aa.angle == oldAngle)
					log.info("don't know how to fix it", wantedIncrement);
				else
					log.info("don't know how to enlarge it further", wantedIncrement);
			}
		}

	}


	/**
	 * Called when there are exactly 2 arcs on the routing node.
	 * This is a common case where a road changes speed/name/class.
	 * It just checks for the angle of the 2 roads and increases it if sharp to reduce any potential
	 * excessive cost for the section so it won't be avoided unneccessarily by routing decisions elsewhere.
	 * Could check that both arcs usable by vehicles and do nothing if not, but hardly worth the bother.
	 *
	 * @param node
	 * @param arcs
	 */
	private static void doSimpleJoin(RouteNode node, List<RouteArc> arcs) {
		RouteArc arc1 = arcs.get(0);
		RouteArc arc2 = arcs.get(1);
		float angle = arc1.getInitialHeading() - arc2.getInitialHeading();
		float extra = 0; // signed amount where +ve increases arc1 & decreases arc2
		if (angle > 360 - SHARP_DEGREES) // crosses -180
			extra = 360 - angle - SHARP_DEGREES;
		else if (angle < SHARP_DEGREES - 360) // ditto the other way
			extra = SHARP_DEGREES - angle - 360;
		else if (angle > 0) {
			if (angle < SHARP_DEGREES)
				extra = SHARP_DEGREES - angle;
		} else if (angle < 0) {
			if (angle > -SHARP_DEGREES)
				extra = -angle - SHARP_DEGREES;
		} // else angle == 0 and can't widen as don't know which way around to do it
		if (extra != 0) {
			if (log.isInfoEnabled())
				log.info("join angle", angle, "° at", node.getCoord(), "increased by", extra);
			arc1.modInitialHeading(+extra/2);
			arc2.modInitialHeading(-extra/2);
		}
	}


	/**
	 * for log messages
	 */
	private static String getCompassBearing (float bearing){
		float cb = (bearing + 360) % 360;
		return Math.round(cb) + "°";
	}

}
