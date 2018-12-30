package pbl;

import static robocode.util.Utils.*;

import java.awt.Color;

import robocode.ScannedRobotEvent;
import robocode.TeamRobot;

public class pbl_robot extends TeamRobot {
	private boolean scanEnemy;
    private Target currentTarget = null;
    private ScannedRobotEvent sre = null;

    private double e_rad;
    private double e_lenth;


	public void run(){
		setBodyColor(Color.white);
		setGunColor(Color.black);
		setRadarColor(Color.blue);
		setBulletColor(Color.cyan);
		setScanColor(Color.red);

		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);

		turnRadarRight(360);

		while (true) {

			setTurnRight(e_rad);
		    setAhead(e_lenth - 50);

		    if(sre != null) {
		    	if(!isTeammate(sre.getName())) {
					scanEnemy=true;
				}


		    	setRadar(sre);
		    }

		    execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		sre = e;

		if(currentTarget != null) {
			if (currentTarget.getName().equals(e.getName()) && !isTeammate(currentTarget.getName())) {
				selectFireMode(e);
				return;
			}else {
				currentTarget = null;
			}
		}
		currentTarget = new Target(e.getName(), e.getHeading(), getTime());
	}

	private void setRadar(ScannedRobotEvent e){
	    double turnradar1;
	    double turnradar2;

	    if(scanEnemy){
	    	turnradar1 = getRadarHeading() - e.getBearing() - getHeading();
	    	if(turnradar1 < 180 || -180 < turnradar1){
	    		turnRadarLeft(turnradar1);
	    		turnRadarLeft(45);
	    		turnRadarRight(90);
		    } else {
		    	if(turnradar1 > 0){						//180<turnradar1<360
		    		turnradar2 = 360 - turnradar1;
		    	}else{									//-360<turnradar1<-180
		    		turnradar2 = 360 + turnradar1;
		    	}
		    	turnRadarRight(turnradar2);
		    	turnRadarRight(45);
		    	turnRadarLeft(90);
		    }
	    } else {
	    	turnRadarRight(360);
	    }
	}

	private void selectFireMode(ScannedRobotEvent e) {
		long diff = getTime() - currentTarget.getTime();
		if(diff == 0) diff = 1;
		double angularVelocity = (e.getHeading() - currentTarget.getHead()) / diff;

		if (Math.abs(angularVelocity) > 0.00001) {
			//角度の変化が大きいときは、円形予測
			enkeiShageki(e, diff, angularVelocity);
		}else {
			senkeiShageki(e);
		}

	}

	private double determineEnergy(double robotDistance) {
		if (robotDistance > 200 || getEnergy() < 15)
		return 1;
		else if (robotDistance > 50)
		return 2;
		else
		return 3;
	}

	private void enkeiShageki(ScannedRobotEvent e, long diff, double angularVelocity) {
		double energy = determineEnergy(e.getDistance());
		double bulletVelocity = 20 - 3 * energy;
		double bulletTime = e.getDistance() / bulletVelocity;
		double radius = e.getVelocity() / angularVelocity;
		double dRad = angularVelocity * bulletTime;

		double enemyBearing = getHeading() + e.getBearing();
		double x = getX() + e.getDistance() * Math.sin(Math.toRadians(enemyBearing));
		double y = getY() + e.getDistance() * Math.cos(Math.toRadians(enemyBearing));

		double nextX = x + radius * Math.cos(Math.toRadians(e.getHeading()))
						- radius * Math.cos(Math.toRadians(e.getHeading() + dRad));
		double nextY = y + radius * Math.sin(Math.toRadians(e.getHeading()))
						- radius * Math.sin(Math.toRadians(e.getHeading() + dRad));

		e_rad = e.getBearing();
		e_lenth = e.getDistance();

		if(nextX < 0){
			nextX = 0;
		}else if(nextX > 799){
			nextX = 799;
		}

		if(nextY < 0){
			nextY = 0;
		}else if(nextY > 799){
			nextY = 799;
		}

		double dx = nextX - getX();
		double dy = nextY -getY();
		double theta = Math.toDegrees(Math.atan2(dx, dy));

		System.out.print("x: " + nextX + " , y: " + nextY + ", theta: " + theta + "\n");

		turnGunRight(normalRelativeAngleDegrees(theta - getGunHeading()));
		fire(energy);

		currentTarget.setHead(e.getHeading());
		currentTarget.setTime(getTime());
	}

	private void senkeiShageki(ScannedRobotEvent e) {
		double energy = determineEnergy(e.getDistance());
		double bulletVelocity = 20 - 3 * energy;
		// Calculate enemy bearing
		double enemyBearing = getHeading() + e.getBearing();
		// Calculate enemy's position
		double dx1 = e.getDistance() * Math.sin(Math.toRadians(enemyBearing));
		double dy1 = e.getDistance() * Math.cos(Math.toRadians(enemyBearing));
		double vx = e.getVelocity() * Math.sin(Math.toRadians(e.getHeading()));
		double vy = e.getVelocity() * Math.cos(Math.toRadians(e.getHeading()));

		// dx = dx1 + dx2, dy = dy1 + dy2
		// (bulletVelocity * t)^2 = dx^2 + dy^2
		// t = (-b +- sqrt(b^2 - 4ac)) / 2a
		double a = bulletVelocity * bulletVelocity - e.getVelocity() * e.getVelocity();
		double b = -2 * dx1 * vx - 2d * dy1 * vy;
		double c = - dx1 * dx1 - dy1 * dy1;
		double d = b * b - 4 * a * c;

		double t = -1;
		if(d >= 0) {
			double t1 = (-b + Math.sqrt(d)) / (2 * a);
			double t2 = (-b - Math.sqrt(d)) / (2 * a);

			if (t1 < 0) {
				if (t2 >= 0) { t = t2; }
			} else {
				if (t2 < 0 || t1 < t2) { t = t1; }
				else { t = t2; }
			}
		}

		if(t >= 0) {
			double dx2 = vx * t;
			double dy2 = vy * t;

			double dx = dx1 + dx2;
			double dy = dy1 + dy2;

			// Calculate angle to target
			double theta = Math.toDegrees(Math.atan2(dx, dy));

			double x = dx + getX();
			double y = dy + getY();
			System.out.print("x: " + x + " , y: " + y + ", theta: " + theta + "\n");

			// Turn gun to target
			turnGunRight(normalRelativeAngleDegrees(theta - getGunHeading()));
			fire(energy);
		}
	}

	//**********************************

	void move(double x, double y, double lenth) {
		if(lenth < 100) {
			turnRight(e_rad);
		}else {

		}
	}

	//**********************************

	class Target {
		private String name = null;
		private double head = 0;
		private long time = 0;
		public Target(String n, double h, long t) {
			setName(n);
			setHead(h);
			setTime(t);
		}
		public String getName() {
			return name;
		}
		private void setName(String n) {
			name = n;
		}
		public double getHead() {
			return head;
		}
		private void setHead(double h) {
			head = h;
		}
		public long getTime() {
			return time;
		}
		private void setTime(long t) {
			time = t;
		}
	}
}
