#include <bits/stdc++.h>

using namespace std;

class block
{
    public:
    int size;
    int startAddress;
    bool allocated;

    block(int s, int addr)
    {
        size = s;
        startAddress = addr;
        allocated = false;
    }
};

class process
{
    public:
    int size;
    int processId;
    bool isallocated;
    int blockStartAddr;

    process(int id, int sz)
    {
        processId = id;
        size = sz;
        isallocated = false;
        blockStartAddr = 0;
    }
};

void displayMemory(vector<block> &blocks, vector<process> &processes)
{
    for (int i=0; i<processes.size(); i++)
    {
        //cout<<"Process "<<processes[i].processId<<" : Start address : "<<processes[i].blockStartAddr<<endl;
        cout<<"Block"<<i+1<<" startAddress : "<<blocks[i].startAddress<<" size : "<<blocks[i].size<<endl;
    }
}

void firstfit (vector<block> &blocks, vector<process> &processes)
{

    for (process &p : processes)
    {
        //process p = processes[i];

        for (block &b : blocks)
        {
            //block b = blocks[j];

            if (b.size>=p.size && !b.allocated)
            {
                p.blockStartAddr = b.startAddress;
                p.isallocated = true; 
                
                b.startAddress += p.size;
                b.size -= p.size;
                cout<<"Process "<<p.processId<<" : "<<"start Address : "<<p.blockStartAddr<<endl;
                break;
            }
        }
    }
    cout<<"\nUpdated Memory blocks : "<<endl;
    displayMemory(blocks,processes);
  
    
}

void nextfit (vector<block> &blocks, vector<process> &processes)
{
    int lastpos = 0;
    for (process &p : processes)
    {
        for (int i = lastpos; i<blocks.size(); i++)
        {
            block &b = blocks[i];
            if (b.size>=p.size && !b.allocated)
            {
                p.blockStartAddr = b.startAddress;
                p.isallocated = true; 
                
                b.startAddress += p.size;
                b.size -= p.size;
                cout<<"Process "<<p.processId<<" : "<<"start Address : "<<p.blockStartAddr<<endl;
                lastpos = i;
                break;
            }

        }
    }
    cout<<"\nUpdated Memory blocks : "<<endl;
    displayMemory(blocks,processes);
}

void bestfit(vector<block> &blocks, vector<process> &processes)
{
    for (process &p : processes)
    {
        int minBlockSize = 1e9;
        // int minAdd = 0;
        //block &minBlock = blocks[0];
        for (block &b : blocks)
        {
            if (b.size>=p.size && !b.allocated)
            {
                //finding minimum block size >= process size

                if (b.size<minBlockSize)
                {
                    
                    minBlockSize = b.size;
                    //minBlock = b;
                    // minAdd = b.startAddress;
                }
            }
        }

        for (block &b : blocks)
        {
            if (b.size == minBlockSize)
            {
                p.blockStartAddr = b.startAddress;
                p.isallocated = true; 
                
                b.startAddress += p.size;
                b.size -= p.size;
                cout<<"Process "<<p.processId<<" : "<<"start Address : "<<p.blockStartAddr<<endl;
                break;

            }
        }
        // p.blockStartAddr = minBlock.startAddress;
        // p.isallocated = true; 
                
        // minBlock.startAddress += p.size;
        // minBlock.size -= p.size;
        // cout<<"Process "<<p.processId<<" : "<<"start Address : "<<p.blockStartAddr<<endl;

    }
    cout<<"\nUpdated Memory blocks : "<<endl;
    displayMemory(blocks,processes);
}
int main()
{
    vector<block> blocks;
    cout<<"\nEnter Block size and start address : "<<endl;

    for (int i=0; i<5; i++)
    {
        int size,addr;
        cout<<"\nEnter block size: ";
        cin>>size;
        cout<<"\nEnter block starting address: ";
        cin>>addr;
        block b(size,addr);
        blocks.push_back(b);
    }

    vector<process> processes;

    for (int i=0; i<4; i++)
    {
        int size,id;
        cout<<"\nEnter Process id : ";
        cin>>id;
        cout<<"\nEnter Process size : ";
        cin>>size;
        process p(id,size);
        processes.push_back(p);
    }

    vector<block> blockCopy = blocks;
    vector<process> processCopy = processes;

    cout<<"-------------First fit-------------------"<<endl;
    firstfit(blocks,processes);
    cout<<"\n----------------Next Fit-----------------"<<endl;
    blocks = blockCopy;
    processes = processCopy;
    nextfit(blocks,processes);
    blocks = blockCopy;
    processes = processCopy;
    cout<<"\n-----------------Best Fit-------------------"<<endl;
    bestfit(blocks,processes);
    return 0;
}