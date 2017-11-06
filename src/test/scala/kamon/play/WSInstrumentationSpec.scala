/* =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.play

import java.net.ConnectException

import kamon.Kamon
import kamon.context.Context
import kamon.context.Context.create
import kamon.testkit._
import kamon.trace.Span.TagValue
import kamon.trace.{Span, SpanCustomizer}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, MustMatchers, OptionValues, WordSpec}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class WSInstrumentationSpec extends WordSpec with MustMatchers
  with ScalaFutures
  with Eventually
  with IntegrationPatience
  with SpanSugar
  with BeforeAndAfterAll
  with MetricInspection
  with Reconfigure
  with OptionValues
  with SpanReporter {

  import reactivemongo.api._

  val driver = MongoDriver.apply()
  val db = driver.connection("localhost:27017").get.database("nezasa_dev")
  val collection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("iam.users"))

  "the instrumentation" should {
    "propagate the current context and generate a span inside a cursor headOption" in {
      val okSpan = Kamon.buildSpan("ok-operation-span").start()

      Kamon.withContext(create(Span.ContextKey, okSpan)) {
        val response = collection.flatMap(x => x.find(BSONDocument.empty).cursor().headOption)
        response.futureValue mustBe defined
      }

      eventually(timeout(2 seconds)) {
        val span = reporter.nextSpan().value
        span.operationName mustBe ""
//        span.operationName mustBe endpoint
//        span.tags("span.kind") mustBe TagValue.String("client")
//        span.tags("http.method") mustBe TagValue.String("GET")
      }


    }

    "propagate the current context and generate a span inside a cursor collect" in {
      val okSpan = Kamon.buildSpan("chupa").start()

      Kamon.withContext(create(Span.ContextKey, okSpan)) {
        val response = collection.flatMap(x => x.find(BSONDocument.empty).cursor().collect[List](maxDocs = 1, err =Cursor.FailOnError[List[BSONDocument]]()))
        response.futureValue.headOption mustBe defined
      }

      eventually(timeout(2 seconds)) {
        val span = reporter.nextSpan().value
        span.operationName mustBe ""
        //        span.operationName mustBe endpoint
        //        span.tags("span.kind") mustBe TagValue.String("client")
        //        span.tags("http.method") mustBe TagValue.String("GET")
      }
    }

    "propagate the current context and generate a span inside a cursor foldResponses" ignore {
      val okSpan = Kamon.buildSpan("chupa").start()

      Kamon.withContext(create(Span.ContextKey, okSpan)) {
        val response = collection.flatMap(x => x.find(BSONDocument.empty).cursor().foldResponses(List.empty[BSONDocument], maxDocs = -1){
          case (docs, response) => Cursor.Cont(docs ++ reactivemongo.core.protocol.Response.parse(response).toList)
        })
        response.futureValue.headOption mustBe defined
      }

      eventually(timeout(2 seconds)) {
        val span = reporter.nextSpan().value
        span.operationName mustBe ""
        //        span.operationName mustBe endpoint
        //        span.tags("span.kind") mustBe TagValue.String("client")
        //        span.tags("http.method") mustBe TagValue.String("GET")
      }
    }

//    "propagate the current context and generate a span inside an async action and complete the ws request" in {
//      val wsClient = app.injector.instanceOf[WSClient]
//      val insideSpan = Kamon.buildSpan("inside-controller-operation-span").start()
//      val endpoint = s"http://localhost:$port/inside-controller"
//
//      Kamon.withContext(create(Span.ContextKey, insideSpan)) {
//        val response = await(wsClient.url(endpoint).get())
//        response.status mustBe 200
//      }
//
//      eventually(timeout(2 seconds)) {
//        val span = reporter.nextSpan().value
//        span.operationName mustBe endpoint
//        span.tags("span.kind") mustBe TagValue.String("client")
//        span.tags("http.method") mustBe TagValue.String("GET")
//      }
//    }
//
//    "propagate the current context and generate a span called not-found and complete the ws request" in {
//      val wsClient = app.injector.instanceOf[WSClient]
//      val notFoundSpan = Kamon.buildSpan("not-found-operation-span").start()
//      val endpoint = s"http://localhost:$port/not-found"
//
//      Kamon.withContext(create(Span.ContextKey, notFoundSpan)) {
//        val response = await(wsClient.url(endpoint).get())
//        response.status mustBe 404
//      }
//
//      eventually(timeout(2 seconds)) {
//        val span = reporter.nextSpan().value
//        span.operationName mustBe "not-found"
//        span.tags("span.kind") mustBe TagValue.String("client")
//        span.tags("http.method") mustBe TagValue.String("GET")
//      }
//    }
//
//    "propagate the current context and generate a span with error and complete the ws request" in {
//      val wsClient = app.injector.instanceOf[WSClient]
//      val errorSpan = Kamon.buildSpan("error-operation-span").start()
//      val endpoint = s"http://localhost:$port/error"
//
//      Kamon.withContext(create(Span.ContextKey, errorSpan)) {
//        val response = await(wsClient.url(endpoint).get())
//        response.status mustBe 500
//      }
//
//      eventually(timeout(2 seconds)) {
//        val span = reporter.nextSpan().value
//        span.operationName mustBe endpoint
//        span.tags("span.kind") mustBe TagValue.String("client")
//        span.tags("http.method") mustBe TagValue.String("GET")
//        span.tags("error") mustBe TagValue.True
//      }
//    }
//
//    "propagate the current context and generate a span with error object and complete the ws request" in {
//      val wsClient = app.injector.instanceOf[WSClient]
//      val errorSpan = Kamon.buildSpan("throw-exception-operation-span").start()
//      val endpoint = s"http://localhost:1000/throw-exception"
//
//      intercept[ConnectException] {
//        Kamon.withContext(create(Span.ContextKey, errorSpan)) {
//          val response = await(wsClient.url(endpoint).get())
//          response.status mustBe 500
//        }
//      }
//
//      eventually(timeout(2 seconds)) {
//        val span = reporter.nextSpan().value
//        span.operationName mustBe endpoint
//        span.tags("span.kind") mustBe TagValue.String("client")
//        span.tags("http.method") mustBe TagValue.String("GET")
//        span.tags("error") mustBe TagValue.True
//        span.tags("error.object").toString must include(TagValue.String("Connection refused").string)
//      }
//    }
//
//    "propagate the current context and pickup a SpanCustomizer and apply it to the new spans and complete the ws request" in {
//      val wsClient = app.injector.instanceOf[WSClient]
//      val okSpan = Kamon.buildSpan("ok-operation-span").start()
//
//      val customizedOperationName = "customized-operation-name"
//      val endpoint = s"http://localhost:$port/ok"
//
//      val context = Context.create(Span.ContextKey, okSpan)
//        .withKey(SpanCustomizer.ContextKey, SpanCustomizer.forOperationName(customizedOperationName))
//
//      Kamon.withContext(context) {
//        val response = await(wsClient.url(endpoint).get())
//        response.status mustBe 200
//      }
//
//      eventually(timeout(2 seconds)) {
//        val span = reporter.nextSpan().value
//        span.operationName mustBe customizedOperationName
//        span.tags("span.kind") mustBe TagValue.String("client")
//        span.tags("http.method") mustBe TagValue.String("GET")
//      }
//    }
  }

//  def insideController(url: String)(app:Application): Action[AnyContent] = Action.async {
//    val wsClient = app.injector.instanceOf[WSClient]
//    wsClient.url(url).get().map(_ ⇒  Ok("Ok"))
//  }
}


