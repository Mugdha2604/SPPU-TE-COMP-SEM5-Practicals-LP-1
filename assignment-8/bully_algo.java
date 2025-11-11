import java.util.ArrayList;
import java.util.List;

// Class representing a node in the distributed system
class Node {
    private int nodeId;
    private boolean isCoordinator;

    public Node(int nodeId) {
        this.nodeId = nodeId;
        this.isCoordinator = false;
    }

    public int getNodeId() {
        return nodeId;
    }

    public boolean isCoordinator() {
        return isCoordinator;
    }

    public void setCoordinator(boolean coordinator) {
        isCoordinator = coordinator;
    }

    // Method to initiate an election
    public void initiateElection(List<Node> nodes) {
        System.out.println("Node " + nodeId + " initiates election.");

        for (Node node : nodes) {
            if (node.getNodeId() > this.nodeId) {
                // Send election message to higher priority nodes
                node.receiveElectionMessage(this);
            }
        }

        // Assume election process completes after initiating
        becomeCoordinator();
    }

    // Method to receive election message from another node
    public void receiveElectionMessage(Node sender) {
        System.out.println("Node " + nodeId + " receives election message from Node " + sender.getNodeId());

        // Respond if current node has higher priority
        if (this.nodeId > sender.getNodeId()) {
            System.out.println("Node " + nodeId + " responds to Node " + sender.getNodeId());
            sender.receiveResponse(this);
        }
    }

    // Method to receive response and acknowledge as coordinator
    public void receiveResponse(Node sender) {
        System.out.println("Node " + nodeId + " receives response from Node " + sender.getNodeId());
    }

    // Method to become the coordinator
    public void becomeCoordinator() {
        System.out.println("Node " + nodeId + " becomes the coordinator.");
        this.isCoordinator = true;
    }
}

public class bully_algo {

    public static void main(String[] args) {
        // Create nodes
        Node node1 = new Node(1);
        Node node2 = new Node(2);
        Node node3 = new Node(3);
        Node node4 = new Node(4);
        Node node5 = new Node(5);

        // List of nodes in the distributed system
        List<Node> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);
        nodes.add(node5);

        // Simulate failure of current coordinator (Node 3)
        node3.setCoordinator(false);

        // Assume Node 3 detects coordinator failure and initiates election
        node3.initiateElection(nodes);
    }
}