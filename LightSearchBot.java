import swiftbot.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;


public class LightSearchBot {
	//declaring variables to use later
	//these variables are used in different method and in the main class also
		public static double leftAverageIntensity; 
		public static double centerAverageIntensity;
		public static double rightAverageIntensity;
    	public static boolean running = true;
    	public static long startTime;
    	public static final int OBJECT_DISTANCE_THRESHOLD = 50; // Threshold for object detection in cm
    	//Array List for the movements in the swiftbot ..
    	public static ArrayList<String> movements = new ArrayList<>();
    	//ALL of these variables will help in the Log display and execution..
    	public static double totalDistance = 0;
    	public static int lightDetectionCount = 0;
    	public static int objectDetectionCount = 0;
    	public static double highestLeftIntensity = 0;
    	public static double highestCenterIntensity = 0;
    	public static double highestRightIntensity = 0;
    	
    	private static int processImage(BufferedImage image) {// Method to process the captured image and decide the direction
            // Convert the image to a pixel matrix
        		System.out.println("Processing the Image");
        		int width = image.getWidth();
            	int height = image.getHeight();
            	int[][] pixelMatrix = new int[height][width];
            	for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixelMatrix[y][x] = image.getRGB(x, y);// to get the RGB value for the image
                  
                }
            }  	
            	System.out.println("The Image has been converted into a pixel matrix\n");
            

            	// Divide the pixel matrix into left, center, and right sections
            	int columnWidth = width / 3;
            	int[][] leftColumn = new int[height][columnWidth];
            	int[][] centerColumn = new int[height][columnWidth];
            	int[][] rightColumn = new int[height][width - 2 * columnWidth];
            	// Copy pixels to respective columns
            	// Columns are divided with help of height and width
            	for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (x < columnWidth)
                        leftColumn[y][x] = pixelMatrix[y][x];
                    else if (x >= columnWidth && x < 2 * columnWidth)
                        centerColumn[y][x - columnWidth] = pixelMatrix[y][x];
                    else
                        rightColumn[y][x - 2 * columnWidth] = pixelMatrix[y][x];
                   
                }
            } 
            	System.out.println("The Image has been divided into three columns\n\n");

            	// Calculate average intensities for each section
            	leftAverageIntensity = calculateAverageIntensity(leftColumn);
            	centerAverageIntensity = calculateAverageIntensity(centerColumn);
            	rightAverageIntensity = calculateAverageIntensity(rightColumn);
            	System.out.println("The average light intensity of the Left section is \n"+   leftAverageIntensity+"\n");
            	System.out.println("The average light intensity of the Center section is\n "+ centerAverageIntensity+"\n");
            	System.out.println("The average light intensity of the Right section is \n"+  rightAverageIntensity+"\n");
            
            

            	// Update highest average intensities if necessary
            	// math class can also be used up here but I used this to make it informative so that one can know what will we get 
            	//the MAX method can be used to make it easier but I preferred this
            	if (leftAverageIntensity > highestLeftIntensity) {
                highestLeftIntensity = leftAverageIntensity;
            	}
            	if (centerAverageIntensity > highestCenterIntensity) {
                highestCenterIntensity = centerAverageIntensity;
            	}
            	if (rightAverageIntensity > highestRightIntensity) {
                highestRightIntensity = rightAverageIntensity;
            	}

            	// Decide the direction with the highest average intensity
            	if (leftAverageIntensity > centerAverageIntensity && leftAverageIntensity > rightAverageIntensity) {
                return 0; // Left direction
            	} else if (centerAverageIntensity > leftAverageIntensity && centerAverageIntensity > rightAverageIntensity) {
                return 1; // Center direction
            	} else {
                return 2; // Right direction
            	}
        	}
     // Method to calculate the average intensity of a pixel matrix
        private static double calculateAverageIntensity(int[][] pixelMatrix) {
            int sum = 0;
            int count = 0;
            for (int[] row : pixelMatrix) {
                for (int pixel : row) {
                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = pixel & 0xff;
                    int intensity = (red + green + blue) / 3;
                    sum += intensity;
                    count++;
                    
                }
            }
            return (double) sum / count;
        }
        	// Method to move the bot based on the decided direction
        private static void MoveRobot(SwiftBotAPI charlie, int direction) {
        		String movement = "";
        		switch (direction) {
                	case 0: // Left
                		movement = "Left";
                		int[] colourtolight= {0,0,255};
                		charlie.fillUnderlights(colourtolight);
                		lightDetectionCount++;
                		System.out.println("Turning Left\n");
                		
                		charlie.move(0, 100, 712); // Turn left
                		charlie.move(100,100,1000);	//continue movement in left direction
                		charlie.disableUnderlights();
                    break;
                	case 1: // Center
                		movement = "Straight";
                		int[] colourtolight1= {0,0,255};
                		lightDetectionCount++;
                		System.out.println("Moving Straight");
                		charlie.fillUnderlights(colourtolight1);
                		charlie.move(100, 100, 1000); // Move forward
                		charlie.move(100, 100, 1000); // Move Forward
                		charlie.disableUnderlights();

                    break;
                	case 2: // Right
                		movement = "Right";
                		int[] colourtolight2= {0,0,255};
                		charlie.fillUnderlights(colourtolight2);
                		lightDetectionCount++;
                		System.out.println("Turning Right\n");
                		charlie.move(100, 0, 712); // Turn right
                		charlie.move(100,100, 1000);// Continue in the right Direction
                		charlie.disableUnderlights();

                    break;
                	default:
                		System.out.println("No valid direction found");
                		lightDetectionCount++;
                    break;
            }
        			movements.add(movement);
        }

        // Method to check for objects and adjust behavior accordingly
        private static boolean checkObjects(SwiftBotAPI charlie) {
        			double distanceToObject = charlie.useUltrasound();
        			//To check if the object is with in 50 cm ..
        			if (distanceToObject <= OBJECT_DISTANCE_THRESHOLD) {
        				System.out.println("Object detected at " + distanceToObject + " cm ahead.\n");
        				objectDetectionCount++;

                // If so then Notify the user about the object
        				int[] colorToLightdown = { 255, 0, 0 }; // Red color
        				charlie.fillUnderlights(colorToLightdown);
        				System.out.println("Please remove the object in front of the Swiftbot.\n");

                // Check after 10 seconds if the object has been removed
                try {
                    Thread.sleep(10000); // Wait for 10 seconds
                } catch (InterruptedException e) {
                   // error may occur when we press X button in the running program but majority of times no error was spotted 
                }

                	double distanceAfterWait = charlie.useUltrasound();
                	if (distanceAfterWait > OBJECT_DISTANCE_THRESHOLD) {
                    // Object removed, continue the search
                		System.out.println("Object removed. Continuing the search.\n");
                    charlie.disableUnderlights();
                    return false; // No object detected
                } 
                	else {
                    // Object not removed, terminate the program
                    System.out.println("Object not removed. Terminating the program.\n");
                    charlie.disableUnderlights();
                    objectDetectionCount++;
                    saveExecutionLogToFile();
                    Termination();
                    System.exit(1);
                   // running = false;
                   // return true; // Object detected
                }
            }
            return false; // No object detected
        }

        // Method to display the log of execution
        private static void displayExecutionLog() {//method to display log 
        		long endTime = System.currentTimeMillis();
        		long duration = (endTime - startTime) / 1000; // Duration in seconds

        			System.out.println("\nExecution Log:-");
        			System.out.println("Highest average intensity in left section: " + highestLeftIntensity);
        			System.out.println("Highest average intensity in center section: " + highestCenterIntensity);
        			System.out.println("Highest average intensity in right section: " + highestRightIntensity);
        			System.out.println("Number of times object detected: " +  objectDetectionCount);
        			System.out.println("Number of times Light detected " + lightDetectionCount );
        			System.out.println("Movements:");
            for (String movement : movements) {
            		System.out.println(movement);
            }
            for (int i = 0; i < movements.size(); i++) {
                	String movement = movements.get(i);
                	if (movement.equals("Straight")) {
                    totalDistance += 10; // Assuming 15 cm for straight movement
                } 
                	else if (movement.equals("Left") || movement.equals("Right")) {
                    totalDistance += 10; // Assuming 20 cm for left/right movement
                }
            }
            		System.out.println("Total distance travelled: " + totalDistance + " cm\n");
            		System.out.println("Duration of execution: " + duration + " seconds\n");
        }
        
        private static void saveExecutionLogToFile() {//method to save log to text file
        			long endTime = System.currentTimeMillis();
        			long duration = (endTime - startTime) / 1000; // Duration in seconds

        		// Create the file path
        			String filePath = "/home/pi/SLSP/log.txt";

        		// Save the log to the file
        	try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                // Write the log data to the file
                	writer.write("Execution Log:\n");
                	writer.write("Highest average intensity in left section: " + highestLeftIntensity + "\n");
                	writer.write("Highest average intensity in center section: " + highestCenterIntensity + "\n");
                	writer.write("Highest average intensity in right section: " + highestRightIntensity + "\n");
                	writer.write("Number of times Object detected: " + objectDetectionCount + "\n");
                	writer.write("Number of times Light detected " + lightDetectionCount + "\n");
                	writer.write("Movements:\n");
                for (String movement : movements) {
                    writer.write(movement + "\n");
                }
                for (String movement : movements) {
                    if (movement.equals("Straight")) {
                        totalDistance += 10; // Assuming 15 cm for straight movement
                    }
                    else if (movement.equals("Left") || movement.equals("Right")) {
                        totalDistance += 10; // Assuming 20 cm for left/right movement
                    }
                }
                	writer.write("Total distance travelled: " + totalDistance + " cm\n");
                	writer.write("Duration of execution: " + duration + " seconds\n");

                	System.out.println("Execution log saved to :\n " + filePath);
            } 
        	catch (IOException e) {
                System.out.println("Error saving execution log: " + e.getMessage());
                e.printStackTrace();
            }
        }
    		

        private static void noLightFound(SwiftBotAPI charlie) {
                Random rand = new Random();
                int randomDirection = rand.nextInt(2);
                try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}// Generate a random number (0 or 1) for direction change
                if (randomDirection == 0) {
                	int [] blue= {0,255,0};
                	charlie.fillUnderlights(blue);
                    System.out.println("Light intensity threshold not met. \nChanging direction 90 degrees to the left."
                    		+ "\n left turn");
                    charlie.move(0, 100, 712);     // Turn left
                    charlie.move(100, 100, 1000); // Continue movement in the new direction
                    charlie.disableUnderlights(); 
                } else {
                	int [] blue= {0,255,0};
                	charlie.fillUnderlights(blue);
                    System.out.println("Light intensity threshold not met. \nChanging direction 90 degrees to the right.\n"
                    		+ "right turn");
                    charlie.move(100, 0, 712);      // Turn right
                    charlie.move(100, 100, 1000);  // Continue movement in the new direction
                    charlie.disableUnderlights(); 
                    
                }
                
            }
    		
    	private static void displaymenu() {//method to display welcome messages
    		

    		    
    		 System.out.println(" _   _      _ _         __        __         _     _ _ ");
    	        System.out.println("| | | | ___| | | ___    \\ \\      / /__  _ __| | __| | |");
    	        System.out.println("| |_| |/ _ \\ | |/ _ \\    \\ \\ /\\ / / _ \\| '__| |/ _` | |");
    	        System.out.println("|  _  |  __/ | | (_) |    \\ V  V / (_) | |  | | (_| |_|");
    	        System.out.println("|_| |_|\\___|_|_|\\___( )    \\_/\\_/ \\___/|_|  |_|\\__,_(_)");
    	        System.out.println("                    |/                                 ");
    		

    		System.out.println("************************************************************************************");
            System.out.println("         SwiftBot Light Search Program       ");
            System.out.println("************************************************************************************");
            System.out.println("1. Press the Button A to Start The Program Program");
            System.out.println("2. Press the Button  X to Exit The Program");
            System.out.println("3. Press the button B for more information of the Program and how is it going work ");
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    	
    }
    	
    	public static void Termination() {
    		System.out.println("~~~~~~~~~~~~~THE PROGRAM IS NOW TERMINATING~~~~~~~~~~~~~~\n");
            System.out.println("TTTTT  H   H   A   N   N  K  K    Y   Y   O O   U   U");
            System.out.println("  T    H   H  A A  NN  N  K K      Y Y   O   O  U   U");
            System.out.println("  T    HHHHH AAAAA N N N  KK        Y    O   O  U   U");
            System.out.println("  T    H   H A   A N  NN  K K       Y    O   O  U   U");
            System.out.println("  T    H   H A   A N   N  K  K      Y     O O    UUU ");
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
        }
    	//This is the main class for the project ....
    	public static void main(String[] args) {
    		
    		displaymenu();
    		SwiftBotAPI charlie = new SwiftBotAPI();
    		startTime = System.currentTimeMillis(); // Start time of execution
    		charlie.enableButton(Button.B, ()-> {
    	   	 	System.out.println("This Is my light search program\n"
    	   	 			+ "`````SUMMARY:\r\n"
    	   	 			+ " --What the Bot Does:\r\n"
    	   	 			+ "\r\n"
    	   	 			+ "*The bot moves around by itself, reacting to light and avoiding obstacles.\r\n"
    	   	 			+ "*It takes pictures and figures out where to go based on the brightness in different parts of the picture.\r\n"
    	   	 			+ "--How It Decides Where to Go:\r\n"
    	   	 			+ "\r\n"
    	   	 			+ "*It looks at the pictures it takes and calculates how bright each part is.\r\n"
    	   	 			+ "*Then, it chooses the direction with the most light to move towards.\r\n"
    	   	 			+ "--What Happens When It Finds an Obstacle:\r\n"
    	   	 			+ "\r\n"
    	   	 			+ "*If the bot sees something in its way, it stops and asks for help to move the obstacle.\r\n"
    	   	 			+ "*Once the obstacle is gone, it continues its search.\r\n"
    	   	 			+ "--Keeping Track of What It Does:\r\n"
    	   	 			+ "\r\n"
    	   	 			+ "*The bot remembers how many times it found light and which way it moved.\r\n"
    	   	 			+ "*After it finishes, it can show a report of what it did, like how far it traveled and how long it took.\r\n"
    	   	 			+ "*Saving Information for Later:\r\n"
    	   	 			+ "\r\n"
    	   	 			+ "--It saves all this information in a file so it can be looked at again later.\r\n"
    	   	 			+ "*If there's a problem saving, it knows how to handle it.\r\n"
    	   	 			+ "--Conclusion:\r\n"
    	   	 			+ "*The LightSearchBot explores its surroundings, reacts to light and obstacles, \n"
    	   	 			+ "and keeps track of its actions. It's like a curious explorer that learns from its experiences and can share what it discovers through its journey.\n"
    	   	 			+ " The program makes sure it doesn't forget anything important by saving everything in a special file for safekeeping.\n\n");
    	    		System.out.println("Press 'A' to start the program \n"
    	    					+ "Press 'X' to terminate the program \n");charlie.disableButton(Button.B);});
    	    
    	    
        
         
       
         
         charlie.enableButton(Button.A, () -> {
        	//button to start the program execution
           
            System.out.println("the image will be saved in the path /home/pi/SLSP/The_Main_Image.jpg\"");
            while (running) {
                // Take an image of the surroundings
                BufferedImage image = charlie.takeStill(ImageSize.SQUARE_1080x1080);

                // Save the captured image to the specified directory
                String imagePath = "/home/pi/SLSP/The_Main_Image.jpg";
                
                System.out.println("The Image has been clicked \n");
                try {
                    File outputImageFile = new File(imagePath);
                    ImageIO.write(image, "jpg", outputImageFile);
                   // System.out.println("Image saved to: " + imagePath);
                } catch (IOException e) {
                    System.out.println("Error saving the image: " + e.getMessage());
                    e.printStackTrace();
                }
               
                // Process the captured image to decide the direction
                int direction = processImage(image);
                if(leftAverageIntensity<15 && rightAverageIntensity<15 && centerAverageIntensity<15  )
                { int [] color = {220,222,45};
                charlie.fillUnderlights(color);
                charlie.disableUnderlights();
                	System.out.println("No Light Source Detected \n"
                			+ "Changing Direction\n");
                	noLightFound(charlie);
               
                }
                
                	 
                    int [] firstcolor = {0,0,255};
                    charlie.fillUnderlights(firstcolor);
                charlie.move(50, 50, 500);
                charlie.disableUnderlights();
                // Move the bot in the decided direction
                MoveRobot(charlie, direction);

                // Check for objects and adjust behavior if necessary
                if (checkObjects(charlie)) {
                    // Object detected, stop execution
                    break;
                }

                // Check if the user wants to terminate the program
                if (!running) {
                    break;
                }
            }

            // After execution, ask the user if they want to display the log
           
            
        });

        charlie.enableButton(Button.X, () -> {
        	
            running = false; //set bool to false so execution can stop
            charlie.disableUnderlights();
            System.out.println("Do you want to display the log of execution? (Press 'Y' for Yes, 'X' for No\n");
            charlie.enableButton(Button.Y, ()->{ //Y to display log, write to file and exit
            	 displayExecutionLog();
            	 saveExecutionLogToFile();
            	 
            	 Termination();
            	 System.exit(0);
            });
            charlie.disableButton(Button.X); //disable first so another functionality can be added
            charlie.enableButton(Button.X, ()->{ //X again to write log to file and exit
            	saveExecutionLogToFile();
            	 
            	 Termination();
            	 		
            	System.exit(0);
            });
        });
    }}

    


