/**
  * Copyright 2015 Thomson Reuters
  *
  * Licensed under the Apache License, Version 2.0 (the “License”); you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
  * an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */


package cmwell.irw

import org.scalatest._
import cmwell.domain._
import cmwell.util.exceptions._
import cmwell.driver.Dao
import cmwell.util.{Box, FullBox, EmptyBox, BoxedFailure}
import cmwell.util.concurrent.SimpleScheduler.scheduleFuture
import org.apache.commons.codec.binary.Base64

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

/**
 * Created with IntelliJ IDEA.
 * User: markz
 * Date: 2/24/13
 * Time: 8:32 AM
 * Tests for IRWService
 */
class IRWCassSpecOld extends {
  val keyspace = "data"
  val mkIRW: Dao => IRWService = { dao =>
    IRWService(dao, 25, true, 120.seconds)
  }
} with IRWCassSpec

class IRWCassSpecNew extends {
  val keyspace = "data2"
  val mkIRW: Dao => IRWService = { dao =>
    IRWService.newIRW(dao, 25, true, 120.seconds)
  }
} with IRWCassSpec

trait IRWServiceTest extends BeforeAndAfterAll { this:Suite =>

  def keyspace: String
  def mkIRW: Dao => IRWService

  var irw : IRWService = _
  var dao : Dao = _

  override protected def beforeAll() {
    dao = Dao("Test",keyspace)
    irw = mkIRW(dao)
    super.beforeAll()
  }

  override protected def afterAll() {
    super.afterAll()
    dao.shutdown()
  }
}

trait IRWCassSpec extends AsyncFlatSpec with Matchers with IRWServiceTest {

  val waitDuration = {
    import scala.concurrent.duration._
    2.seconds
  }

  "test" should "be successful" in {
    (true) should equal(true)
  }

  "bulk write and read" should "be successful" in {

    val data = Vector.tabulate(10) { i =>
      ObjectInfoton("/cmt/cm/bulk-test/objinfo_" + i, "dc_test", None, Map("name" -> Set[FieldValue](FString("gal"), FString("yoav"))))
    }

    irw.writeSeqAsync(data).flatMap { _ =>

      val uuids = data.map(_.uuid)
      val paths = data.map(_.path)
      val junks = Vector("no1", "no2", "no3")

      val uuidsRead = irw.readUUIDSAsync(uuids).map(_.collect { case FullBox(i) => i })
      val pathsRead = irw.readPathsAsync(paths).map(_.collect { case FullBox(i) => i })
      val uuidsWithJunkRead = irw.readUUIDSAsync(uuids ++ junks).map(_.collect { case FullBox(i) => i })
      val pathsWithJunkRead = irw.readPathsAsync(paths ++ junks).map(_.collect { case FullBox(i) => i })

      for {
        us <- uuidsRead
        ps <- pathsRead
        uj <- uuidsWithJunkRead
        pj <- pathsWithJunkRead
      } yield {
        withClue(s"number of infotons retrieved by uuids ${us.mkString("[",",","]")} must equal the total number of written infotons [${data.size}]") {
          us.size should equal(data.size)
        }
        withClue(s"number of infotons retrieved by paths ${ps.mkString("[",",","]")} must equal the total number of written infotons [${data.size}]") {
          ps.size should equal(data.size)
        }
        withClue(s"number of infotons retrieved by uuids ${uj.mkString("[",",","]")} and junk ${junks.mkString("[",",","]")} must equal the total number of written infotons [${data.size}]") {
          uj.size should equal(data.size)
        }
        withClue(s"number of infotons retrieved by uuids ${pj.mkString("[",",","]")} and junk ${junks.mkString("[",",","]")} must equal the total number of written infotons [${data.size}]") {
          pj.size should equal(data.size)
        }
      }
    }
  }

  "try to read none exisiting infoton" should "be successful" in {
    val objUuidInfo : Box[Infoton] = Await.result(irw.readUUIDAsync("nono"), waitDuration)
    objUuidInfo match {
      case FullBox(info) => assert(false)
      case _ => assert(true)
    }

    val objPathInfo : Box[Infoton] = Await.result(irw.readPathAsync("/cmt/path_not_exists"), waitDuration)
    objPathInfo match {
      case FullBox(info) => assert(false)
      case _ => assert(true)
    }
  }

