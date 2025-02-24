package ca.mcgill.ecse211.project;

import static ca.mcgill.ecse211.project.LightLocalizer.relocalize;
import static ca.mcgill.ecse211.project.Resources.BASE_WIDTH;
import static ca.mcgill.ecse211.project.Resources.BLOCK_WIDTH;
import static ca.mcgill.ecse211.project.Resources.FORWARD_SPEED;
import static ca.mcgill.ecse211.project.Resources.ROTATE_SPEED;
import static ca.mcgill.ecse211.project.Resources.TILE_SIZE;
import static ca.mcgill.ecse211.project.Resources.WHEEL_RAD;
import static ca.mcgill.ecse211.project.Resources.blocks;
import static ca.mcgill.ecse211.project.Resources.gr;
import static ca.mcgill.ecse211.project.Resources.green;
import static ca.mcgill.ecse211.project.Resources.isRedTeam;
import static ca.mcgill.ecse211.project.Resources.island;
import static ca.mcgill.ecse211.project.Resources.leftMotor;
import static ca.mcgill.ecse211.project.Resources.obstacles;
import static ca.mcgill.ecse211.project.Resources.odometer;
import static ca.mcgill.ecse211.project.Resources.red;
import static ca.mcgill.ecse211.project.Resources.rightMotor;
import static ca.mcgill.ecse211.project.Resources.rr;
import static ca.mcgill.ecse211.project.Resources.szg;
import static ca.mcgill.ecse211.project.Resources.szr;
import static ca.mcgill.ecse211.project.Resources.tng;
import static ca.mcgill.ecse211.project.Resources.tnr;
import static ca.mcgill.ecse211.project.UltrasonicLocalizer.frontUSDistance;
import static ca.mcgill.ecse211.project.UltrasonicLocalizer.topUSDistance;
import static java.lang.Math.atan2;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static simlejos.ExecutionController.waitUntilNextStep;

import ca.mcgill.ecse211.playingfield.Block;
import ca.mcgill.ecse211.playingfield.Point;
import ca.mcgill.ecse211.playingfield.Region;
import java.util.ArrayList;

public class Navigation {
  // Coordinate variables from WiFi class
  public static double Red_LL_x = red.ll.x;
  public static double Red_LL_y = red.ll.y;
  public static double Red_UR_x = red.ur.x;
  public static double Red_UR_y = red.ur.y;
  public static double Green_LL_x = green.ll.x;
  public static double Green_LL_y = green.ll.y;
  public static double Green_UR_x = green.ur.x;
  public static double Green_UR_y = green.ur.y;
  public static double TNR_LL_x = tnr.ll.x;
  public static double TNR_LL_y = tnr.ll.y;
  public static double TNR_UR_x = tnr.ur.x;
  public static double TNR_UR_y = tnr.ur.y;
  public static double TNG_LL_x = tng.ll.x;
  public static double TNG_LL_y = tng.ll.y;
  public static double TNG_UR_x = tng.ur.x;
  public static double TNG_UR_y = tng.ur.y;
  public static double SZR_LL_x = szr.ll.x;
  public static double SZR_LL_y = szr.ll.y;
  public static double SZR_UR_x = szr.ur.x;
  public static double SZR_UR_y = szr.ur.y;
  public static double SZG_LL_x = szg.ll.x;
  public static double SZG_LL_y = szg.ll.y;
  public static double SZG_UR_x = szg.ur.x;
  public static double SZG_UR_y = szg.ur.y;
  public static double Island_LL_x = island.ll.x;
  public static double Island_LL_y = island.ll.y;
  public static double Island_UR_x = island.ur.x;
  public static double Island_UR_y = island.ur.y;
  public static double RR_LL_x = rr.left.x;
  public static double RR_LL_y = rr.left.y;
  public static double GR_LL_x = gr.left.x;
  public static double GR_LL_y = gr.left.y;
  // Team coordinate variables
  public static double lowerLeftSzgX = 0;
  public static double lowerLeftSzgY = 0;
  public static double upperRightSzgX = 0;
  public static double upperRightSzgY = 0;
  public static double lowerLeftX = 0;
  public static double lowerLeftY = 0;
  public static double upperRightX = 0;
  public static double upperRightY = 0;
  public static double lowerLeftTunnelX = 0;
  public static double lowerLeftTunnelY = 0;
  public static double upperRightTunnelX = 0;
  public static double upperRightTunnelY = 0;
  public static double lowerLeftRampX = 0;
  public static double lowerLeftRampY = 0;

  public static int startCorner;
  public static String closestSzg;

  // Map orientation booleans
  public static boolean upperonmap = false;
  public static boolean leftonmap = false;
  public static boolean horizontaltunnel = false;

