//*GL guys
/*output should show when a car:

• Arrives
• Enters the queue
• Is taken by a Pump
• Starts service (Acquiring the Pumps)
• Finishes service (Releasing the Pumps)
 */

import java.util.*;

class semaphore {

    protected int value = 0 ;

    protected semaphore() { value = 0 ; }

    protected semaphore(int initial) { value = initial ; }

    public synchronized void P() {
	  
        value-- ;
        if (value < 0)
        try { wait() ; } catch(  InterruptedException e ) { }
    }

    public synchronized void V() {
        value++ ; if (value <= 0) notify() ;
    }
}

class Car extends Thread {
}

class Pump extends Thread {
}

public class ServiceStation {
    //initializes the shared resources
    private int bufferSize;
    private int numPumps;
    private int numCars;
    protected Queue<String> queue;
    protected semaphore mutex;
    protected semaphore empty;
    protected semaphore full;
    protected semaphore bays;
    private List<Pump> pumpThreads;
    private List<Car> carThreads;

    public ServiceStation(int bufferSize, int numPumps, int numCars) {
        this.bufferSize = bufferSize;
        this.numPumps = numPumps;
        this.numCars = numCars;
        this.queue = new LinkedList<>();
        this.mutex = new semaphore(1);
        this.empty = new semaphore(bufferSize);
        this.full = new semaphore(0);
        this.bays = new semaphore(numPumps);
        this.pumpThreads = new ArrayList<>();
        this.carThreads = new ArrayList<>();
    }

    public void startSimulation() throws InterruptedException {
        for (int i = 0; i < numPumps; i++) {
            Pump pump = new Pump(i, this);
            pumpThreads.add(pump);
            pump.start();
        }
        for (int i = 0; i < numCars; i++) {
            Car car = new Car("C" + i, this);
            carThreads.add(car);
            car.start();
        }
        for (Thread t : carThreads) {
            t.join();
        }
        for (int i = 0; i < numPumps; i++) {
            full.V();
        }
        for (Thread t : pumpThreads) {
            t.join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Simulation started.");
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter buffer size (queue capacity): ");
        int bufferSize = scanner.nextInt();

        System.out.print("Enter number of pumps: ");
        int numPumps = scanner.nextInt();

        System.out.print("Enter number of cars: ");
        int numCars = scanner.nextInt();

        scanner.close();
        ServiceStation station = new ServiceStation(bufferSize, numPumps, numCars);
        station.startSimulation();

        System.out.println("Simulation ended.");
    }

}
