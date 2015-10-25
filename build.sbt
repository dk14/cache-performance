name := "cache-performance"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies += "nl.grons"          %% "metrics-scala"    % "3.5.1_a2.3"

libraryDependencies += "org.scalaz"        %% "scalaz-core"      % "7.1.0"

libraryDependencies += "org.scalaz.stream" %% "scalaz-stream"    % "0.7.2a"

//DSE (Cassandra+Solr+Spark):

libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.8"

libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-mapping" % "2.1.8"

libraryDependencies += "com.datastax.spark" %% "spark-cassandra-connector" % "1.5.0-M2"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.5.1"

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "1.5.1"

libraryDependencies += "com.websudos" %% "phantom-dsl" % "1.12.2" exclude ("com.websudos", "diesel-engine_2.11")

//Hazelcast:

libraryDependencies += "com.hazelcast" % "hazelcast" % "3.5.3"

libraryDependencies += "javax.cache" % "cache-api" % "1.0.0"

libraryDependencies += "org.apache.solr" % "solr-core" % "4.6.0.3.4-SNAPSHOT"