  // Key points on map
  public static Point SZ_dest;
  public static Point startingCorner;
  public static double startingHeading;
  public static Point tunnelReturnPoint;
  public static double tunnelReturnHeading;
  public static double tunnelLength;

  public static ArrayList<SafePath> paths = new ArrayList<SafePath>();

  /** Do not instantiate this class. */
  private Navigation() {
  }

  /**
   * Pushes a block forward over a fixed distance and returns the average torque.
   * This methods assumes that we are 1/2 a tile behind the block (in the dir. we
   * want to push).
   * 
   * @param dist Positive distance the block should be pushed over (in m).
   * @return Average torque over the push period.
   */
  public static double pushFor(double dist) {
    setSpeed(FORWARD_SPEED);
    moveStraightFor(Resources.PUSH_TRAVEL_OFFSET); // have the bot touching the box
    System.out.println("pushing for " + dist);
    final int distTacho = convertDistance(dist);
    final int leftMotorTacho = leftMotor.getTachoCount();
    System.out.println(leftMotorTacho);

    moveStraightForReturn(dist * 3.28084);

    double avg = 0;
    int readings = 0;
    while (Math.abs((leftMotor.getTachoCount()) - leftMotorTacho) < Math.abs(distTacho)) {
      // while distance wasn't reached calculate average torque and wait.
      double trk = (leftMotor.getTorque() + rightMotor.getTorque()) / 2;
      avg = (avg * readings + trk) / ++readings;

      waitUntilNextStep();
    }

    leftMotor.stop();
    rightMotor.stop();
    odometer.printPosition();
    return avg;
  }

  /**
   * This function navigates to a given unknown object's position on the map and
   * checks if the object is a block or an obstacle. If it is a block, return true
   * and update the blocks list in Resources. Otherwise, return false and update
   * the obstacles list in Resources.
   *
   * @param pt       Target object's position (point).
   * @param curr     Current position (point).
   * @param curTheta Current heading of the robot.
   * @return Returns true if the object is a block. False if it is an obstacle.
   */
  public static boolean validateBlock(Point pt, Point curr, double curTheta) {
    // It is safe to assume that there is nothing in the way as the block readings
    // are based on the unknowns list of objects that were all read radially from
    // the search position, so only objects with direct LOS were added to that list.

    // Find proxy point and navigate to it
    double dist = distanceBetween(curr, pt);
    double destAngle = getDestinationAngle(curr, pt);
    turnBy(minimalAngle(curTheta, destAngle));
    // block width is 10cm, so "radius" would be 5 or more (trig). So 10cm would put
    // is within the 7cm margin.
    moveStraightFor(dist - (BLOCK_WIDTH / TILE_SIZE));
    double[] xyt = odometer.getXyt();
    Point current = new Point(xyt[0] / TILE_SIZE, xyt[1] / TILE_SIZE);
    turnTo(getDestinationAngle(current, pt));
    System.out.println("VALIDATING NOW");
    // Note: THE FOLLOWING PART ONLY WORKS IF WITHIN ~7CM (+-2cm) OF THE TARGET.
    int top = topUSDistance();
    int front = frontUSDistance();
    System.out.println("Top : " + top);
    System.out.println("Front x2 : " + front * 2);
    // THE FOLLOWING ONLY WORKS WITHIN 7+-2 CM OF THE OBJECT.
    if (blockOrObstacle()) {
      blocks.add(new Block(pt, -1)); // Add as block with placeholder torque
      System.out.println("Is a block");
      return true;
    } else { // Both are seeing roughly the same object (within 7cm) (tall wall)
      obstacles.add(pt); // Add as obstacle
      System.out.println("Not a block");
      return false;
    }
  }

  /**
   * Compares the front and top ultrasonic sensor values to determine if the
   * obstacle in front of the robot is a block or not. Note that this method only
   * works if the robot is within ~7cm (+-2cm) of the obstacle and facing it.
   *
   * @return True if the obstacle in front of the robot is a block, false if it is
   *         a wall.
   */
  public static boolean blockOrObstacle() {
    int top = topUSDistance();
    int front = frontUSDistance();
    System.out.println("Top : " + top);
    System.out.println("Front x2 : " + front * 2);
    // If top USS not seeing same-ish as front USS (block too short, wall is tall
    // enough to be seen)
    return (top > 2 * front && top > 10);
    // 10 is used as it is bigger than the possible value top could see within the
    // 7m radius of an obstacle.
  }

  /**
   * Travels to the given destination.
   * 
   * @param destination A point representing the destination.
   */
  public static void travelTo(Point destination) {
    double[] xyt = odometer.getXyt();
    Point currentLocation = new Point(xyt[0] / TILE_SIZE, xyt[1] / TILE_SIZE);
    double currentTheta = xyt[2];
    double destinationTheta = getDestinationAngle(currentLocation, destination);
    turnBy(minimalAngle(currentTheta, destinationTheta));
    moveStraightFor(distanceBetween(currentLocation, destination));
  }

