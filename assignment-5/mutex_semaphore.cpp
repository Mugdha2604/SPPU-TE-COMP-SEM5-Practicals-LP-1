#include <iostream>
#include <queue>
#include <thread>
#include <mutex>
#include <condition_variable> // Required for C++17 semaphore implementation
#include <chrono>

using namespace std;

/**
 * A simple semaphore implementation using C++17 std::mutex and std::condition_variable.
 */
class Semaphore {
public:
    explicit Semaphore(int initial_count = 0)
        : count(initial_count) {}

    /**
     * @brief Decrements the internal counter, or blocks until it can.
     */
    void acquire() {
        // Use std::unique_lock to avoid ambiguity
        std::unique_lock<std::mutex> lock(mtx);
        // Wait until the count is greater than 0
        cv.wait(lock, [this] { return count > 0; });
        // Decrement the count
        count--;
    }

    /**
     * @brief Increments the internal counter and notifies a waiting thread.
     */
    void release() {
        // Use std::lock_guard to avoid ambiguity
        std::lock_guard<std::mutex> lock(mtx);
        // Increment the count
        count++;
        // Notify one waiting thread that the count has changed
        cv.notify_one();
    }

private:
    // Explicitly use std::mutex and std::condition_variable to avoid ambiguity
    std::mutex mtx;
    std::condition_variable cv;
    int count;
};

/**
 * Solves the Producer-Consumer problem using a custom C++17 semaphore.
 * - Producer: Creates items and puts them into a shared buffer.
 * - Consumer: Takes items from the shared buffer and consumes them.
 *
 * Semaphores Used:
 * - empty_slots: Counts the number of empty slots in the buffer. Producer waits on this.
 * - filled_slots: Counts the number of filled slots in the buffer. Consumer waits on this.
 *
 * Mutex Used (as a binary semaphore):
 * - buffer_mutex: Ensures that only one thread can access the shared buffer at a time.
 *
 * NOTE: To compile this code, you need a C++17 compliant compiler.
 * For example, using g++: g++ -std=c++17 -o producer_consumer producer_consumer.cpp -pthread
 */

// The shared buffer with a fixed size.
const int BUFFER_SIZE = 5;
// --- ADD A LIMIT TO THE NUMBER OF ITEMS ---
const int ITEMS_TO_PRODUCE = 20;
const int POISON_PILL = -1; // A special value to signal the end

queue<int> buffer;

// --- RENAMED SEMAPHORES TO AVOID AMBIGUITY ---
// 'buffer_mutex' ensures mutual exclusion for buffer access.
Semaphore buffer_mutex(1);
// 'empty_slots' tracks the number of available empty slots.
Semaphore empty_slots(BUFFER_SIZE);
// 'filled_slots' tracks the number of available items.
Semaphore filled_slots(0);

// Explicitly use std::mutex to avoid ambiguity with the Semaphore named 'mutex'
std::mutex cout_mutex;

/**
 * The producer's logic.
 */
void produce() {
    // --- CHANGED to a 'for' loop ---
    for (int item = 1; item <= ITEMS_TO_PRODUCE; ++item) {
        // Wait for an empty slot to become available.
        empty_slots.acquire();

        // Acquire the mutex to enter the critical section.
        buffer_mutex.acquire();

        // --- Critical Section ---
        buffer.push(item);
        {
            // Explicitly use std::lock_guard
            std::lock_guard<std::mutex> lock(cout_mutex);
            cout << "Producer produced item: " << item << ". Buffer size: " << buffer.size() << endl;
        }
        // --- End of Critical Section ---

        // Release the mutex.
        buffer_mutex.release();

        // Signal that a new item is available for consumption.
        filled_slots.release();

        // Shortened sleep time for a faster demonstration
        this_thread::sleep_for(chrono::milliseconds(100));
    }

    // --- ADD STOPPING LOGIC ---
    // After finishing production, send a "poison pill" to the consumer
    {
        std::lock_guard<std::mutex> lock(cout_mutex);
        cout << "Producer finished. Sending poison pill..." << endl;
    }
    empty_slots.acquire();
    buffer_mutex.acquire();
    buffer.push(POISON_PILL);
    buffer_mutex.release();
    filled_slots.release(); // Signal the consumer to check this last "item"
}

/**
 * The consumer's logic.
 */
void consume() {
    // --- CHANGED to 'while (true)' with a break condition ---
    while (true) {
        // Wait for an item to become available.
        filled_slots.acquire();

        // Acquire the mutex to enter the critical section.
        buffer_mutex.acquire();

        // --- Critical Section ---
        int consumed_item = buffer.front();
        buffer.pop();
        
        // --- ADD STOPPING LOGIC ---
        if (consumed_item == POISON_PILL) {
             {
                std::lock_guard<std::mutex> lock(cout_mutex);
                cout << "Consumer received poison pill. Stopping." << endl;
            }
            // Release the mutex and signal the empty slot, then break
            buffer_mutex.release();
            empty_slots.release(); // Must balance the semaphores
            break; // Exit the loop
        }

        {
            // Explicitly use std::lock_guard
            std::lock_guard<std::mutex> lock(cout_mutex);
            cout << "Consumer consumed item: " << consumed_item << ". Buffer size: " << buffer.size() << endl;
        }
        // --- End of Critical Section ---

        // Release the mutex.
        buffer_mutex.release();

        // Signal that an empty slot is now available.
        empty_slots.release();

        // Shortened sleep time for a faster demonstration
        this_thread::sleep_for(chrono::milliseconds(150));
    }
}


int main() {
    cout << "--- Starting Producer-Consumer Simulation (C++) ---" << endl;
    cout << "Buffer size: " << BUFFER_SIZE << ", Items to produce: " << ITEMS_TO_PRODUCE << endl;

    // Create and start the producer and consumer threads.
    thread producer_thread(produce);
    thread consumer_thread(consume);

    // The threads will now exit their loops, so join() will complete.
    producer_thread.join();
    consumer_thread.join();

    cout << "--- Simulation Finished ---" << endl;
    return 0;
}