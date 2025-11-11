//package groupb;

import java.util.*;

public class ring_algo {

    // 1. Process class now includes a 'participating' flag
    static class Process {
        int id;
        boolean isalive;
        boolean participating; // Tracks if it has sent its *own* ID
        Process next;

        Process(int id) {
            this.id = id;
            this.isalive = true;
            this.participating = false; // Not participating by default
        }
    }

    static List<Process> processes = new ArrayList<>();
    static int coordinatorId = -1; // -1 means no coordinator

    public static void main(String args[]) {
        // --- Setup ---
        // Create 5 processes (p1 to p5)
        for (int i = 1; i <= 5; i++) {
            processes.add(new Process(i));
        }

        // Link them in a ring (p1->p2... p5->p1)
        for (int i = 0; i < processes.size(); i++) {
            processes.get(i).next = processes.get((i + 1) % processes.size());
        }

        // Set initial coordinator (the highest ID)
        coordinatorId = 5;
        System.out.println("Initial coordinator is: p" + coordinatorId);

        // --- Simulate Failure ---
        Process coordProcess = getProcessById(coordinatorId);
        if (coordProcess != null) {
            coordProcess.isalive = false;
            System.out.println("Coordinator p" + coordinatorId + " failed.");
        }

        // --- Start Election ---
        // Any process (e.g., p2) that detects the failure can start
        startElection(2);
    }

    // Helper to get a process by its ID
    // (This is used by main to start, not by the algorithm itself)
    static Process getProcessById(int id) {
        for (Process p : processes) {
            if (p.id == id) {
                return p;
            }
        }
        return null;
    }

    // Helper for a process to find its *next alive* neighbor
    // This simulates skipping over failed nodes.
    static Process getNextAliveNeighbor(Process current) {
        Process nextP = current.next;
        
        // Loop around the ring until we find an alive process
        while (nextP != current) {
            if (nextP.isalive) {
                return nextP;
            }
            nextP = nextP.next;
        }
        // If we loop all the way back, no other process is alive
        return null; 
    }

    // Main function to start the election process
    static void startElection(int initiatorId) {
        Process initiator = getProcessById(initiatorId);
        if (initiator == null || !initiator.isalive) {
            System.out.println("p" + initiatorId + " cannot start election as it is down.");
            return;
        }

        System.out.println("\n--- Election Started by p" + initiatorId + " ---");
        
        // Reset participation status for all nodes
        for (Process p : processes) {
            p.participating = false;
        }

        // Mark initiator as participating and send message to its neighbor
        initiator.participating = true;
        
        Process nextNode = getNextAliveNeighbor(initiator);
        if (nextNode == null) {
            // Initiator is the only one alive
            System.out.println("p" + initiatorId + " is the only active node. It becomes coordinator.");
            coordinatorId = initiatorId;
            return;
        }

        // Start the message pass with the initiator's own ID
        passElectionMessage(nextNode, initiatorId);
    }

    // 2. This is the core distributed algorithm
    // Simulates a node *receiving* a message and acting on it
    static void passElectionMessage(Process receiver, int messageId) {
        
        System.out.println("p" + receiver.id + " received message with ID " + messageId);

        // --- Algorithm Logic ---
        if (messageId > receiver.id) {
            // Received ID is greater: Forward the message
            System.out.println("   -> ID " + messageId + " > " + receiver.id + ". Forwarding message.");
            
            Process nextNode = getNextAliveNeighbor(receiver);
            if (nextNode == null) return; // Should not happen
            
            passElectionMessage(nextNode, messageId); // Forward the *same* messageId

        } else if (messageId < receiver.id) {
            // Received ID is smaller: This node has a higher ID.
            if (!receiver.participating) {
                // Not yet participating, so replace message with own ID
                System.out.println("   -> ID " + messageId + " < " + receiver.id 
                                 + ". Replacing message with own ID " + receiver.id + " and forwarding.");
                
                receiver.participating = true; // Mark as participating
                
                Process nextNode = getNextAliveNeighbor(receiver);
                if (nextNode == null) return;

                passElectionMessage(nextNode, receiver.id); // Forward *own* ID
            } else {
                // Already participating (i.e., already sent its own ID).
                // This incoming message is "stale" and can be discarded.
                System.out.println("   -> ID " + messageId + " < " + receiver.id 
                                 + ". Already participating. Discarding message.");
            }
        } else {
            // messageId == receiver.id
            // The node's own ID has returned! It is the new coordinator.
            System.out.println("   -> ID " + messageId + " == " + receiver.id + ".");
            System.out.println("   -> My own ID has returned!");
            
            coordinatorId = receiver.id;
            System.out.println("--- Election Won by: p" + coordinatorId + " ---");
            
            // 3. Send a "COORDINATOR" message to announce the winner
            sendCoordinatorMessage(getNextAliveNeighbor(receiver), receiver);
        }
    }

    // 4. A second message-passing phase to announce the winner
    static void sendCoordinatorMessage(Process receiver, Process initiator) {
        // Stop when the message gets back to the new coordinator
        if (receiver == initiator) {
            System.out.println("p" + coordinatorId + " has informed all active nodes.");
            return;
        }

        System.out.println("p" + coordinatorId + " (Coordinator) sends announcement to p" + receiver.id);
        
        Process nextNode = getNextAliveNeighbor(receiver);
        if (nextNode != null) {
            sendCoordinatorMessage(nextNode, initiator);
        }
    }
}