  /**
   * Moves robot to Point(x,y) while scanning for obstacles, rereoutes where
   * necessary.
   *
   * @param destination given as point in TILE LENGTHS (e.g., (15, 0))
   */
  public static void travelToSafely(Point destination) {
    System.out.println("=> Proceeding to (" + String.format("%02.2f", destination.x) + ", "
        + String.format("%02.2f", destination.y) + ")...");

    // While not at destination, continuously try to navigate there
    boolean atDestination = false;
    while (!atDestination) {
      // Get current location in TILE LENGTHS from odometer; store in Point
      double[] xyt = odometer.getXyt();
      Point currentLocation = new Point(xyt[0] / TILE_SIZE, xyt[1] / TILE_SIZE);
      double currentTheta = xyt[2];

      // Check if at destination
      double distanceToDest = distanceBetween(currentLocation, destination);
      if (distanceToDest < 0.66) {
        // Close enough to destination; simply travelTo()
        System.out.println("=> Within one tile length. Completing travel...");

        travelTo(destination); // NOTE: travelTo assumes odometer is in meters

        System.out.println("=> Arrived safely at destination.");

        // Exit loop (set atDestination to true)
        atDestination = true;
      } else {
        // Not at destination; try to find path...
        // System.out.println("=> Finding best route...");

        // Turn towards destination point
        double destinationTheta = getDestinationAngle(currentLocation, destination);
        turnBy(minimalAngle(currentTheta, destinationTheta));

        // Check if path is clear (sweep tile in front and validate any object)
        double originalTheta = odometer.getXyt()[2]; // Save odometer heading
        UltrasonicLocalizer.searchObject();
        odometer.setTheta(originalTheta); 
        // Restore odometer heading (corrects reset in searchObject())

        // If an obstacle is present, rotate 90 degrees and try again
        boolean obstaclePresent = UltrasonicLocalizer.isObject;
        while (obstaclePresent) {
          System.out.println("Obstacle detected. Re-routing...");
          // Rotate 90 degrees
          turnBy(-90);

          // Check if path is clear (sweep tile in front and validate any object)
          originalTheta = odometer.getXyt()[2]; // Save heading
          UltrasonicLocalizer.searchObject();
          odometer.setTheta(originalTheta); // Restore heading (corrects reset from search)

          // If no obstacle is present, set obstaclePresent to false (exits loop!)
          obstaclePresent = UltrasonicLocalizer.isObject;
        }

        // Move forward one tile (or some other distance?) once there's no obstacle
        moveStraightFor(1.00);
      }
    }
  }

  /**
   * Moves robot to Point(x,y) while scanning for obstacles, rereoutes where
   * necessary.
   *
   * @param destination given as point in TILE LENGTHS (e.g., (15, 0))
   */
  public static void findPath(Point destination) {
    double angle = 0;
    double lenght = 0;
    Point init = null;
    System.out.println("=> Proceeding to (" + String.format("%02.2f", destination.x) + ", "
        + String.format("%02.2f", destination.y) + ")...");

    // While not at destination, continuously try to navigate there
    boolean atDestination = false;
    while (!atDestination) {
      // Get current location in TILE LENGTHS from odometer; store in Point
      double[] xyt = odometer.getXyt();
      Point currentLocation = new Point(xyt[0] / TILE_SIZE, xyt[1] / TILE_SIZE);
      double currentTheta = xyt[2];
      angle = currentTheta;

      // Check if at destination
      double distanceToDest = distanceBetween(currentLocation, destination);
      if (distanceToDest < 0.66) {
        // Close enough to destination; simply travelTo()
        System.out.println("=> Within one tile length. Completing travel...");

        travelTo(destination); // NOTE: travelTo assumes odometer is in meters

        System.out.println("=> Arrived safely at destination.");

        lenght += distanceToDest;
        angle = odometer.getXyt()[2];
        // calculate intial position
        double thetaPrime = 180 - angle;
        double thtaPrimePrime = 180 - 90 - thetaPrime;
        double radian = Math.toRadians(thtaPrimePrime);
        double yPoint = Math.sin(radian) * lenght;
        double xPoint = Math.cos(radian) * lenght;

        init = new Point((Math.abs(xPoint - (destination.x / 3.281) * 3.281)),
            (yPoint + (destination.y / 3.281) * 3.281));

        SafePath path = new SafePath(lenght, angle, init);
        paths.add(path);

        // Exit loop (set atDestination to true)
        atDestination = true;
      } else {
        // Not at destination; try to find path...
        // System.out.println("=> Finding best route...");

        // Turn towards destination point
        double destinationTheta = getDestinationAngle(currentLocation, destination);
        turnBy(minimalAngle(currentTheta, destinationTheta));

        // Check if path is clear (sweep tile in front and validate any object)
        double originalTheta = odometer.getXyt()[2]; // Save odometer heading
        UltrasonicLocalizer.searchObject();
        odometer.setTheta(originalTheta);
        // Restore odometer heading (corrects reset in searchObject())

        // If an obstacle is present, rotate 90 degrees and try again
        boolean obstaclePresent = UltrasonicLocalizer.isObject;
        boolean resetLenght = false;
        while (obstaclePresent) {
          resetLenght = true;
          System.out.println("Obstacle detected. Re-routing...");
          // Rotate 90 degrees
          turnBy(-90);

          // Check if path is clear (sweep tile in front and validate any object)
          originalTheta = odometer.getXyt()[2]; // Save heading
          UltrasonicLocalizer.searchObject();
          odometer.setTheta(originalTheta); // Restore heading (corrects reset from search)

          // If no obstacle is present, set obstaclePresent to false (exits loop!)
          obstaclePresent = UltrasonicLocalizer.isObject;
        }

        // Move forward one tile (or some other distance?) once there's no obstacle

        if (resetLenght) {
          lenght = 0;
          resetLenght = false;
        } else {
          lenght++;
        }
        moveStraightFor(1.00);
      }
    }
  }

