#ifndef PAKC_SOCKET_H
#define PAKC_SOCKET_H

#include "import.h"
#include <sys/time.h>



#define SOCKET_NAME "somethingf"
#define BACKLOG 8
int sock, clientD;
sockaddr_un addr_server;
char socket_name[108];



int Create()
{
	int isCreated = (sock = socket(AF_UNIX, SOCK_STREAM, 0)) >= 0;
	return isCreated;
}

void Close()
{
	// Reset each fd to -1 right after closing. Without this, Close() (called from every
	// socket error path: sendData/recvData/Accept/Bind/Listen) leaves the old fd numbers
	// in place, so a later error closes the SAME fd again. By then the OS has reused that
	// number for a Parcel, and Android's fdsan aborts the whole process (signal 35) inside
	// Overlay.DrawOn -> ESPView.onDraw. Nulling the fds makes the double-close a no-op.
	if (clientD > 0) {
		close(clientD);
		clientD = -1;
	}
	if (sock > 0) {
		close(sock);
		sock = -1;
	}
}

int Accept()
{
	if ((clientD = accept(sock, nullptr, nullptr)) < 0)
	{
		Close();
		return 0;
	}
	// Cap blocking reads so Overlay.DrawOn cannot freeze the UI forever.
	// Response is large (~100KB+); 120ms was too short under load and caused half-reads.
	struct timeval tv;
	tv.tv_sec = 0;
	tv.tv_usec = 500000; // 500ms
	setsockopt(clientD, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
	setsockopt(clientD, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
	return 1;
}

int Bind()
{
	memset(socket_name, 0, sizeof(socket_name));
	memcpy(&socket_name[0], "\0", 1);
	strcpy(&socket_name[1], SOCKET_NAME);

	memset(&addr_server, 0, sizeof(addr_server));
	addr_server.sun_family = AF_UNIX;	// Unix Domain instead of AF_INET IP
										// domain
	strncpy(addr_server.sun_path, socket_name, sizeof(addr_server.sun_path) - 1);	// 108 
																					// char 
																					// max
	int amit = bind(sock, (struct sockaddr *)&addr_server, sizeof(addr_server));
	if (amit < 0)
	{
		Close();
		return 0;
	}
	return 1;
}

int Listen()
{
	if (listen(sock, BACKLOG) < 0)
	{
		Close();
		return 0;
	}
	return 1;
}

int sendData(void *inData, size_t size)
{
	char *buffer = (char *)inData;
	int numSent = 0;

	while (size)
	{
		do
		{
			numSent = write(clientD, buffer, size);
		}
		while (numSent == -1 && EINTR == errno);

		if (numSent <= 0)
		{
			Close();
			break;
		}

		size -= numSent;
		buffer += numSent;
	}
	return numSent;
}

int send(void *inData, size_t size)
{
	uint32_t length = htonl(size);
	if (sendData(&length, sizeof(uint32_t)) <= 0)
	{
		return 0;
	}
	return sendData(inData, size) > 0;
}

int recvData(void *outData, size_t size)
{
	char *buffer = (char *)outData;
	int numReceived = 0;

	while (size)
	{
		do
		{
			numReceived = read(clientD, buffer, size);
		}
		while (numReceived == -1 && EINTR == errno);

		if (numReceived <= 0)
		{
			Close();
			break;
		}

		size -= numReceived;
		buffer += numReceived;
	}
	return numReceived;
}

size_t receive(void *outData)
{
	uint32_t length = 0;
	int code = recvData(&length, sizeof(uint32_t));
	if (code > 0)
	{
		length = ntohl(length);
		recvData(outData, static_cast < size_t > (length));
	}
	return length;
}


#endif // PAKC_SOCKET_H
