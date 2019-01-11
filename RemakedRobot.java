package pbltest;

import static robocode.util.Utils.*;

import java.awt.Color;
import java.util.ArrayList;

import robocode.ScannedRobotEvent;
import robocode.TeamRobot;

public class RemakedRobot extends TeamRobot {
	static ArrayList<AliveRobot> robotList;
	AliveRobot currentTarget;

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

		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
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
		if (robotDistance > 200 || getEnergy() < 15) return 0;
		else if (robotDistance > 100) return 1;
		else if (robotDistance > 50) return 2;
		else return 3;
	}

	// 円形予測に基づく相手の予測地点に対する角度を算出
	private double enkeiYosoku(ScannedRobotEvent e, long diff, double angularVelocity) {
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

	private void enkeiShageki(ScannedRobotEvent e, long diff, double angularVelocity) {
		double energy = determineEnergy(e.getDistance());
		turnGunRight(normalRelativeAngleDegrees(enkeiYosoku(e, diff, angularVelocity) - getGunHeading()));
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

	
	
         private void setRadar(ScannedRobotEvent e){

	 double turnradar1;
         double turnradar2;

	    if(scanEnemy){
		if (Math.abs(angularVelocity) > 0.00001) {
			//角度の変化が大きいときは、円形予測
			turnradar1=enkeiYosoku(e, diff, angularVelocity)-getRadarHeading();
		}else {
			turnradar1=senkeiYosoku(e)-getRadarHeading();
		}

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

	    else {

	    	turnRadarRight(360);

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
		public AliveRobot(ScannedRobotEvent e, long t, double bearing) {
			setName(e.getName());
			setX(e.getDistance(), bearing); // bearing = getHeading() + e.getBearing();
			setY(e.getDistance(), bearing);
			setVelocity(e.getVelocity());
			setHead(e.getHeading());
			setEnergy(e.getEnergy());
			setTime(t);
		}

		// すべての情報を更新する
		public void update(ScannedRobotEvent e, long t, double bearing) {
			setX(e.getDistance(), bearing); // bearing = getHeading() + e.getBearing();
			setY(e.getDistance(), bearing);
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
		public void setX(double distance, double bearing) {
			x = getX() +distance * Math.sin(Math.toRadians(bearing));
		}
		// y座標
		public double getY() {
			return y;
		}
		public void setY(double distance, double bearing) {
			y = getY() +distance * Math.cos(Math.toRadians(bearing));
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

