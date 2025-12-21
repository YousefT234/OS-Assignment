package com.scheduler;

import org.json.*;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.*;

class Process {
    private final String name;
    private final int arrivalTime;
    private final int initialBurstTime;
    private int burstTime;
    private int priority;
    private int quantum;
    private List<Integer> quantumHistory;

    private int waitingTime;
    private int turnaroundTime;
    private int completionTime;
    private int startTime;
    private int lastExecutionTime;
    private int last;
    private int inputOrder;

    public Process(String name, int arrivalTime, int burstTime, int priority, int quantum, int inputOrder) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.initialBurstTime = burstTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.quantum = quantum;
        this.quantumHistory = new ArrayList<>();
        quantumHistory.add(quantum);
        this.startTime = -1;
        this.lastExecutionTime = 0;
        this.last = arrivalTime;
        this.inputOrder = inputOrder;
    }

    public Process(String name, int arrivalTime, int burstTime, int priority, int quantum) {
        this(name, arrivalTime, burstTime, priority, quantum, 0); // default inputOrder = 0
    }

    public String getName() {
        return name;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getInitialBurstTime() {
        return initialBurstTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getPriority() {
        return priority;
    }

    public int getQuantum() {
        return quantum;
    }

    public List<Integer> getQuantumHistory() {
        return quantumHistory;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public int getTurnaroundTime() {
        return turnaroundTime;
    }

    public int getCompletionTime() {
        return completionTime;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void execute(int duration) {
        if (startTime == -1) {
            startTime = lastExecutionTime;
        }
        burstTime -= duration;
        lastExecutionTime += duration;
    }

    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public void setCompletionTime(int completionTime) {
        this.completionTime = completionTime;
    }

    public void setLastExecutionTime(int lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
        quantumHistory.add(quantum);
    }

    public boolean isCompleted() {
        return burstTime <= 0;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getLast() {
        return last;
    }

    public void setLast(int last) {
        this.last = last;
    }

    public int getInputOrder() {
        return inputOrder;
    }
}

abstract class AbstractScheduler {

    protected List<Process> initialProcesses;

    public abstract void schedule();

    public List<String> executionOrder = new ArrayList<>();

    protected final int contextSwitchTime;

    public AbstractScheduler(List<Process> processes, int csTime) {
        this.initialProcesses = deepCopyProcesses(processes);
        this.contextSwitchTime = csTime;
    }

    protected int performContextSwitch(int currentTime) {
        return currentTime + contextSwitchTime;
    }

    private List<Process> deepCopyProcesses(List<Process> original) {
        List<Process> copy = new java.util.ArrayList<>();
        for (Process p : original) {
            copy.add(new Process(
                    p.getName(),
                    p.getArrivalTime(),
                    p.getInitialBurstTime(),
                    p.getPriority(),
                    p.getQuantum()));
        }
        return copy;
    }

    public static void calculateMetrics(Process p, int completionTime) {
        p.setCompletionTime(completionTime);
        int turnaroundTime = completionTime - p.getArrivalTime();
        p.setTurnaroundTime(turnaroundTime);
        int waitingTime = turnaroundTime - p.getInitialBurstTime();
        p.setWaitingTime(waitingTime);
    }

    public static double calculateAverageWaitingTime(List<Process> processes) {
        double totalWaitingTime = 0;
        for (Process p : processes) {
            totalWaitingTime += p.getWaitingTime();
        }
        if (processes.isEmpty())
            return 0.0;

        double average = totalWaitingTime / processes.size();
        return Math.round(average * 100.0) / 100.0;
    }

    public static double calculateAverageTurnaroundTime(List<Process> processes) {
        double totalTurnaroundTime = 0;
        for (Process p : processes) {
            totalTurnaroundTime += p.getTurnaroundTime();
        }
        if (processes.isEmpty())
            return 0.0;

        double average = totalTurnaroundTime / processes.size();
        return Math.round(average * 100.0) / 100.0;
    }
}

class SJFScheduler extends AbstractScheduler {

    public SJFScheduler(List<Process> processes, int contextSwitchTime) {
        super(processes, contextSwitchTime);
    }

    @Override
    public void schedule() {
        int currentTime = 0;
        int completedProcessesCount = 0;
        int totalProcesses = initialProcesses.size();

        PriorityQueue<Process> readyQueue = new PriorityQueue<>(
                Comparator.comparingInt(Process::getBurstTime)
                        .thenComparingInt(Process::getArrivalTime));

        List<Process> activeProcesses = new ArrayList<>(initialProcesses);
        activeProcesses.sort(Comparator.comparingInt(Process::getArrivalTime));

        Process currentProcess = null;
        Process lastProcess = null;

        while (completedProcessesCount < totalProcesses) {

            while (!activeProcesses.isEmpty() && activeProcesses.get(0).getArrivalTime() <= currentTime) {
                readyQueue.add(activeProcesses.remove(0));
            }

            if (!readyQueue.isEmpty()) {
                Process bestCandidate = readyQueue.peek();

                if (bestCandidate != lastProcess && lastProcess != null) {
                    currentTime += contextSwitchTime;
                }

                currentProcess = bestCandidate;

                if (currentProcess.getStartTime() == -1) {
                    currentProcess.setLastExecutionTime(currentTime);
                }

                currentProcess.execute(1);
                currentTime++;

                if (currentProcess != lastProcess)
                    executionOrder.add(currentProcess.getName());

                if (currentProcess.isCompleted()) {
                    completedProcessesCount++;
                    readyQueue.poll();

                    calculateMetrics(currentProcess, currentTime);
                }

                lastProcess = currentProcess;

            } else {
                currentTime++;
            }
        }
        printResults();
    }

    private void printResults() {
        System.out.println("--- Shortest Job First Scheduling ---");
        System.out.println("Execution Order: " + executionOrder);
        System.out.println("Process\t\tWaiting Time\t\tTurnaround Time");

        List<Process> sorted = new ArrayList<>(initialProcesses);
        sorted.sort(Comparator.comparing(Process::getName));

        for (Process p : sorted) {
            System.out.println(p.getName() + "\t\t    " +
                    p.getWaitingTime() + "\t\t\t       " +
                    p.getTurnaroundTime() + "\t\t");
        }

        System.out.println("Average Waiting Time: " +
                String.format("%.2f", calculateAverageWaitingTime(initialProcesses)));

        System.out.println("Average Turnaround Time: " +
                String.format("%.2f", calculateAverageTurnaroundTime(initialProcesses)));

    }
}

class RRScheduler extends AbstractScheduler {

    private final int timeQuantum;

    public RRScheduler(List<Process> processes, int contextSwitchTime, int timeQuantum) {
        super(processes, contextSwitchTime);
        this.timeQuantum = timeQuantum;
    }

    @Override
    public void schedule() {

        List<Process> processes = initialProcesses;
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        Queue<Process> readyQueue = new LinkedList<>();

        int currentTime = 0;
        int index = 0;
        Process currentProcess = null;
        Process lastProcess = null;

        while (true) {

            while (index < processes.size() &&
                    processes.get(index).getArrivalTime() <= currentTime) {
                readyQueue.add(processes.get(index));
                index++;
            }

            if (currentProcess == null) {
                if (readyQueue.isEmpty()) {
                    if (index == processes.size())
                        break;
                    currentTime = processes.get(index).getArrivalTime();
                    continue;
                }

                currentProcess = readyQueue.poll();

                if (lastProcess != null) {
                    currentTime = performContextSwitch(currentTime);
                }

                if (currentProcess.getStartTime() == -1)
                    currentProcess.setStartTime(currentTime);
            }

            executionOrder.add(currentProcess.getName());

            int execTime = Math.min(timeQuantum, currentProcess.getBurstTime());
            currentProcess.execute(execTime);
            currentTime += execTime;

            while (index < processes.size() &&
                    processes.get(index).getArrivalTime() <= currentTime) {
                readyQueue.add(processes.get(index));
                index++;
            }

            if (currentProcess.isCompleted()) {
                calculateMetrics(currentProcess, currentTime);
                lastProcess = currentProcess;
                currentProcess = null;
            } else {
                readyQueue.add(currentProcess);
                lastProcess = currentProcess;
                currentProcess = null;
            }
        }

        printResults();
    }

    private void printResults() {
        System.out.println("--- Round Robin Scheduling ---");
        System.out.println("Execution Order: " + executionOrder);
        System.out.println("Process\t\tWaiting Time\t\tTurnaround Time");

        List<Process> sorted = new ArrayList<>(initialProcesses);
        sorted.sort(Comparator.comparing(Process::getName));

        for (Process p : sorted) {
            System.out.println(p.getName() + "\t\t    " +
                    p.getWaitingTime() + "\t\t\t       " +
                    p.getTurnaroundTime() + "\t\t");
        }

        System.out.println("Average Waiting Time: " +
                String.format("%.2f", calculateAverageWaitingTime(initialProcesses)));

        System.out.println("Average Turnaround Time: " +
                String.format("%.2f", calculateAverageTurnaroundTime(initialProcesses)));

    }
}

class PreemptivePriorityScheduler extends AbstractScheduler {

    private final int agingInterval;

    public PreemptivePriorityScheduler(List<Process> processes, int csTime, int agingInterval) {
        super(processes, csTime);
        this.agingInterval = agingInterval;
    }

    @Override
    public void schedule() {
        int time = 0;
        Process current = null;
        String lastRecorded = null;

        while (true) {
            boolean allDone = true;
            for (Process p : initialProcesses) {
                if (p.getBurstTime() > 0) {
                    allDone = false;
                    break;
                }
            }
            if (allDone)
                break;

            for (Process p : initialProcesses) {
                if (p.getArrivalTime() <= time && p.getBurstTime() > 0) {
                    if (time > p.getLast() && (time - p.getLast()) % agingInterval == 0) {
                        p.setPriority(Math.max(1, p.getPriority() - 1));
                        p.setLast(time);
                    }
                }
            }

            Process next = null;
            for (Process p : initialProcesses) {
                if (p.getBurstTime() > 0 && p.getArrivalTime() <= time) {
                    if (next == null ||
                            p.getPriority() < next.getPriority() ||
                            (p.getPriority() == next.getPriority() && p.getArrivalTime() < next.getArrivalTime()) ||
                            (p.getPriority() == next.getPriority() && p.getArrivalTime() == next.getArrivalTime()
                                    && p.getInputOrder() < next.getInputOrder())) {
                        next = p;
                    }
                }
            }

            if (next == null) {
                time++;
                continue;
            }

            if (current != next) {
                if (current != null) {
                    time += contextSwitchTime;
                }
                current = next;
                if (!current.getName().equals(lastRecorded)) {
                    executionOrder.add(current.getName());
                    lastRecorded = current.getName();
                }
                continue;
            }

            // Execute one unit of time
            current.execute(1);
            time++;
            current.setLast(time);

            if (current.isCompleted()) {
                calculateMetrics(current, time);
            }
        }

    }
}

class AGScheduler extends AbstractScheduler {
    public AGScheduler(List<Process> processes, int csTime) {
        super(processes, csTime);
    }

    @Override
    public void schedule() {
        List<Process> processes = initialProcesses;
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));
        Queue<Process> readyQueue = new LinkedList<>();
        int currentTime = 0;
        int nextProcessIndex = 0;
        Process currentProcess = null;
        while (true) {
            // Add arrived processes
            while (nextProcessIndex < processes.size()
                    && processes.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                Process p = processes.get(nextProcessIndex++);
                readyQueue.add(p);
            }
            if (readyQueue.isEmpty() && currentProcess == null) {
                if (nextProcessIndex == processes.size())
                    break;
                currentTime = processes.get(nextProcessIndex).getArrivalTime();
                continue;
            }

            if (currentProcess == null) {
                currentProcess = readyQueue.poll();

            }
            if (currentProcess.getStartTime() == -1) {
                currentProcess.setStartTime(currentTime);
            }
            // Phase 1
            executionOrder.add(currentProcess.getName());
            int time = (currentProcess.getQuantum() + 3) / 4;
            time = Math.min(time, currentProcess.getBurstTime());
            currentProcess.execute(time);
            int rem = currentProcess.getQuantum() - time;
            currentTime += time;

            if (currentProcess.getBurstTime() == 0) {
                calculateMetrics(currentProcess, currentTime);
                currentProcess.setQuantum(0); // scenario iv
                currentProcess = null;
                continue;
            }
            while (nextProcessIndex < processes.size()
                    && processes.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                Process p = processes.get(nextProcessIndex++);
                readyQueue.add(p);
            }

            // go to second phase (Priority)
            Process nextProcess = null;
            if (!readyQueue.isEmpty())
                nextProcess = Collections.min(readyQueue, Comparator.comparingInt(Process::getPriority));
            if (nextProcess == null || currentProcess.getPriority() <= nextProcess.getPriority())
                nextProcess = currentProcess;
            if (nextProcess != currentProcess) {
                currentProcess.setQuantum(currentProcess.getQuantum() + (rem + 1) / 2); // scenario ii
                readyQueue.remove(nextProcess);
                readyQueue.add(currentProcess);
                currentProcess = nextProcess;
                continue;
            }
            time = (currentProcess.getQuantum() + 3) / 4;
            time = Math.min(time, currentProcess.getBurstTime());
            currentProcess.execute(time);
            rem -= time;
            currentTime += time;

            if (currentProcess.getBurstTime() == 0) {
                calculateMetrics(currentProcess, currentTime);
                currentProcess.setQuantum(0); // scenario iv
                currentProcess = null;
                continue;
            }
            while (nextProcessIndex < processes.size()
                    && processes.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                Process p = processes.get(nextProcessIndex++);
                readyQueue.add(p);
            }

            // Third phase (Shortest remaining time)
            nextProcess = null;
            if (!readyQueue.isEmpty())
                nextProcess = Collections.min(readyQueue, Comparator.comparingInt(Process::getBurstTime));

            if (nextProcess == null || currentProcess.getBurstTime() <= nextProcess.getBurstTime())
                nextProcess = currentProcess;
            if (nextProcess != currentProcess) {
                currentProcess.setQuantum(currentProcess.getQuantum() + rem); // scenario iii
                readyQueue.remove(nextProcess);
                readyQueue.add(currentProcess);
                currentProcess = nextProcess;
                continue;
            }
            time = rem;
            time = Math.min(time, currentProcess.getBurstTime());
            currentProcess.execute(time);
            currentTime += time;

            if (currentProcess.getBurstTime() == 0) {
                calculateMetrics(currentProcess, currentTime);
                currentProcess.setQuantum(0); // scenario iv
                currentProcess = null;
                continue;
            }
            currentProcess.setQuantum(currentProcess.getQuantum() + 2); // scenario i
            readyQueue.add(currentProcess);
            currentProcess = null;

        }
        printResults();
    }

    private void printResults() {
        System.out.println("--- AG Scheduling ---");
        System.out.println("Execution Order: " + executionOrder);
        System.out.println("Process\t\tWaiting Time\t\tTurnaround Time\t\tquantum history");

        List<Process> sorted = new ArrayList<>(initialProcesses);
        sorted.sort(Comparator.comparing(Process::getName));

        for (Process p : sorted) {
            System.out.println(p.getName() + "\t\t    " +
                    p.getWaitingTime() + "\t\t\t       " +
                    p.getTurnaroundTime() + "\t\t" +
                    p.getQuantumHistory());
        }

        System.out.println("Average Waiting Time: " +
                String.format("%.2f", calculateAverageWaitingTime(initialProcesses)));

        System.out.println("Average Turnaround Time: " +
                String.format("%.2f", calculateAverageTurnaroundTime(initialProcesses)));

    }
}

class SchedulerTest {
    @Test
    void testSchedulingAlgorithms() throws Exception {
        File folder = new File("test_cases/Other_Schedulers");
        File folder2 = new File("test_cases/AG");

        File[] Other_Schedulers = folder.listFiles((dir, name) -> name.endsWith(".json"));
        File[] AG = folder2.listFiles((dir, name) -> name.endsWith(".json"));

        for (File file : Other_Schedulers) {
            JSONObject testCase = loadTestCaseFromFile(file);
            System.out.println("Running test: " + file.getName());
            runTestCase(testCase);
        }

        // for (File file : AG) {
        // JSONObject testCase = loadTestCaseFromFile(file);
        // System.out.println("Running test: " + file.getName());
        // runTestCase_AG(testCase);
        // }
    }

    private JSONObject loadTestCaseFromFile(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            byte[] bytes = is.readAllBytes();
            String json = new String(bytes, StandardCharsets.UTF_8);

            return new JSONObject(json);
        }
    }

    private List<Process> parseProcesses(JSONArray arr) {
        List<Process> list = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject p = arr.getJSONObject(i);
            list.add(new Process(
                    p.getString("name"),
                    p.getInt("arrival"),
                    p.getInt("burst"),
                    p.getInt("priority"),
                    0,
                    i));
        }
        return list;
    }

    private List<Process> parseProcesses_AG(JSONArray arr) {
        List<Process> list = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject p = arr.getJSONObject(i);
            list.add(new Process(
                    p.getString("name"),
                    p.getInt("arrival"),
                    p.getInt("burst"),
                    p.getInt("priority"),
                    p.getInt("quantum"),
                    i));
        }
        return list;
    }

    private void runTestCase(JSONObject tc) {
        String testName = tc.getString("name");
        JSONObject input = tc.getJSONObject("input");
        int contextSwitch = input.getInt("contextSwitch");
        int rrQuantum = input.getInt("rrQuantum");
        int agingInterval = input.getInt("agingInterval");

        JSONArray processesArr = input.getJSONArray("processes");
        JSONObject expected = tc.getJSONObject("expectedOutput");
        for (int i = 0; i < 3; i++) {
            AbstractScheduler scheduler = null;
            String algoName = "";
            String tempTestName = testName;
            // parse fresh processes for each algorithm to avoid mutated state
            List<Process> processes = parseProcesses(processesArr);
            if (i == 0) {
                algoName = "SJF";
                scheduler = new SJFScheduler(processes, contextSwitch);
                scheduler.schedule();
                tempTestName += " - SJF";
            } else if (i == 1) {
                algoName = "RR";
                scheduler = new RRScheduler(processes, contextSwitch, rrQuantum);
                scheduler.schedule();
                tempTestName += " - RR";
            } else if (i == 2) {
                algoName = "Priority";
                scheduler = new PreemptivePriorityScheduler(processes, contextSwitch, agingInterval);
                scheduler.schedule();
                tempTestName += " - Priority";
            }

            JSONObject expectedAlgoData = expected.getJSONObject(algoName);
            JSONArray expectedOrder = expectedAlgoData.getJSONArray("executionOrder");
            List<String> executionOrder = scheduler.executionOrder;
            assertEquals(expectedOrder.length(),
                    executionOrder.size(),
                    tempTestName + " execution order size mismatch");
            for (int j = 0; j < expectedOrder.length(); j++) {
                assertEquals(expectedOrder.getString(j),
                        executionOrder.get(j),
                        tempTestName + " execution order mismatch at step " + j);
            }
            JSONArray expectedResults = expectedAlgoData.getJSONArray("processResults");
            List<Process> actualProcesses = scheduler.initialProcesses;
            for (int j = 0; j < expectedResults.length(); j++) {
                JSONObject exp = expectedResults.getJSONObject(j);
                Process act = actualProcesses.get(j);
                assertEquals(exp.getString("name"), act.getName());
                assertEquals(exp.getDouble("waitingTime"), (double) act.getWaitingTime(), 1.1,
                        tempTestName + " waiting time mismatch for process " + act.getName());
                assertEquals(exp.getDouble("turnaroundTime"), (double) act.getTurnaroundTime(), 1.1,
                        tempTestName + " turnaround time mismatch for process " + act.getName());
            }

            assertEquals(expectedAlgoData.getDouble("averageWaitingTime"),
                    AbstractScheduler.calculateAverageWaitingTime(actualProcesses),
                    1.1,
                    tempTestName + " avg waiting time mismatch");

            assertEquals(expectedAlgoData.getDouble("averageTurnaroundTime"),
                    AbstractScheduler.calculateAverageTurnaroundTime(scheduler.initialProcesses),
                    1.1,
                    tempTestName + " avg turnaround time mismatch");

            System.out.println(tempTestName + " passed\n");
        }
    }

    private void runTestCase_AG(JSONObject tc) {
        JSONObject input = tc.getJSONObject("input");
        List<Process> processes = parseProcesses_AG(input.getJSONArray("processes"));
        JSONObject expected = tc.getJSONObject("expectedOutput");
        AGScheduler scheduler = new AGScheduler(processes, 0);
        scheduler.schedule();
        JSONArray expectedOrder = expected.getJSONArray("executionOrder");
        List<String> executionOrder = scheduler.executionOrder;
        assertEquals(expectedOrder.length(),
                executionOrder.size(),
                "execution order size mismatch");
        for (int j = 0; j < expectedOrder.length(); j++) {
            assertEquals(expectedOrder.getString(j),
                    executionOrder.get(j),
                    "execution order mismatch at step " + j);
        }
        JSONArray expectedResults = expected.getJSONArray("processResults");
        List<Process> actualProcesses = scheduler.initialProcesses;
        for (int j = 0; j < expectedResults.length(); j++) {
            JSONObject exp = expectedResults.getJSONObject(j);
            Process act = actualProcesses.get(j);
            assertEquals(exp.getString("name"), act.getName());
            assertEquals(exp.getDouble("waitingTime"), (double) act.getWaitingTime(), 1.1,
                    "waiting time mismatch for process " + act.getName());
            assertEquals(exp.getDouble("turnaroundTime"), (double) act.getTurnaroundTime(), 1.1,
                    "turnaround time mismatch for process " + act.getName());
            List<Integer> quantumHistory = act.getQuantumHistory();
            JSONArray expectedQuantumHistory = exp.getJSONArray("quantumHistory");
            assertEquals(expectedQuantumHistory.length(), quantumHistory.size(),
                    "quantum history size mismatch for process " + act.getName());
            for (int k = 0; k < expectedQuantumHistory.length(); k++) {
                assertEquals(expectedQuantumHistory.getInt(k), quantumHistory.get(k),
                        "quantum history mismatch for process " + act.getName() + " at index " + k);
            }
        }

        assertEquals(expected.getDouble("averageWaitingTime"),
                AbstractScheduler.calculateAverageWaitingTime(actualProcesses),
                1.1,
                "avg waiting time mismatch");

        assertEquals(expected.getDouble("averageTurnaroundTime"),
                AbstractScheduler.calculateAverageTurnaroundTime(scheduler.initialProcesses),
                1.1,
                "avg turnaround time mismatch");

        System.out.println("Test passed\n");

    }

}

public class Main {
    public static void main(String[] args) throws Exception {
        SchedulerTest sc = new SchedulerTest();
        sc.testSchedulingAlgorithms();
    }
}