  "file write and read" should "be successful" in {
    val content = new String("iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAALEgAACxIB0t1+/AAAABV0RVh0Q3JlYXRpb24gVGltZQA2LzI0LzA59sFr4wAAABx0RVh0U29mdHdhcmUAQWRvYmUgRmlyZXdvcmtzIENTNAay06AAAAIgSURBVDiNlZI9aFNRGIaf+2PSVJvc5PZHJaRxCsZCW52iUap0KChYB3E0jkUQ0cVB7FVR10JRcEtnh2booKCYgOCg6Q/FQqHiLTQh2sZ7g0rU5PY4JK2J1lI/+Dh8h/M+533hk3L3CegKU26I8R/1A14XHc6ogXWm3J3RGHsPgbRDtQB34V0sUFiYUluqxJAh+/IJAG0toHlAbwVF3gbijdJSJSaJmwj8wDUBwJf3Gez5FPbbJLpqs9+7DcQCSdxAoAMX0uALg7cbAOd7iU/pMawXBpGOf7gpgiSuI+houPSFIW5Az0UAyrk5lsYHiPrsvyGrIIkrCLogmwOXCroHuvaA4g/D+RR09uKUSyzdCRNps5sBH0GmAjhw5KEgcstEOm6wYGvYeRMe98HcBIrHR+hymrxde7vZlQYAZgbXbo19p0eJ3jUpBoexvwGTCYSZwRPsRT5h4FSbAZJIIAg22DplwMlRAD48Okcon0Lxh3FGZhCAde8AHXI9ygrIG7R8CeYLkJ80YLwfgNClJKsVDYomTE+gtmrQl2iKIVO3pA4aRB6YqIMGrMzC89soHh/ysavggPJqDAB3z9nfgGoDQI8ncLV3o8frPzw1WC+X0I7W5zWTytoy3oMDfwAc4Csob5JgLdfODXv5WVzt3ZvzrpJZy17X4ID0eZisX+UwGuDZYtu2qjJgg1VlWl2UGYr85Jm/QP8O5QBYMjOLKkO/ABjzzMAyxYbTAAAAAElFTkSuQmCC").getBytes

    val c = new Base64().decode(content)
    val fileContent = FileContent(c, "image/png")

    val fileInfo = FileInfoton("/irw/command-test/file_1","dc_test", Some(666L), Map("name" -> Set[FieldValue](FString("gal"), FString("yoav"))) , fileContent)
    Await.ready(irw.writeAsync(fileInfo),waitDuration)
    val cmpObjInfo : Box[Infoton] = Await.result(irw.readUUIDAsync(fileInfo.uuid), waitDuration)
    cmpObjInfo match {
      case FullBox(i) =>
        i.path  should equal (fileInfo.path)
        i.uuid  should equal (fileInfo.uuid)
        i.lastModified should equal (fileInfo.lastModified)
        i.fields.size should equal (fileInfo.fields.size)
        i.indexTime should equal (Some(666L))
        i match {
          case FileInfoton(_,_,_,_,_,c,_) =>
            c match {
              case Some(cc) =>
                withClue(new String(cc.data.get) + "-------------------------------------" + new String(fileContent.data.get)) {
                  new String(cc.data.get) should equal(new String(fileContent.data.get))
                  cc.length should equal(fileContent.length)
                  cc.mimeType should equal(fileContent.mimeType)
                }
              case None => assert(false)
            }
          case _ =>
            assert(false)

        }
      case _ => assert(false)
    }

  }
  "object utf write and read " should "be successful" in {
    val objInfo = ObjectInfoton("/irw/utf8/objinfo_1","dc_test", None, Map("name" -> Set[FieldValue](FString("罗湖区南湖路国贸商业大厦28-30楼\"@ese"), FString("罗湖区南湖路国贸商业大厦30楼a、B\"@ese"))))
    Await.ready(irw.writeAsync(objInfo),waitDuration)
    // lets compare by uuid read
    val cmpObjInfo : Box[Infoton] = Await.result(irw.readUUIDAsync(objInfo.uuid), waitDuration)
    cmpObjInfo match {
      case FullBox(i) =>
        i.path  should equal (objInfo.path)
        i.uuid  should equal (objInfo.uuid)
        i.lastModified should equal (objInfo.lastModified)
        i.fields.size should equal (objInfo.fields.size)
      case _ => assert(false)
    }
  }