  /**
   * Turns the robot with a minimal angle towards the given input angle in
   * degrees, no matter what its current orientation is. This method is different
   * from {@code turnBy()}.
   * 
   * @param angle Angle the robot should turn to in degrees.
   */
  public static void turnTo(double angle) {
    turnBy(minimalAngle(odometer.getXyt()[2], angle));
  }

  /**
   * Returns the angle that the robot should point towards to face the destination
   * in degrees.
   * 
   * @param current     Current position of the robot.
   * @param destination Destination of the robot.
   *
   * @return Destination angle.
   */
  public static double getDestinationAngle(Point current, Point destination) {
    return (toDegrees(atan2(destination.x - current.x, destination.y - current.y)) + 360) % 360;
  }

  /**
   * Returns the signed minimal angle from the initial angle to the destination
   * angle.
   * 
   * @param initialAngle Current angle of the robot in degrees.
   * @param destAngle    Target heading of the robot in degrees.
   *
   * @return Miniman angle difference in degrees.
   */
  public static double minimalAngle(double initialAngle, double destAngle) {
    double dtheta = destAngle - initialAngle;
    if (dtheta < -180) {
      dtheta += 360;
    } else if (dtheta > 180) {
      dtheta -= 360;
    }
    return dtheta;
  }

  /**
   * Returns the distance between the two points in tile lengths.
   * 
   * @param p1 First Point
   * @param p2 Second Point
   *
   * @return Distance between the two points in m.
   */
  public static double distanceBetween(Point p1, Point p2) {
    double dx = p2.x - p1.x;
    double dy = p2.y - p1.y;
    return sqrt(dx * dx + dy * dy);
  }

  /**
   * Moves the robot straight for the given distance.
   *
   * @param distance in feet (tile sizes), may be negative
   */
  public static void moveStraightFor(double distance) {
    setSpeed(FORWARD_SPEED);
    leftMotor.rotate(convertDistance(distance * TILE_SIZE), true);
    rightMotor.rotate(convertDistance(distance * TILE_SIZE), false);
  }

  /**
   * Moves the robot straight for the given distance.
   *
   * @param distance in meters, may be negative
   */
  public static void moveStraightForMeters(double distance) {
    setSpeed(FORWARD_SPEED);
    leftMotor.rotate(convertDistance(distance), true);
    rightMotor.rotate(convertDistance(distance), false);
  }

  /**
   * Moves the robot straight for the given distance. Returns immediately so as to
   * not stop the execution of subsequent code.
   *
   * @param distance in feet (tile sizes), may be negative
   */
  public static void moveStraightForReturn(double distance) {
    setSpeed(FORWARD_SPEED);
    leftMotor.rotate(convertDistance(distance * TILE_SIZE), true);
    rightMotor.rotate(convertDistance(distance * TILE_SIZE), true);
  }

  /**
   * Moves the robot straight for the given distance. Returns immediately so as to
   * not stop the execution of subsequent code.
   *
   * @param distance in meters, may be negative
   */
  public static void moveStraightForReturnMeters(double distance) {
    setSpeed(FORWARD_SPEED);
    leftMotor.rotate(convertDistance(distance), true);
    rightMotor.rotate(convertDistance(distance), true);
  }

  /** Moves the robot forward for an indeterminate distance. */
  public static void forward() {
    setSpeed(FORWARD_SPEED);
    leftMotor.forward();
    rightMotor.forward();
  }

