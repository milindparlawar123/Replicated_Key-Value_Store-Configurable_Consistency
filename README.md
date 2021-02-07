#  cs457-557-fall2020-pa3-mparlaw1

# Cassandra-type-Distributed-NoSQL-database
A Cassandra type Eventually Consistent Key-Value Store borrows most of its designs from Cassandra and some from Dynamo.

# Requirenments
Java
Apache Thrift

# Key-Value store

Each replica server will be a key-value store. Keys are unsigned integers between 0 and 255. Values are strings. Each replica server should support the following key-value operations:

get key – given a key, return its corresponding value
put key value – if the key does not already exist, create a new key-value pair; otherwise, update the key to the new value As with Cassandra, to handle a write request, the replica must first log this write in a write-ahead log on persistent storage before updating its in-memory data structure. In this way, if a replica failed and restarted, it can restore its memory state by replaying the disk log.
# Eventual Consistency

Each replica server is pre-configured with information about all other replicas. The replication factor will be 4 – every key-value pair should be stored on all four replicas. Every client request (get or put) is handled by a coordinator. Client can select any replica server as the coordi- nator.

# Consistency level - 
Similar to Cassandra, consistency level is configured by the client. When issuing a request, put or get, the client explicitly specifies the desired consistency level: ONE or QUORUM.
write request : For a write request with consistency level QUORUM, the coordinator will send the request to all replicas (including itself). It will respond successful to the client once the write has been written to two replicas.
read request : For a read request with consistency level QUORUM, the coordinator will return the most recent data from two replicas. To support this operation, when 1handling a write request, the coordinator will record the time at which the request was received and include this as a timestamp when contacting replica servers for writing.
With eventual consistency, different replicas may be inconsistent. For example, due to failure, a replica misses one write for a key k. When it recovers, it replays its log to restore its memory state. When a read request for key k comes next, it returns its own version of the value, which is inconsistent. To ensure that all replicas eventually become consistent, we will implement the following two procedures, and your key-value store will be configured to use either of the two.

# Read repair. 
When handling read requests, the coordinator contacts all replicas. If it finds inconsistent data, it will perform “read repair” in the background.
# Hinted handoff 
During write, the coordinator tries to write to all replicas. As long as enough replicas have succeeded, ONE or QUORUM, it will respond successful to the client. However, if not all replicas succeeded, e.g., three have succeeded but one replica server has failed, the coordinator would store a “hint” locally. If at a later time the failed server has recovered, it might be selected as coordinator for another client’s request. This will allow other replica servers that have stored “hints” for it to know it has recovered and send over the stored hints.

*****************************************************************************************************************************************

# Intructions

1. compile the project - make 
2. To run replica server - ./server.sh {port} {replica server name } { config value}.  example:  ./server.sh 9011 s1 true 
        Note:  value: false -> Read repair. true -> Hinted Hand-off
3. To run client - ./client.sh 



# PROJET DEMO:

 NOTE : Select 0 for S1
        Select 1 for S2
        Select 2 for S3
        Select 3 for S4

# PART ONE : hinted handoff mode
Test Case 1 - Key-value store put/get

Start four replicas: S1, S2, S3 S4, in hinted handoff mode. Initially all four replicas are empty.
A Client C1, selects S1 as Coordinator.

Client	Coordinator	Consistency	Operation	Key	Value
C1	S1	ONE	put	10	aaa
.....................................................................................................

Another client, C2, selects S2 as coordinator
Client	Coordinator	Consistency	Operation	Key	Value
C2	S2	ONE	get	10	(Expected result:aaa)
.....................................................................................................

Test Case 2 - Write-ahead Log

Kill all servers via command line, then restart S1. A client C3 issues request.

Client	Coordinator	Consistency	Operation	Key	Value
C3	S1	ONE	get	10	(Expected result:aaa)
.....................................................................................................

Test Case 3 - Hinted Hand-off
Restart S2, S3, S4. All four replicas are running. Kill S1. A client C4 to issue request.

Client	Coordinator	Consistency	Operation	Key	Value
C4	S2	ONE	put	10	bbb
.....................................................................................................

Restart S1, Instruct another client, C5, to issue request:

Client	Coordinator	Consistency	Operation	Key	Value
C5	S1	ONE	put	20	ccc
.....................................................................................................

Kill S2, S3, S4. Instruct C5 to issue request

Client	Coordinator	Consistency	Operation	Key	Value
C5	S1	ONE	get	10	(Expected result:bbb)
.....................................................................................................

Test Case 4 - Configurable Consistency Level

With S2, S3, S4 killed, instruct C5 to issue request:

Client	Coordinator	Consistency	Operation	Key	Value
C5	S1	QUORUM	get	10	(Expected result:exception)
.....................................................................................................

# PART 2:  Read Repair MODE
 NOTE : Select 0 for S5
        Select 1 for S6
        Select 2 for S7
        Select 3 for S8

Test Case 5 - Read Repair

Start four replicas,S5,S6,S7,S8 in read repair mode.
Instruct a Client C6 to issue request.

Client	Coordinator	Consistency	Operation	Key	Value
C6	S5	QUORUM	put	30	ddd
.....................................................................................................
Kill S8.

Client	Coordinator	Consistency	Operation	Key	Value
C6	S5	QUORUM	put	30	eee
.....................................................................................................
Restart S8, A new client C7 to issue request:

Client	Coordinator	Consistency	Operation	Key	Value
C7	S8	ONE	get	30	(Expected result:ddd)
.....................................................................................................
Instruct C6 to issue request:

Client	Coordinator	Consistency	Operation	Key	Value
C6	S5	QUORUM	get	30	(Expected result:eee)
.....................................................................................................
Kill S5,S6,S7. Instruct C7 to issue request:

Client	Coordinator	Consistency	Operation	Key	Value
C7	S8	ONE	get	30	(Expected result:eee)




# Input - Output:

Server: 

mparlaw1@remote01:~/cs457-557-fall2020-pa3-mparlaw1$ ./server.sh 9011 s1 false
......................................................................
Started the simple server...

Client:
mparlaw1@remote05:~/cs457-557-fall2020-pa3-mparlaw1$ ./client.sh 

select coordinator 

enter 0 for  s1 128.226.114.201 9011
enter 1 for  s2 128.226.114.202 9012
enter 2 for  s3 128.226.114.203 9013
enter 3 for  s4 128.226.114.204 9014

0

 selected coordinator is  s1 128.226.114.201 9011
 
Enter request type : get or put :
put
Enter key :
30
Enter value :
ddd
Enter Consistency Level : ONE or QUORUM : 
QUORUM


# References

Cassandra
http://cassandra.apache.org/

https://www.facebook.com/notes/facebook-engineering/cassandra-a-structured-storage-system-on-a-p2p-network/24413138919/

Dynamo: Amazon’s Highly Available Key-value Store Giuseppe DeCandia, Deniz Hastorun, Madan Jampani, Gunavardhan Kakulapati, Avinash Lakshman, Alex Pilchin, Swaminathan Sivasubramanian, Peter Vosshall and Werner Vogels
