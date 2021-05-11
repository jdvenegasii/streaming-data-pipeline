package com.labs1904.spark

import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client.{ConnectionFactory, Get}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.log4j.Logger
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.{DataFrame, SparkSession}


/**
 * Spark Structured Streaming app
 *
 */
object StreamingPipeline {
  System.setProperty("hadoop.home.dir", "C:\\winutils")
  lazy val logger: Logger = Logger.getLogger(this.getClass)
  val jobName = "StreamingPipeline"

  case class Review(marketplace:String, customer_id:String, review_id:String, product_id:String, product_parent:String,
                     product_title:String, product_category:String, star_rating:String, helpful_votes:String, total_votes:String,
                     vine:String, verified_purchase:String, review_headline:String, review_body:String, review_date:String)

  case class ReviewEnriched(marketplace:String, customer_id:String, review_id:String, product_id:String, product_parent:String,
                    product_title:String, product_category:String, star_rating:String, helpful_votes:String, total_votes:String,
                    vine:String, verified_purchase:String, review_headline:String, review_body:String, review_date:String,
                            name:String, birthdate:String, mail:String, sex:String, username:String)

  implicit def stringToBytes(str: String): Array[Byte] = Bytes.toBytes(str)

  implicit def bytesToString(bytes: Array[Byte]): String = Bytes.toString(bytes)

  def main(args: Array[String]): Unit = {
    try {
      val spark = SparkSession.builder().config("spark.hadoop.dfs.client.use.datanode.hostname", "true").config("spark.hadoop.fs.defaultFS", "hdfs://manager.hourswith.expert:8020").appName(jobName).master("local[*]").getOrCreate()
      val bootstrapServers = "35.239.241.212:9092,35.239.230.132:9092,34.69.66.216:9092"

      import spark.implicits._
      val df = spark
        .readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", bootstrapServers)
        .option("subscribe", "reviews-as-tabs")
        .option("startingOffsets", "earliest")
        .option("failOnDataLoss", "false")
        .option("maxOffsetsPerTrigger", "20")
        .load()
        .selectExpr("CAST(value AS STRING)").as[String]


      df.printSchema()

      val reviews = df.map(csvLine => {
        val csvArray = csvLine.split("\t")
        Review(
          csvArray(0),
          csvArray(1),
          csvArray(2),
          csvArray(3),
          csvArray(4),
          csvArray(5),
          csvArray(6),
          csvArray(7),
          csvArray(8),
          csvArray(9),
          csvArray(10),
          csvArray(11),
          csvArray(12),
          csvArray(13),
          csvArray(14),
        )
      })

      val customers = reviews.mapPartitions(partition => {

        val conf = HBaseConfiguration.create()
        conf.set("hbase.zookeeper.quorum", "cdh01.hourswith.expert:2181,cdh02.hourswith.expert:2181,cdh03.hourswith.expert:2181");
        val connection = ConnectionFactory.createConnection(conf)
        val table = connection.getTable(TableName.valueOf("shared:users"))

        val iter = partition.map(reviews => {
          val get = new Get(Bytes.toBytes(reviews.customer_id)).addFamily(Bytes.toBytes("f1"))
          val result = table.get(get)

          println(result)

          val name = result.getValue("f1", "name")
          val birthdate = result.getValue("f1", "birthdate")
          val mail = result.getValue("f1", "mail")
          val sex = result.getValue("f1", "sex")
          val username = result.getValue("f1", "username")

          ReviewEnriched(
            reviews.marketplace,
            reviews.customer_id,
            reviews.review_id,
            reviews.product_id,
            reviews.product_parent,
            reviews.product_title,
            reviews.product_category,
            reviews.star_rating,
            reviews.helpful_votes,
            reviews.total_votes,
            reviews.vine,
            reviews.verified_purchase,
            reviews.review_headline,
            reviews.review_body,
            reviews.review_date,
            name,
            birthdate,
            mail,
            sex,
            username
          )

        }).toList.iterator

        connection.close()

        iter
      })

    //add data clean up here (personal preference)
      val query = customers.writeStream
        .outputMode(OutputMode.Append())
        .format("json")
        .partitionBy("star_rating")
        .option("path", "/user/jvenegas/reviews_ext")
        .option("checkpointLocation", "/user/jvenegas/reviews_checkpoint")
        .trigger(Trigger.ProcessingTime("5 seconds"))
        .start()

      query.awaitTermination()

    } catch {
      case e: Exception => logger.error(s"$jobName error in main", e)
    }
  }
}
