package group07;

import static robocode.util.Utils.*;

import java.awt.Color;

import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;

public class G07_Leader extends TeamRobot {
	private boolean scanEnemy;
    private Target currentTarget = null;
    private ScannedRobotEvent sre = null;
    private double circum;

	public void run(){
		setBodyColor(Color.white);
		setGunColor(Color.black);
		setRadarColor(Color.blue);
		setBulletColor(Color.cyan);
		setScanColor(Color.red);

		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);

		turnRadarRight(360);
		move_robot();

		while (true) {
			setAhead(circum);
		    setTurnLeft(360);

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

	@Override
	public void onHitWall(HitWallEvent event) {
		setTurnLeft(240);
		setAhead(circum);
		execute();
	}

	@Override
	public void onHitRobot(HitRobotEvent event) {
		back(circum / (2 * Math.PI));
		setTurnLeft(240);
		setAhead(circum);
		execute();
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

			// Turn gun to target
			turnGunRight(normalRelativeAngleDegrees(theta - getGunHeading()));
			fire(energy);
		}
	}

	//runのループを行う前に実行
	void move_robot(){
		double fieldHeight = getBattleFieldHeight();
		double fieldWidth = getBattleFieldWidth();
		double centerX = (fieldWidth / 2);
		double centerY = (fieldHeight / 2);
		double x = getX();
		double y = getY();
		double mini_wall_y, mini_wall_x, mini_wall; //壁との距離
		double wall_y, wall_x, wall; //最も近い壁
		double rad; //フィールドに宣言

		if(y >= centerY){
			mini_wall_y = 799 - y;
			wall_y = 1;
			//上の壁
		}else{
			mini_wall_y = y;
			wall_y = 2;
		    //下の壁
		}

		if(x <= centerX){
			mini_wall_x = x;
			wall_x = 3;
			//左の壁
		}else{
			mini_wall_x = 799 - x;
			wall_x = 4;
			//右の壁
		}

		if(mini_wall_x >= mini_wall_y){
			mini_wall = mini_wall_y;
			wall = wall_y;
		}else{
			mini_wall = mini_wall_x;
			wall = wall_x;
		}

		rad = mini_wall / 2;
		circum = 2 * Math.PI * (rad);

		if(wall == 1) {
			turnRight(180 - getHeading());
		} else if(wall == 2) {
			turnLeft(getHeading());
		} else if(wall == 3) {
			turnRight(90 - getHeading());
		} else if(wall == 4) {
			turnRight(270 - getHeading());
		}

		ahead(rad);
		turnLeft(80);

		System.out.println("x:"+x);
		System.out.println("y:"+y);
		System.out.println("rad:"+rad);
		System.out.println("nowX:"+getX());
		System.out.println("nowY:"+getY());
	}

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