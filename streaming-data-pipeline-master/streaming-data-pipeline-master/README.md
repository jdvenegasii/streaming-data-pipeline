# Hours with Experts - Streaming Data Pipeline

A [Spark streaming](https://spark.apache.org/docs/latest/) application that ingests data
from [Kafka](https://kafka.apache.org/), supplements with data stored in [HBase](https://hbase.apache.org/book.html),
and saves the enriched output to [HDFS](https://hadoop.apache.org/docs/r1.2.1/hdfs_design.html). Done as part of
the [Hours with Experts](https://1904labs.com/our-culture/community/hours-with-experts/) course by 1904Labs

## Getting Started

To start, clone this repo to your computer:

```
git clone https://github.com/1904labs/streaming-data-pipeline.git
```

1. Open the pom.xml in Intellij
2. Select “Open as Project”

Give IntelliJ a few minutes to download the project's dependencies. You'll see the progress bar in the bottom right
loading.

This project will have you

1. Ingest data from a "reviews" Kafka topic.
2. Parse the comma separated values into a Review scala case class
3. Extract the userId from the Review object.
4. Use the userId to lookup the corresponding user data in HBase.
5. Join the review data with the user data.
6. Save this combined result in hdfs
7. Setup a Hive Table that points to the enriched result stored in HDFS


  