  /** Moves the robot backward for an indeterminate distance. */
  public static void backward() {
    setSpeed(FORWARD_SPEED);
    leftMotor.backward();
    rightMotor.backward();
  }

  /**
   * Turns the robot by a specified angle. Note that this method is different from
   * {@code turnTo()}. For example, if the robot is facing 90 degrees, calling
   * {@code turnBy(90)} will make the robot turn to 180 degrees, but calling
   * {@code turnTo(90)} should do nothing (since the robot is already at 90
   * degrees).
   *
   * @param angle the angle by which to turn, in degrees
   */
  public static void turnBy(double angle) {
    setSpeed(ROTATE_SPEED);
    leftMotor.rotate(convertAngle(angle), true);
    rightMotor.rotate(-convertAngle(angle), false);
  }

  /** Rotates motors clockwise. */
  public static void clockwise() {
    setSpeed(ROTATE_SPEED);
    leftMotor.forward();
    rightMotor.backward();
  }

  /** Rotates motors counterclockwise. */
  public static void counterclockwise() {
    setSpeed(ROTATE_SPEED);
    leftMotor.backward();
    rightMotor.forward();
  }

  /** Stops both motors. This also resets the motor speeds to zero. */
  public static void stopMotors() {
    leftMotor.stop();
    rightMotor.stop();
  }

  /**
   * Converts input distance to the total rotation of each wheel needed to cover
   * that distance.
   *
   * @param distance the input distance in meters
   * @return the wheel rotations necessary to cover the distance in degrees
   */
  public static int convertDistance(double distance) {
    return (int) toDegrees(distance / WHEEL_RAD);
  }

  /**
   * Converts input angle to total rotation of each wheel needed to rotate robot
   * by that angle.
   *
   * @param angle the input angle in degrees
   * @return the wheel rotations (in degrees) necessary to rotate the robot by the
   *         angle
   */
  public static int convertAngle(double angle) {
    return convertDistance(toRadians((BASE_WIDTH / 2) * angle));
  }

  /**
   * Sets the speed of both motors to the same values.
   *
   * @param speed the speed in degrees per second
   */
  public static void setSpeed(int speed) {
    setSpeeds(speed, speed);
  }

  /**
   * Sets the speed of both motors to different values.
   *
   * @param leftSpeed  the speed of the left motor in degrees per second
   * @param rightSpeed the speed of the right motor in degrees per second
   */
  public static void setSpeeds(int leftSpeed, int rightSpeed) {
    leftMotor.setSpeed(leftSpeed);
    rightMotor.setSpeed(rightSpeed);
  }

  /**
   * Sets the acceleration of both motors.
   *
   * @param acceleration the acceleration in degrees per second squared
   */
  public static void setAcceleration(int acceleration) {
    leftMotor.setAcceleration(acceleration);
    rightMotor.setAcceleration(acceleration);
  }

  /**
   * Sets up the points.
   */
  public static void setPoints() {
    if (isRedTeam) {
      // Set RED TEAM coordinates
      lowerLeftSzgX = SZR_LL_x;
      lowerLeftSzgY = SZR_LL_y;
      upperRightSzgX = SZR_UR_x;
      upperRightSzgY = SZR_UR_y;
      lowerLeftX = Red_LL_x;
      lowerLeftY = Red_LL_y;
      upperRightX = Red_UR_x;
      upperRightY = Red_UR_y;
      lowerLeftTunnelX = TNR_LL_x;
      lowerLeftTunnelY = TNR_LL_y;
      upperRightTunnelX = TNR_UR_x;
      upperRightTunnelY = TNR_UR_y;
      lowerLeftRampX = RR_LL_x;
      lowerLeftRampY = RR_LL_y;

      startCorner = Resources.redCorner;
    } else {
      // Set GREEN TEAM coordinates
      lowerLeftSzgX = SZG_LL_x;
      lowerLeftSzgY = SZG_LL_y;
      upperRightSzgX = SZG_UR_x;
      upperRightSzgY = SZG_UR_y;
      lowerLeftX = Green_LL_x;
      lowerLeftY = Green_LL_y;
      upperRightX = Green_UR_x;
      upperRightY = Green_UR_y;
      lowerLeftTunnelX = TNG_LL_x;
      lowerLeftTunnelY = TNG_LL_y;
      upperRightTunnelX = TNG_UR_x;
      upperRightTunnelY = TNG_UR_y;
      lowerLeftRampX = GR_LL_x;
      lowerLeftRampY = GR_LL_y;
      startCorner = Resources.greenCorner;
    }
  }

