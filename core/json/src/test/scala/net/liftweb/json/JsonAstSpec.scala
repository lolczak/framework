/*
 * Copyright 2009-2011 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb
package json

import org.specs2.mutable._
import org.specs2.ScalaCheck
import org.scalacheck._
import org.scalacheck.Prop.{forAll, forAllNoShrink}


/**
 * System under specification for JSON AST.
 */
object JsonAstSpec extends Specification with JValueGen with ScalaCheck {
  "JSON AST Specification".title
  
  "Functor identity" in { (json: JValue) => 
    json === (json map identity)
  }

  "Functor composition" in check{ (json: JValue, fa: JValue => JValue, fb: JValue => JValue) =>
      json.map(fb).map(fa) === json.map(fa compose fb)
  }

  "Monoid identity" in { (json: JValue) => 
    (json ++ JNothing === json) and (JNothing ++ json === json)
  }

  "Monoid associativity" in check { (x: JValue, y: JValue, z: JValue) => 
    x ++ (y ++ z) === (x ++ y) ++ z
  }

  "Merge identity" in { (json: JValue) => 
    (json merge JNothing) === json and (JNothing merge json) === json
  }

  "Merge idempotency" in { (x: JValue) => 
   (x merge x) === x
  }

  "Diff identity" in check { (json: JValue) =>
    ((json diff JNothing) === Diff(JNothing, JNothing, json)) and
    ((JNothing diff json) === Diff(JNothing, json, JNothing))
  }

  "Diff with self is empty" in check { (x: JValue) => 
   (x diff x) === Diff(JNothing, JNothing, JNothing)
  }

  "Diff is subset of originals" in check { (x: JObject, y: JObject) => 
    val Diff(c, a, d) = x diff y
    y === (y merge (c merge a))
  }

  "Diff result is same when fields are reordered" in check { (x: JObject) => 
   (x diff reorderFields(x)) === Diff(JNothing, JNothing, JNothing)
  }

  "Remove all" in { (x: JValue) => 
   (x remove { _ => true }) === JNothing
  }

  "Remove nothing" in { (x: JValue) => 
   (x remove { _ => false }) === x
  }

  "Remove removes only matching elements (in case of a field, its value is set to JNothing)" in {
    forAllNoShrink(genJValue, genJValueClass) { (json: JValue, x: Class[_ <: JValue]) => {
      val removed = json remove typePredicate(x)
      val Diff(c, a, d) = json diff removed
      val elemsLeft = removed filter {
        case JField(_, JNothing) => false
        case _ => true
      }
      c == JNothing && a == JNothing && elemsLeft.forall(_.getClass != x)
    }}
  }

  "Replace one" in checkProp { 
     val anyReplacement = (x: JValue, replacement: JObject) => {
      def findOnePath(jv: JValue, l: List[String]): List[String] = jv match {
        case JField(name, value) => findOnePath(value, name :: l)
        case JObject(fl) => fl match {
          case field :: xs => findOnePath(field, l)
          case Nil => l
        }
        case _ => l
      }

      val path = findOnePath(x, Nil).reverse
      val result = x.replace(path, replacement)

      def replaced(path: List[String], in: JValue): Boolean = {
        path match {
          case Nil => x == in

          case name :: Nil => (in \ name) match {
            case `replacement` => true
            case _ => false
          }

          case name :: xs => (in \ name) match {
            case JNothing => false
            case value => replaced(xs, value)
          }
        }
      }

      replaced(path, result)
    }

    // ensure that we test some JObject instances
    val fieldReplacement = (x: JObject, replacement: JObject) => anyReplacement(x, replacement)

    fieldReplacement.forAll && anyReplacement.forAll
  }

  private def reorderFields(json: JValue) = json map {
    case JObject(xs) => JObject(xs.reverse)
    case x => x
  }

  private def typePredicate(clazz: Class[_])(json: JValue) = json match {
    case x if x.getClass == clazz => true
    case _ => false
  }

  implicit def arbJValue: Arbitrary[JValue] = Arbitrary(genJValue)
  implicit def arbJObject: Arbitrary[JObject] = Arbitrary(genObject)
}
