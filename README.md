# cs457-557-fall2020-pa3-mparlaw1
Instructions

1. compile the project - make 
2. To run replica server - ./server.sh {port} {replica server name } { config value}.  example:  ./server.sh 9011 s1 true 
        Note:  value: false -> Read repair. true -> Hinted Hand-off
3. To run client - ./client.sh 



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