  /**
   * Finds the point before the tunnel.
   *
   * @return Returns the point before the tunnel.
   */
  public static Point getPointBeforetunnel() {
    // Get team points first
    setPoints();

    // Calculate point before tunnel
    double angle = 0;
    double x = 0;
    double y = 0;
    Point dest = new Point(0, 0);

    // Determine starting coordinates and tunnel orientation/entrance from
    // startCorner
    if (startCorner == 3) {
      // In the UPPER-LEFT corner
      x = 1;
      y = 8;
      angle = 90;
      upperonmap = true;
      leftonmap = true;

      // Check whether tunnel is horizontal or vertical
      if (Island_LL_x > upperRightX) {
        // Tunnel is horizontal (search zone is to right of starting zone)
        dest.x = lowerLeftTunnelX - 1;
        dest.y = (upperRightTunnelY + lowerLeftTunnelY) / 2;
        horizontaltunnel = true;
      } else if (Island_UR_y < lowerLeftY) {
        // Tunnel is vertical (search zone is below starting zone)
        dest.y = upperRightTunnelY + 1;
        dest.x = (lowerLeftTunnelX + upperRightTunnelX) / 2;
      }

    } else if (startCorner == 2) {
      // In the UPPER-RIGHT corner
      x = 14;
      y = 8;
      angle = -90;
      upperonmap = true;
      leftonmap = false;

      // Check whether tunnel is horizontal or vertical
      if (Island_UR_x < lowerLeftX) {
        // Tunnel is horizontal (search zone is to left of starting zone)
        dest.x = upperRightTunnelX + 1;
        dest.y = (upperRightTunnelY + lowerLeftTunnelY) / 2;
        horizontaltunnel = true;
      } else if (Island_UR_y < lowerLeftY) {
        // Tunnel is vertical (search zone is below starting zone)
        dest.y = upperRightTunnelY + 1;
        dest.x = (lowerLeftTunnelX + upperRightTunnelX) / 2;
      }

    } else if (startCorner == 0) {
      // In the LOWER-LEFT corner
      x = 1;
      y = 1;
      angle = 90;
      upperonmap = false;
      leftonmap = true;

      // Check whether tunnel is horizontal or vertical
      if (Island_LL_x > upperRightX) {
        // Tunnel is horizontal (search zone is to right of starting zone)
        dest.x = lowerLeftTunnelX - 1;
        dest.y = (upperRightTunnelY + lowerLeftTunnelY) / 2;
        horizontaltunnel = true;
      } else if (Island_LL_y > upperRightY) {
        // Tunnel is vertical (search zone is above starting zone)
        dest.y = lowerLeftTunnelY - 1;
        dest.x = (lowerLeftTunnelX + upperRightTunnelX) / 2;
      }

    } else if (startCorner == 1) {
      // In the LOWER-RIGHT corner
      x = 14;
      y = 1;
      angle = -90;
      upperonmap = false;
      leftonmap = false;

      // System.out.println("=> Starting from Corner " + startCorner + " (" + x + ", "
      // + y + ")...");

      // Check whether tunnel is horizontal or vertical
      if (Island_UR_x < lowerLeftX) {
        // Tunnel is horizontal (search zone is to the left of starting zone)
        // System.out.println("=> Tunnel runs horizontally.");
        dest.x = upperRightTunnelX + 1;
        dest.y = (upperRightTunnelY + lowerLeftTunnelY) / 2;
        horizontaltunnel = true;
      } else if (Island_LL_y > upperRightY) {
        // Tunnel is vertical (search zone is above starting zone)
        // System.out.println("=> Tunnel runs vertically.");
        dest.y = lowerLeftTunnelY - 1;
        dest.x = (lowerLeftTunnelX + upperRightTunnelX) / 2;
      }

    }

    // Save location of starting corner
    startingCorner = new Point(x, y);
    startingHeading = angle;

    // Set odometer to starting corner (in meters!)
    odometer.setX(x * TILE_SIZE);
    odometer.setY(y * TILE_SIZE);
    odometer.setTheta(angle);
    return dest;

  }

