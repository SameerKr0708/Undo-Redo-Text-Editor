/**
 * A simple Stack (LIFO) built on top of a singly linked list.
 * The "top" pointer always refers to the most recently pushed node,
 * so push(), pop() and peek() all run in O(1) time.
 */
public class Stack {

    // One node of the linked list
    private static class Node {
        String data;
        Node next;

        Node(String data) {
            this.data = data;
        }
    }

    private Node top;   // reference to the top node
    private int size;   // number of items in the stack

    /** Adds an item on top of the stack. */
    public void push(String value) {
        Node node = new Node(value);
        node.next = top;
        top = node;
        size++;
    }

    /** Removes and returns the top item, or null if the stack is empty. */
    public String pop() {
        if (isEmpty()) {
            return null;
        }
        String value = top.data;
        top = top.next;
        size--;
        return value;
    }

    /** Returns the top item without removing it, or null if empty. */
    public String peek() {
        return isEmpty() ? null : top.data;
    }

    public boolean isEmpty() {
        return top == null;
    }

    public int size() {
        return size;
    }

    /** Removes every item from the stack. */
    public void clear() {
        top = null;
        size = 0;
    }

    /** Returns the items from top to bottom (used by the GUI history panel). */
    public String[] toArray() {
        String[] items = new String[size];
        Node current = top;
        int i = 0;
        while (current != null) {
            items[i++] = current.data;
            current = current.next;
        }
        return items;
    }
}
