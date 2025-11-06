//GL guys
/*output should show when a car:

• Arrives
• Enters the queue
• Is taken by a Pump
• Starts service (Acquiring the Pumps)
• Finishes service (Releasing the Pumps)
 */

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class semaphore {
    protected int value;
    protected semaphore(int initial) { value = initial; }

    public synchronized void P() throws InterruptedException {
        value--;
        if (value < 0) wait();
    }

    public synchronized void V() {
        value++;
        if (value <= 0) notify();
    }

    public synchronized int getValue() { return value; }
}

class Car extends Thread {
    String carNumber;
    ServiceStation serviceStation;

    public Car(String carNumber, ServiceStation serviceStation) {
        this.carNumber = carNumber;
        this.serviceStation = serviceStation;
    }

    private void print(String msg) {
        System.out.println(msg);
    }

    @Override
    public void run() {
        print(carNumber + " Arrived");

        synchronized (serviceStation.mutex) {
            boolean mustWait = serviceStation.queue.size() >= serviceStation.bufferSize ||
                               serviceStation.bays.getValue() <= 0;
            if (mustWait) {
                print(carNumber + " arrived and waiting");
            }
        }

        try {
            serviceStation.empty.P();
            serviceStation.mutex.P();
            serviceStation.queue.offer(carNumber);
            print(carNumber + " Enters the queue, Queue size: " + serviceStation.queue.size());
            serviceStation.mutex.V();
            serviceStation.full.V();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class Pump extends Thread {
    private int pumpId;
    private ServiceStation station;

    public Pump(int pumpId, ServiceStation station) {
        this.pumpId = pumpId;
        this.station = station;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                station.full.P();
                station.mutex.P();
                String car = station.queue.poll();
                if (car != null) {
                    System.out.println("Pump " + pumpId + ": took " + car);
                    station.mutex.V();
                    station.empty.V();

                    station.bays.P();
                    System.out.println("Pump " + pumpId + ": " + car + " starts service");
                    Thread.sleep(200 + (long)(Math.random() * 300));
                    System.out.println("Pump " + pumpId + ": " + car + " finishes service");
                    System.out.println("Pump " + pumpId + " is now free");
                    station.bays.V();

                    station.processed.incrementAndGet();
                } else {
                    station.mutex.V();
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}

public class ServiceStation {
    protected int bufferSize;
    private int numPumps;
    private int numCars;
    protected Queue<String> queue;
    protected semaphore mutex;
    protected semaphore empty;
    protected semaphore full;
    protected semaphore bays;
    private List<Pump> pumpThreads;
    private List<Car> carThreads;
    protected AtomicInteger processed;

    public ServiceStation(int bufferSize, int numPumps, int numCars) {
        this.bufferSize = Math.max(1, Math.min(10, bufferSize));
        this.numPumps = Math.max(1, numPumps);
        this.numCars = numCars;

        this.queue = new LinkedList<>();
        this.mutex = new semaphore(1);
        this.empty = new semaphore(this.bufferSize);
        this.full = new semaphore(0);
        this.bays = new semaphore(this.numPumps);
        this.pumpThreads = new ArrayList<>();
        this.carThreads = new ArrayList<>();
        this.processed = new AtomicInteger(0);
    }

    public void startSimulation() throws InterruptedException {
        for (int i = 1; i <= numPumps; i++) {
            Pump p = new Pump(i, this);
            pumpThreads.add(p);
            p.start();
        }

        for (int i = 1; i <= numCars; i++) {
            Car c = new Car("C" + i, this);
            carThreads.add(c);
            c.start();
            Thread.sleep(80); 
        }

        for (Car c : carThreads) c.join();

        while (processed.get() < numCars) {
            Thread.sleep(100);
        }

        for (Pump p : pumpThreads) p.interrupt();
        for (Pump p : pumpThreads) p.join();

        System.out.println("Simulation ended.");
    }

    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter buffer size (1-10): ");
        int buffer = sc.nextInt();
        System.out.print("Enter number of pumps: ");
        int pumps = sc.nextInt();
        System.out.print("Enter number of cars: ");
        int cars = sc.nextInt();
        sc.close();

        new ServiceStation(buffer, pumps, cars).startSimulation();
    }
}
