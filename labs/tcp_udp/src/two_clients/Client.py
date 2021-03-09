import socket

serverIP = "127.0.0.1"
serverPort = 9010
msg = "Ping"

print('PYTHON UDP CLIENT')
client = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
client.sendto(bytes(msg, 'utf-8'), (serverIP, serverPort))

msg, _ = client.recvfrom(serverPort)

print("received msg:", str(msg, 'utf-8'))