  /**
   * Goes through the tunnel plus 0.4 tile lengths ahead.
   */
  public static void goThroughTunnel() {
    // Calculate point before tunnel, then travel to it
    Point destination = getPointBeforetunnel();
    System.out.println("[STATUS] Travelling to tunnel...");
    travelToSafely(destination);
    System.out.println("[STATUS] Arrived at tunnel. Passing through to island...");

    // Determine orientation of tunnel based on position of starting zone
    if (upperonmap == true) {
      // Check UPPER-X cases

      if (leftonmap) {
        // UPPER-LEFT
        if (horizontaltunnel) {
          turnTo(90);
          tunnelLength = upperRightTunnelX - lowerLeftTunnelX + 1.4;
          moveStraightFor(tunnelLength);
        } else {
          turnTo(180);
          tunnelLength = upperRightTunnelY - lowerLeftTunnelY + 1.4;
          moveStraightFor(tunnelLength);
        }
      } else {
        // UPPER-RIGHT
        if (horizontaltunnel) {
          turnTo(-90);
          tunnelLength = upperRightTunnelX - lowerLeftTunnelX + 1.4;
          moveStraightFor(tunnelLength);
        } else {
          turnTo(-180);
          tunnelLength = upperRightTunnelY - lowerLeftTunnelY + 1.4;
          moveStraightFor(tunnelLength);
        }
      }
    } else {
      // Check LOWER-X cases

      if (leftonmap) {
        // LOWER-LEFT
        if (horizontaltunnel) {
          turnTo(90);
          tunnelLength = upperRightTunnelX - lowerLeftTunnelX + 1.4;
          moveStraightFor(tunnelLength);
        } else {
          turnTo(0);
          tunnelLength = upperRightTunnelY - lowerLeftTunnelY + 1.4;
          moveStraightFor(tunnelLength);
        }
      } else {
        // LOWER-RIGHT
        if (horizontaltunnel) {
          turnTo(-90);
          tunnelLength = upperRightTunnelX - lowerLeftTunnelX + 1.4;
          moveStraightFor(tunnelLength);
        } else {
          turnTo(0);
          tunnelLength = upperRightTunnelY - lowerLeftTunnelY + 1.4;
          moveStraightFor(tunnelLength);
        }
      }
    }

    // Save current location and opposite heading after tunnel (For returning)
    tunnelReturnPoint = new Point(odometer.getXyt()[0] 
        / TILE_SIZE, odometer.getXyt()[1] / TILE_SIZE);
    tunnelReturnHeading = odometer.getXyt()[2] + 180.0;
  }

