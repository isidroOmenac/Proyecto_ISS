-- MySQL dump 10.13  Distrib 5.7.17, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: iss
-- ------------------------------------------------------
-- Server version	5.5.5-10.1.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `login`
--

DROP TABLE IF EXISTS `login`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `login` (
  `user` varchar(50) NOT NULL,
  `password` varchar(30) NOT NULL,
  `email` varchar(50) NOT NULL,
  `nombre` varchar(20) NOT NULL,
  `Apellidos` varchar(50) NOT NULL,
  `Dirección` varchar(60) NOT NULL,
  `id` int(10) NOT NULL,
  PRIMARY KEY (`id`,`user`),
  UNIQUE KEY `user_UNIQUE` (`user`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='Aquí guardamos las posibles instrucciones sobre el control de la temperatura';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login`
--

LOCK TABLES `login` WRITE;
/*!40000 ALTER TABLE `login` DISABLE KEYS */;
INSERT INTO `login` VALUES ('juanito','ja','ja','ja','aj','ja',1),('isidro','ja','jai','jaj','ja','ja',2);
/*!40000 ALTER TABLE `login` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registro_amb`
--

DROP TABLE IF EXISTS `registro_amb`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `registro_amb` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `temp` float NOT NULL,
  `user` varchar(50) NOT NULL,
  `fechaHora` mediumtext NOT NULL,
  `sensor` varchar(45) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `fk_user_idx` (`user`),
  KEY `fk_sensor_idx` (`sensor`),
  CONSTRAINT `fk_sensor` FOREIGN KEY (`sensor`) REFERENCES `sensor` (`sensor`) ON DELETE NO ACTION ON UPDATE CASCADE,
  CONSTRAINT `fk_user` FOREIGN KEY (`user`) REFERENCES `login` (`user`) ON DELETE NO ACTION ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registro_amb`
--

LOCK TABLES `registro_amb` WRITE;
/*!40000 ALTER TABLE `registro_amb` DISABLE KEYS */;
INSERT INTO `registro_amb` VALUES (1,23,'juanito','2147483647','juanito salon'),(2,34,'juanito','132131','juanito baño'),(11,25,'isidro','1527239724313','isidro baño'),(12,30,'isidro','1527239729951','isidro baño');
/*!40000 ALTER TABLE `registro_amb` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registro_sensor`
--

DROP TABLE IF EXISTS `registro_sensor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `registro_sensor` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `user` varchar(50) NOT NULL,
  `sensor` varchar(45) NOT NULL,
  `power` tinyint(4) NOT NULL,
  `fechaHora` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `fk_user_idx` (`user`),
  KEY `fk_sensor_idx` (`sensor`),
  CONSTRAINT `fk_sensor4` FOREIGN KEY (`sensor`) REFERENCES `sensor` (`sensor`) ON DELETE NO ACTION ON UPDATE CASCADE,
  CONSTRAINT `fk_user4` FOREIGN KEY (`user`) REFERENCES `login` (`user`) ON DELETE NO ACTION ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registro_sensor`
--

LOCK TABLES `registro_sensor` WRITE;
/*!40000 ALTER TABLE `registro_sensor` DISABLE KEYS */;
INSERT INTO `registro_sensor` VALUES (40,'isidro','isidro baño',1,'1528128305417');
/*!40000 ALTER TABLE `registro_sensor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registro_user`
--

DROP TABLE IF EXISTS `registro_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `registro_user` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `temp` float NOT NULL,
  `user` varchar(50) NOT NULL,
  `fechaHora` mediumtext NOT NULL,
  `sensor` varchar(45) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `fk_user4_idx` (`user`),
  KEY `fk_sensor_idx` (`sensor`),
  CONSTRAINT `fk_sensor1` FOREIGN KEY (`sensor`) REFERENCES `sensor` (`sensor`) ON DELETE NO ACTION ON UPDATE CASCADE,
  CONSTRAINT `fk_user1` FOREIGN KEY (`user`) REFERENCES `login` (`user`) ON DELETE NO ACTION ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registro_user`
--

LOCK TABLES `registro_user` WRITE;
/*!40000 ALTER TABLE `registro_user` DISABLE KEYS */;
INSERT INTO `registro_user` VALUES (7,35,'juanito','1521403591440','juanito baño'),(8,22,'juanito','1521403753429','juanito salon'),(9,22,'isidro','1521403794979','isidro baño');
/*!40000 ALTER TABLE `registro_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sensor`
--

DROP TABLE IF EXISTS `sensor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sensor` (
  `user` varchar(50) NOT NULL,
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `sensor` varchar(45) NOT NULL,
  `estado` tinyint(4) NOT NULL,
  `fechaHora` mediumtext NOT NULL,
  PRIMARY KEY (`id`,`sensor`),
  UNIQUE KEY `sensor_UNIQUE` (`sensor`),
  KEY `fk_user3_idx` (`user`),
  CONSTRAINT `fk_user2` FOREIGN KEY (`user`) REFERENCES `login` (`user`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sensor`
--

LOCK TABLES `sensor` WRITE;
/*!40000 ALTER TABLE `sensor` DISABLE KEYS */;
INSERT INTO `sensor` VALUES ('juanito',7,'juanito cocina',0,''),('juanito',8,'juanito baño',1,''),('juanito',9,'juanito salon',0,''),('isidro',10,'isidro baño',0,''),('isidro',11,'isidro patio',0,''),('isidro',12,'isidro entrada',1,'');
/*!40000 ALTER TABLE `sensor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'iss'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-06-04 18:10:38
