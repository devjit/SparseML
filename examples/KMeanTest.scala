import org.apache.log4j.{Level, Logger}
import org.apache.spark.mllib.clustering.{ScalableKMeans, KMeans}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.linalg.{SparseVector, Vectors, Vector}

import scala.util.Random


//spark/bin/spark-submit --master spark://10.100.34.48:7077 --class  ScalableKMeanTest --executor-memory 20g --executor-cores 1 --driver-memory 24g --conf spark.driver.maxResultSize=8g --conf spark.akka.frameSize=1024 unnamed.jar 50 1000000 100 0.1 1 my 9

//guale spark/bin/spark-submit --master spark://10.100.34.48:7077 --class  ScalableKMeanTest --executor-memory 5g --executor-cores 1 --driver-memory 24g --conf spark.driver.maxResultSize=8g --conf spark.akka.frameSize=1024 unnamed.jar 50 5000000 100 0.1 1 my 15
/**
 * Created by yuhao on 1/23/16.
 */
object ScalableKMeanTest {

  def main(args: Array[String]) {
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)

    val conf = new SparkConf().setAppName(s"kmeans: ${args.mkString(",")}")
    val sc = new SparkContext(conf)

    val k = args(0).toInt
    val dimension = args(1).toInt
    val recordNum = args(2).toInt
    val sparsity = args(3).toDouble
    val iterations = args(4).toInt
    val means = args(5)
    val parNumber = args(6).toInt

    val data: RDD[Vector] = sc.parallelize(1 to recordNum, parNumber).map(i => {
      val ran = new Random()
      val indexArr = ran.shuffle((0 until dimension).toList).take((dimension * sparsity).toInt).sorted.toArray
      val valueArr = (1 to (dimension * sparsity).toInt).map(in => ran.nextDouble()).sorted.toArray
      val vec: Vector = new SparseVector(dimension, indexArr, valueArr)
      vec
    }).cache()
    println(args.mkString(", "))
    println(data.count() + " records generated")

    val st = System.nanoTime()

    val model = if(means == "my") {
      println("running scalable kmeans")
      val model = new ScalableKMeans()
        .setK(k)
        .setInitializationMode("random")
        .setMaxIterations(iterations)
        .run(data)
      model
    } else {
      println("running mllib kmeans")
      val model = new KMeans()
        .setK(k)
        .setInitializationMode("random")
        .setMaxIterations(iterations)
        .run(data)
      model
    }

    println((System.nanoTime() - st) / 1e9 + " seconds cost")
    println("final clusters: " + model.clusterCenters.length)
    println(model.clusterCenters.map(v => v.numNonzeros).mkString("\n"))

    sc.stop()
  }

}