  /**
   * Checks whether the robot is in the search zone.
   *
   * @return true if in search zone.
   */
  public static boolean inSearchZone() {
    if (isRedTeam) {
      // If RED TEAM, use RED SEARCH ZONE coordinates
      if (odometer.getXyt()[0] / TILE_SIZE > SZR_LL_x && odometer.getXyt()[0] / TILE_SIZE < SZR_UR_x
          && odometer.getXyt()[1] / TILE_SIZE > SZR_LL_y 
          && odometer.getXyt()[1] / TILE_SIZE < SZR_UR_y) {
        return true;
      } else {
        return false;
      }
    } else {
      // If GREEN TEAM, use GREEN SEARCH ZONE coordinates
      if (odometer.getXyt()[0] / TILE_SIZE > SZG_LL_x && odometer.getXyt()[0] / TILE_SIZE < SZG_UR_x
          && odometer.getXyt()[1] / TILE_SIZE > SZG_LL_y 
          && odometer.getXyt()[1] / TILE_SIZE < SZG_UR_y) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Moves robot to searchZone after having moved through tunnel. Assumes that
   * robot will be in center of tile just after tunnel
   */
  public static void goToSearchZone() {
    System.out.println("[STATUS] Navigating to search zone...");

    // Get current location from odometer; set as Point
    double[] xyt = odometer.getXyt();
    Point currentLocation = new Point(xyt[0], xyt[1]);
    // System.out.println(currentLocation.x + ", " + currentLocation.y);

    // Get search zone corner coordinates
    Point LL_SZ = new Point(lowerLeftSzgX, lowerLeftSzgY);
    Point LR_SZ = new Point(upperRightSzgX, lowerLeftSzgY);
    Point UL_SZ = new Point(lowerLeftSzgX, upperRightSzgY);
    Point UR_SZ = new Point(upperRightSzgX, upperRightSzgY);
    Point[] szCorners = new Point[] { LL_SZ, LR_SZ, UL_SZ, UR_SZ };

    // Find closest corner of search zone and set as destination "dest"
    SZ_dest = LL_SZ;
    double searchZoneStartAngle = 0;
    for (int i = 0; i < szCorners.length; i++) {
      Point currCorner = szCorners[i];
      if (distanceBetween(currentLocation, currCorner) 
          <= distanceBetween(currentLocation, SZ_dest)) {
        // Closest point found; set appropriate offset
        if (i == 0) {
          // Lower-left corner
          System.out.println("=> Lower-left corner of search zone is closest.");
          SZ_dest = new Point(currCorner.x + 0.5, currCorner.y + 0.5);
          searchZoneStartAngle = 90;
          closestSzg = "LL"; // this is for the search method
        } else if (i == 1) {
          // Lower-right corner
          System.out.println("=> Lower-right corner of search zone is closest.");
          SZ_dest = new Point(currCorner.x - 0.5, currCorner.y + 0.5);
          searchZoneStartAngle = -90;
          closestSzg = "LR"; // this is for the search method

        } else if (i == 2) {
          // Upper-left corner
          System.out.println("=> Upper-left corner of search zone is closest.");
          SZ_dest = new Point(currCorner.x + 0.5, currCorner.y - 0.5);
          searchZoneStartAngle = 90;
          closestSzg = "UL"; // this is for the search method

        } else {
          // Upper-right corner
          System.out.println("=> Upper-right corner of search zone is closest.");
          SZ_dest = new Point(currCorner.x - 0.5, currCorner.y - 0.5);
          searchZoneStartAngle = -90;
          closestSzg = "UR"; // this is for the search method

        }
      }
    }

    // Travel to nearest corner in search zone
    travelToSafely(SZ_dest);

    // Turn to search zone starting angle
    turnTo(searchZoneStartAngle);
  }

  /**
   * Moves robot back to tunnel and then back to starting corner.
   * 
   */
  public static void returnToStart() {
    System.out.println("[STATUS] Tasks complete. Returning to start...");

    // Return to starting corner of search zone
    //travelTo(SZ_dest);

    // Return to point before tunnel and correct heading; traverse tunnel
    System.out.println("=> Returning through tunnel...");
    travelTo(tunnelReturnPoint);
    turnTo(tunnelReturnHeading);
    moveStraightFor(tunnelLength);

    // Return to starting corner
    System.out.println("=> Returning to starting corner...");
    travelToSafely(startingCorner);
    relocalize();
    turnTo(startingHeading);

    System.out.println("[STATUS] ヽ༼◉ل͜◉༽ﾉ All done ヽ༼◉ل͜◉༽ﾉ");
  }

  /**
   * Sorts an ArrayList of Points based on their distance to a given point.
   *
   * @param list ArrayList of points
   * @param curr Point around which to sort.
   * @return Returns a new arraylist containing the same points but sorted by
   *         distance from the curr point.
   */
  public static ArrayList<Point> sortDistances(ArrayList<Point> list, Point curr) {
    ArrayList<Point> result = new ArrayList<Point>();

    for (int i = 0; i < list.size(); i++) { // for all individual points in list
      int index = 0;
      Point offsetPt = new Point(list.get(i).x + 0.5, list.get(i).y + 0.5);
      while (true) { // loop until point position is found
        if (result.size() <= index) {
          // if there are no more elements to compare to
          result.add(index, list.get(i));
          break;
        } else {
          Point resultOffset = new Point(result.get(index).x + 0.5, result.get(index).y + 0.5);
          if (distanceBetween(curr, offsetPt) > distanceBetween(curr, resultOffset)) {
            // current index object belongs later in the list
            index++;
          } else {
            // current index object belongs before the current list element
            result.add(index, list.get(i));
            break;
          }
        }
      }
    }
    return result;
  }

  /**
   * A method that takes as input a search zone and generates all valid tiles
   * inside the zone (valid meaning that there are no known obstacles, ie the ramp
   * and bin).
   *
   * @param szn Search zone (region)
   * @return ArrayList of points contianing all lower left corners of valid tiles.
   */
  public static ArrayList<Point> szTiles(Region szn) {
    // Tiles are identified by their lower left corner.
    ArrayList<Point> tiles = new ArrayList<Point>();
    // Determine which tiles the ramp and bin occupy (2 tiles only)
    Point lre = isRedTeam ? rr.left : gr.left;
    Point rre = isRedTeam ? rr.right : gr.right;
    Point rt1 = new Point(100, 100);
    Point rt2 = new Point(100, 100);
    if (lre.x < rre.x) { // upward facing bin
      rt1 = new Point(lre.x, lre.y);
      rt2 = new Point(lre.x, lre.y + 1);
    } else if (lre.x > rre.x) { // downward facing bin
      rt1 = new Point(rre.x, rre.y - 1);
      rt2 = new Point(rre.x, rre.y - 2);
    } else if (lre.y < rre.y) { // left facing bin
      rt1 = new Point(lre.x - 1, lre.y);
      rt2 = new Point(lre.x - 2, lre.y);
    } else { // (lre.y > rre.y) // right facing bin
      rt1 = new Point(rre.x, rre.y);
      rt2 = new Point(rre.x + 1, rre.y);
    }
    // ramp tiles stored in rt1 and rt2
    // iterate over all tiles in the region
    for (int y = (int) szn.ll.y; y < (int) szn.ur.y; y++) {
      for (int x = (int) szn.ll.x; x < (int) szn.ur.x; x++) {
        boolean sameP1 = x == rt1.x && y == rt1.y;
        boolean sameP2 = x == rt2.x && y == rt2.y;
        if (!sameP1 && !sameP2) { // Is neither ramp or bin point
          tiles.add(new Point(x, y));
        }
      }
    }
    return tiles;
  }

}
