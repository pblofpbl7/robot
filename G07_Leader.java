package group07;

import static robocode.util.Utils.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;

public class G07_Leader extends TeamRobot {
	static ArrayList<AliveRobot> robotList;
	static ArrayList<String> deadRobots;
	static ArrayList<String> teammates;
	AliveRobot currentTarget;

	public void run(){
		setBodyColor(Color.white);
		setGunColor(Color.black);
		setRadarColor(Color.blue);
		setBulletColor(Color.cyan);
		setScanColor(Color.red);

		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);

		robotList = new ArrayList<AliveRobot>();
		deadRobots = new ArrayList<String>();
		teammates = new ArrayList<String>(Arrays.asList("G07_Leader","G07_Sub1","GG07_Sub2"));

		while (true) {
			turnRadarRight(360);
			for (AliveRobot aliveRobot : robotList) {
				System.out.println("name: "+aliveRobot.getName() + "  (x,y): ("+aliveRobot.getX()+","+getY()+")");
			}
			System.out.println("\n");
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		int index = 0;
		double bearing = getHeading() + e.getBearing();
		if(!teammates.contains(e.getName())) {
			if(robotList.size() != 0) {
				for (AliveRobot aliveRobot : robotList) {
					if(aliveRobot.getName().equals(e.getName())) {
						break;
					}
					index++;
				}
				if(index >= robotList.size()) {
					if(deadRobots.size() != 0 &&  !deadRobots.contains(e.getName())){
						robotList.add(new AliveRobot(e, getTime(), bearing, getX(), getY()));
					}
				}else{
					updateRobotInfo(robotList.get(index), e, bearing, getTime(), getX(), getY());
				}
			}else{
				robotList.add(new AliveRobot(e, getTime(), bearing, getX(), getY()));
			}
		}
	}

	@Override
	public void onRobotDeath(RobotDeathEvent e) {
		if(deadRobots.size() == 0) {
			addDeadRobots(e);
		}
		else if(!deadRobots.contains(e.getName())){
			addDeadRobots(e);
		}

		int index = 0;
		for (AliveRobot aliveRobot : robotList) {
			if(aliveRobot.getName().equals(e.getName())) {
				index++;
				removeAliveRobot(index);
				break;
			}
		}
	}

	public static synchronized void addDeadRobots(RobotDeathEvent e){
		deadRobots.add(e.getName());
	}

	public static synchronized void removeAliveRobot(int index) {
		robotList.remove(index);
	}

	public static synchronized void updateRobotInfo(AliveRobot aliveRobot, ScannedRobotEvent e,
			double bearing, long time, double x, double y) {
		aliveRobot.update(e, time, bearing, x, y);
	}

	private void selectFireMode(ScannedRobotEvent e) {
		long diff = getTime() - currentTarget.getTime();
		if(diff == 0) diff = 1;
		double angularVelocity = (e.getHeading() - currentTarget.getHead()) / diff;

		if (Math.abs(angularVelocity) > 0.00001) {
			//角度の変化が大きいときは、円形予測
			enkeiShageki(e, angularVelocity);
		}else {
			senkeiShageki(e);
		}

	}

	private double determineEnergy(double robotDistance) {
		if (robotDistance > 200 || getEnergy() < 15) return 0;
		else if (robotDistance > 100) return 1;
		else if (robotDistance > 50) return 2;
		else return 3;
	}

	// 円形予測に基づく相手の予測地点に対する角度を算出
	private double enkeiYosoku(ScannedRobotEvent e, double angularVelocity) {
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
		return Math.toDegrees(Math.atan2(dx, dy));
	}

	private void enkeiShageki(ScannedRobotEvent e, double angularVelocity) {
		double energy = determineEnergy(e.getDistance());
		turnGunRight(normalRelativeAngleDegrees(enkeiYosoku(e, angularVelocity) - getGunHeading()));
		fire(energy);

		currentTarget.setHead(e.getHeading());
		currentTarget.setTime(getTime());
	}

	private double senkeiYosoku(ScannedRobotEvent e) {
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
			return Math.toDegrees(Math.atan2(dx, dy));
		}else return 1000;
	}

	private void senkeiShageki(ScannedRobotEvent e) {
		double point = senkeiYosoku(e);
		if(point == 1000) {
			return;
		}else {
			turnGunRight(normalRelativeAngleDegrees(senkeiYosoku(e) - getGunHeading()));
			fire(determineEnergy(e.getDistance()));
		}
	}


	/*
	 * 保持する情報
	 *  ・名前
	 *  ・(x,y)座標
	 *  ・速度
	 *  ・向き
	 *  ・残りエネルギー
	 *  ・時間
	 */
	class AliveRobot {
		private String name = null;
		private double x = 0;
		private double y = 0;
		private double velocity = 0;
		private double head = 0;
		private double energy = 0;
		private long time = 0;
		public AliveRobot(ScannedRobotEvent e, long t, double bearing,
				double x, double y) {
			setName(e.getName());
			setX(x, e.getDistance(), bearing); // bearing = getHeading() + e.getBearing();
			setY(y, e.getDistance(), bearing);
			setVelocity(e.getVelocity());
			setHead(e.getHeading());
			setEnergy(e.getEnergy());
			setTime(t);
		}

		// すべての情報を更新する
		public void update(ScannedRobotEvent e, long t, double bearing,
				double x, double y) {
			setX(x, e.getDistance(), bearing); // bearing = getHeading() + e.getBearing();
			setY(y, e.getDistance(), bearing);
			setVelocity(e.getVelocity());
			setHead(e.getHeading());
			setEnergy(e.getEnergy());
			setTime(t);
		}

		// 名前
		public String getName() {
			return name;
		}
		public void setName(String n) {
			name = n;
		}
		// x座標
		public double getX() {
			return x;
		}
		public void setX(double getX, double distance, double bearing) {
			x = getX +distance * Math.sin(Math.toRadians(bearing));
		}
		// y座標
		public double getY() {
			return y;
		}
		public void setY(double getY, double distance, double bearing) {
			y = getY +distance * Math.cos(Math.toRadians(bearing));
		}
		// 速さ
		public double getVelocity() {
			return velocity;
		}
		public void setVelocity(double v) {
			velocity = v;
		}
		// 向き
		public double getHead() {
			return head;
		}
		public void setHead(double h) {
			head = h;
		}
		// 残りエネルギー
		public double getEnergy() {
			return energy;
		}
		public void setEnergy(double e) {
			energy = e;
		}
		// 時間
		public long getTime() {
			return time;
		}
		public void setTime(long t) {
			time = t;
		}
	}
}