  "object write and read" should "be successful" in {
    val objInfo = ObjectInfoton("/irw/command-test/objinfo_1","dc_test", None, Map("name" -> Set[FieldValue](FString("gal"), FString("yoav"))))
    val vecSizeTry = Try {
      Await.ready(irw.writeAsync(objInfo), waitDuration)
      // lets compare by uuid read
      val cmpObjInfo: Box[Infoton] = Await.result(irw.readUUIDAsync(objInfo.uuid), waitDuration)
      cmpObjInfo match {
        case FullBox(i) =>
          i.path should equal(objInfo.path)
          i.uuid should equal(objInfo.uuid)
          i.lastModified should equal(objInfo.lastModified)
          i.fields.size should equal(objInfo.fields.size)
        case _ => assert(false)
      }
      // lets compare by path read
      val cmpByPathObjInfo: Box[Infoton] = Await.result(irw.readPathAsync(objInfo.path), waitDuration)
      cmpByPathObjInfo match {
        case FullBox(i) =>
          i.path should equal(objInfo.path)
          i.uuid should equal(objInfo.uuid)
          i.lastModified should equal(objInfo.lastModified)
          i.fields.size should equal(objInfo.fields.size)
        case _ => assert(false)
      }

      val objInfo_v2 = ObjectInfoton("/irw/command-test/objinfo_1", "dc_test", None, Map("name" -> Set[FieldValue](FString("gal"), FString("yoav"), FString("rony"))))
      Await.ready(irw.writeAsync(objInfo_v2), waitDuration)
      // lets 2 version compare by uuid read
      val cmpObjInfo_v2: Box[Infoton] = Await.result(irw.readUUIDAsync(objInfo_v2.uuid), waitDuration)
      cmpObjInfo_v2 match {
        case FullBox(i) =>
          i.path should equal(objInfo_v2.path)
          i.uuid should equal(objInfo_v2.uuid)
          i.lastModified should equal(objInfo_v2.lastModified)
          i.fields.size should equal(objInfo_v2.fields.size)
        case _ => assert(false)
      }

      // lets compere by path read
      val cmpByPathObjInfo_v2: Box[Infoton] = Await.result(irw.readPathAsync(objInfo_v2.path), waitDuration)
      cmpByPathObjInfo_v2 match {
        case FullBox(i) =>
          i.path should equal(objInfo_v2.path)
          i.uuid should equal(objInfo_v2.uuid)
          i.lastModified should equal(objInfo_v2.lastModified)
          i.fields.size should equal(objInfo_v2.fields.size)
        case _ => assert(false)
      }
      // lets check history method
      val vec = irw.history(objInfo.path, 100000)
      vec.size
    }

    withClue(vecSizeTry.transform(i => Try(s"got $i"),e => Try(stackTraceToString(e)))) {
      vecSizeTry.isSuccess should be (true)
      vecSizeTry.get should equal (2)
    }
  }

  "write fat infoton (with more than 65K fields/values)" should "succeed" in {
    val lotsOfFields = Seq.tabulate(0xFFFF * 2){ n =>
      s"field$n" -> Set[FieldValue](FString(s"value$n"))
    }.toMap

    val fatFoton = ObjectInfoton("/irw/xyz/fatfoton1", "dc_test", None, lotsOfFields)

    irw.writeAsync(fatFoton).flatMap{ _ =>
      scheduleFuture(10.seconds) {
        irw.readPathAsync("/irw/xyz/fatfoton1").map{
          case FullBox(readInfoton) => readInfoton shouldBe fatFoton
          case EmptyBox => fail("/irw/xyz/fatfoton1 was not found")
          case BoxedFailure(e) => fail("error occured",e)
        }
      }
    }
  }

  "object write and update indexTime" should "be successful" in {
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    val objInfo = ObjectInfoton("/irw/command-test/JohnSmith","dc_test", None, Map("status" -> Set[FieldValue](FString("R.I.P"))))
    val idxT = 1234567891011L
    val f = irw.writeAsync(objInfo).flatMap{_ =>
      val uuid = objInfo.uuid
      irw.addIndexTimeToUuid(uuid, idxT)
    }
    Await.ready(f,10.seconds)
    Await.result(irw.readUUIDAsync(objInfo.uuid),10.seconds) match {
      case FullBox(i) => i.indexTime  should equal (Some(idxT))
      case _ => fail("could not retrieve infoton from IRW")
    }
  }
}
