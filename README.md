# Particle Simulator

This is a simple particle simulator implemented in Java. It uses Swing for the graphical user interface and Java's built-in concurrency utilities for the simulation logic.

# How to run the program
- Open the `jar` file in the `src` directory, two windows should pop-up (canvas + inputs)
 
- Click on the dropdown to change input type `Single Particle`, `Constant Velocity + Varying Angle`, `Constant Start Points + Varying Angle`, `Constant Start Points + Varying Velocity`, `Add Wall`
 
  - As for all types, simply input the required fields (X, Y, angle, velocity, etc.)
  - Click on the `Add` button after completing the inputs
 
- Clear all of the `Particles` by clicking on the `Clear Particles` button

- Clear all of the `Walls` by clicking on the `Clear Walls` button

- Close either of the windows to stop the program

# For Developers

## Prerequisites

- Java Development Kit (JDK) 8 or later

## Compiling the Program

1. Open a terminal/command prompt.
2. Navigate to the directory containing the `src` folder.
3. Compile the Java files using the `javac` command:

```sh
javac src/*.java
```

This will compile all the Java files in the `src` directory and create corresponding `.class` files.

## Running the Program

After compiling the program, you can run it using the `java` command. The main class of the program is `ParticleSimulator`.

```sh
java src/ParticleSimulator
```

This will start the particle simulator. You can interact with the program using the graphical user interface.

## Creating a JAR File

1. After compiling the program, navigate to the `src` directory:

```sh
cd src
```

2. Create a manifest file named `Manifest.txt` with the following content:

```txt
Main-Class: ParticleSimulator

```
Note the extra newline at the end of the file.

This tells the JAR file which class to run when it's executed.

3. Create the JAR file using the `jar` command:

```sh
jar cvfm ParticleSimulator.jar Manifest.txt *.class
```

This will create a JAR file named `ParticleSimulator.jar` that includes all the `.class` files in the current directory and uses the manifest file you created.

## Running the JAR File

After creating the JAR file, you can run it using the `java` command:

```sh
java -jar ParticleSimulator.jar
```

This will start the particle simulator just like before, but now you're running it from a single JAR file instead of multiple `.class` files.

## Features

- Add particles with fixed velocity and angle.
- Add particles with a fixed start point and varying angle.
- Add particles with a fixed start point and varying velocity.
- Add walls for the particles to bounce off.
- Clear all particles or walls.