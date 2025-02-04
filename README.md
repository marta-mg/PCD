# PCD
share files system P2P

Compilar IscTorrent: 
cd src
javac -cp . gui/*.java
javac -cp . descarregamento/*.java
javac -cp . ligacao/*.java
javac -cp . procura/*.java

java gui/IscTorrent 8081 dl1
