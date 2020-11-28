# cs457-557-fall2020-pa3-mparlaw1
Instructions

1. compile the project - make 
2. To run replica server - ./server.sh {port} {replica server name } { config value}.  example:  ./server.sh 9011 s1 true 
        Note:  value: false -> Read repair. true -> Hinted Hand-off
3. To run client - ./client.sh 



PROJET DEMO:

 NOTE : Select 0 for S1
        Select 1 for S2
        Select 2 for S3
        Select 3 for S4

PART ONE : hinted handoff mode
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

PART 2:  Read Repair MODE
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

Input - Output:

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
