package group07;

import static robocode.util.Utils.*;

import java.awt.Color;
import java.util.ArrayList;

import robocode.HitRobotEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;

public class G07_Sub2 extends TeamRobot {
	ArrayList<String> deadRobots = new ArrayList<String>();
	AliveRobot currentTarget;
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

		while (true) {
			turnRadarRight(360);
			wall_move();
			if(currentTarget != null) {
				System.out.println(currentTarget.getName());
			}
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {

		if(currentTarget != null && currentTarget.getName().equals(e.getName())) {
			double bearing = getHeading() + e.getBearing();
			selectFireMode(e);
			currentTarget.update(e, getTime(), bearing, getX(), getY());
			e_rad = e.getBearing();
			e_lenth = e.getDistance();
		}
		else if(!isTeammate(e.getName())) {
			determineTarget(e);
		}
	}

	@Override
	public void onRobotDeath(RobotDeathEvent e) {
		if(deadRobots.size() == 0) {
			deadRobots.add(e.getName());
		}
		else if(!deadRobots.contains(e.getName())){
			deadRobots.add(e.getName());
		}
	}

	private void selectFireMode(ScannedRobotEvent e) {
		long diff = getTime() - currentTarget.getTime();
		if(diff == 0) diff = 1;
		double angularVelocity = (e.getHeading() - currentTarget.getHead()) / diff;

		if(e.getDistance() > 200) {
			return;
		}
		else if (Math.abs(angularVelocity) > 0.00001) {
			//角度の変化が大きいときは、円形予測
			enkeiShageki(e, angularVelocity);
		}else {
			senkeiShageki(e);
		}

	}

	private void determineTarget(ScannedRobotEvent e) {
		double bearing = getHeading() + e.getBearing();

		if(currentTarget == null) {
			if(e.getName().contains("Sub1")) {
				currentTarget = new AliveRobot(e, getTime(), bearing, getX(), getY());
			}
		} else if(deadRobots.size() != 0) {
			if(deadRobots.contains(currentTarget.getName())) {
				if(currentTarget.getName().contains("Sub1")) {
					if(e.getName().contains("Sub2")) {
						currentTarget = new AliveRobot(e, getTime(), bearing, getX(), getY());
					} else if(e.getName().contains("Leader")) {
						boolean isInList = false;
						for (String dead : deadRobots) {
							if(dead.contains("Sub2")) {
								isInList = !isInList;
								break;
							}
						}
						if(isInList) {
							currentTarget = new AliveRobot(e, getTime(), bearing, getX(), getY());
						}
					}
				} else if(currentTarget.getName().contains("Sub2")) {
					if(e.getName().contains("Leader")) {
						currentTarget = new AliveRobot(e, getTime(), bearing, getX(), getY());
					} else {
						boolean isInList = false;
						for (String dead : deadRobots) {
							if(dead.contains("Leader")) {
								isInList = !isInList;
								break;
							}
						}
						if(isInList) {
							currentTarget = new AliveRobot(e, getTime(), bearing, getX(), getY());
						}
					}
				} else {
					currentTarget = new AliveRobot(e, getTime(), bearing, getX(), getY());
				}
			}
		}
	}


	private double determineEnergy(double robotDistance) {
		if (robotDistance > 200 || getEnergy() < 15) return 0;
		else if (robotDistance > 150) return 1;
		else if (robotDistance > 100) return 2;
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
		turnGunRight(normalRelativeAngleDegrees(enkeiYosoku(e, angularVelocity) - getGunHeading()));
		fire(determineEnergy(e.getDistance()));
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

	@Override
	public void onHitRobot(HitRobotEvent e) {
		setTurnLeft(20);
		setAhead(-100);
	}

	void main_move() {

		double len, sq_len, a2;
		double rad2;
		double a=100;

		sq_len = Math.pow(e_lenth, 2) - Math.pow(a, 2);
		len = Math.sqrt(sq_len);

		a2 = a/len + a%len;
    	rad2 = 1/( Math.tan(a2) ) + 1%( Math.tan(a2) );

		setTurnRight(e_rad - rad2);
		setAhead(e_lenth - 100);

	}
	void wall_move() {
		double x,y,dx,dy,a;
		double my_rad,rad = 0;
		boolean flag = false;
		boolean wall_1 = false ;  // x = 0 の壁
		boolean wall_2 = false ;  // x = 799 の壁
		boolean wall_3 = false ;  // y = 0 の壁
		boolean wall_4 = false ;  // y = 799  の壁
		//いずれかの壁との距離が一定以下になった場合true

		x = getX();
		y = getY();
		my_rad = getHeading();

		if(x < 50) {
			wall_1 = true;
			flag = true;
		}else if(x > 749) {
			wall_2 = true;
			flag = true;
		}

		if(y < 50) {
			wall_3 = true;
			flag = true;
		}else if(y > 749) {
			wall_4 = true;
			flag = true;
		}

		if(flag) {
			if(wall_1) {
		    if(wall_3) {
		    	dx = 399 - x;
		    	dy = 399 - y;
		    	a = dy/dx + dy%dx;
		    	rad = 1/( Math.tan(a) ) + 1%( Math.tan(a) );
		    	rad = 90 - rad;
		    }else if(wall_4){
		    	dx = 399 - x;
		    	dy = y - 399;
		    	a = dy/dx + dy%dx;
		    	rad = 1/( Math.tan(a) ) + 1%( Math.tan(a) );
		    	rad = 90 + rad;
		    }else {
		    	rad = 90 - my_rad;
		    }
		}

			if(wall_2) {
				if(wall_3) {
					dx = x - 399;
					dy = 399 - y;
					a = dy/dx + dy%dx;
					rad = 1/( Math.tan(a) ) + 1%( Math.tan(a) );
					rad = 270 - rad;
				}else if(wall_4){
					dx = x - 399;
					dy = y - 399;
					a = dy/dx + dy%dx;
					rad = 1/( Math.tan(a) ) + 1%( Math.tan(a) );
				    rad = 270 + rad;
				}else {
					rad = 270 - my_rad;
				}
			}

			if(wall_3) {
				if(!wall_1 || !wall_2) {
					rad = 360 - my_rad;
				}
			}

			if(wall_4) {
				if(!wall_1 || !wall_2) {
					rad = 180 - my_rad;
				}
			}
			setTurnRight(rad);
			setAhead(200);
		}else {
			main_move();
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
	public class AliveRobot {
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
