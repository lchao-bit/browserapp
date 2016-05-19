#include <arpa/inet.h>
#include <sys/socket.h>
#include <netdb.h>
#include <ifaddrs.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>

int main(int argc, char *argv[])
{
    struct ifaddrs *ifaddr, *ifa;
    int family, s, t, bs;
    char host[NI_MAXHOST];
    struct sockaddr_rc loc_addr = { 0 }, rem_addr = { 0 };
    char buf[1024] = { 0 };
    int client, bytes_read;
    socklen_t opt = sizeof(rem_addr);

    // allocate socket
    s = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);

    // bind socket to port 1 of the first available 
    // local bluetooth adapter
    loc_addr.rc_family = AF_BLUETOOTH;
    loc_addr.rc_bdaddr = *BDADDR_ANY;
    loc_addr.rc_channel = (uint8_t) 1;
    bind(s, (struct sockaddr *)&loc_addr, sizeof(loc_addr));
    
    while(1)
    {
        // put socket into listening mode
        listen(s, 1);

        // accept one connection
        client = accept(s, (struct sockaddr *)&rem_addr, &opt);

        ba2str( &rem_addr.rc_bdaddr, buf );
        fprintf(stderr, "accepted connection from %s\n", buf);
        memset(buf, 0, sizeof(buf));

        // read data from the client
        bytes_read = read(client, buf, sizeof(buf));
        if( bytes_read > 0 ) {
            printf("received [%s]\n", buf);
        }

        if (getifaddrs(&ifaddr) == -1) 
        {
            perror("getifaddrs");
            exit(EXIT_FAILURE);
        }


        for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) 
        {
            if (ifa->ifa_addr == NULL)
                continue;  

            t = getnameinfo(ifa->ifa_addr,sizeof(struct sockaddr_in),host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);

            if((strcmp(ifa->ifa_name,"br0")==0)&&(ifa->ifa_addr->sa_family==AF_INET))
            {
                if (t != 0)
                {
                    printf("getnameinfo() failed: %s\n", gai_strerror(s));
                    exit(EXIT_FAILURE);
                }
                printf("\tInterface : <%s>\n",ifa->ifa_name );
                printf("\t  Address : <%s>\n", host);
                bs = send(client, host, strlen(host), 0);
                printf("bs: %d\n", bs);
            }
        }
    }
    freeifaddrs(ifaddr);
    close(client);
    exit(EXIT_SUCCESS);
}
