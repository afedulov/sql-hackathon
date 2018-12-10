import org.apache.flink.api.common.time.Time
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.TableEnvironment
import org.apache.flink.table.api.scala._
import org.apache.flink.types.Row
import scala.collection.mutable

object SqlPlayground {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    val tEnv = TableEnvironment.getTableEnvironment(env)

    val data = new mutable.MutableList[(String, Long, Int, Int)]
    //first window
    data.+=(("ACME", Time.seconds(1).toMilliseconds, 1, 1))
    data.+=(("ACME", Time.seconds(2).toMilliseconds, 2, 2))
    //second window
    data.+=(("ACME", Time.seconds(4).toMilliseconds, 1, 4))
    data.+=(("ACME", Time.seconds(5).toMilliseconds, 1, 3))
    //third window
    data.+=(("ACME", Time.seconds(7).toMilliseconds, 2, 3))
    data.+=(("ACME", Time.seconds(8).toMilliseconds, 2, 3))

    data.+=(("ACME1", Time.seconds(1).toMilliseconds, 20, 4))
    data.+=(("ACME1", Time.seconds(1).toMilliseconds, 24, 4))
    data.+=(("ACME1", Time.seconds(1).toMilliseconds, 25, 3))
    data.+=(("ACME1", Time.seconds(1).toMilliseconds, 19, 8))

    val t = env.fromCollection(data)
      .assignAscendingTimestamps(e => e._2)
      .toTable(tEnv, 'symbol, 'tstamp.rowtime, 'price, 'tax)
    tEnv.registerTable("Ticker", t)

    val sqlQuery =
      s"""
         |SELECT *
         |FROM (
         |   SELECT
         |      symbol,
         |      SUM(price) as price,
         |      TUMBLE_ROWTIME(tstamp, interval '3' second) as rowTime,
         |      TUMBLE_START(tstamp, interval '3' second) as startTime
         |   FROM Ticker
         |   GROUP BY symbol, TUMBLE(tstamp, interval '3' second)
         |)
         |MATCH_RECOGNIZE (
         |  PARTITION BY symbol
         |  ORDER BY rowTime
         |  MEASURES
         |    B.price as dPrice,
         |    B.startTime as dTime
         |  ONE ROW PER MATCH
         |  PATTERN (A B)
         |  DEFINE
         |    B AS B.price < A.price
         |)
         |""".stripMargin

    tEnv.sqlQuery(sqlQuery).toAppendStream[Row].print
    env.execute
  }
}
