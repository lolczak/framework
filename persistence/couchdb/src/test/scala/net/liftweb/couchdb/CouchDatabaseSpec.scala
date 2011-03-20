/*
 * Copyright 2010-2011 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb
package couchdb

import java.net.ConnectException

import dispatch.{Http, StatusCode}

import org.specs2.mutable._


/**
 * Systems under specification for CouchDatabase.
 */
object CouchDatabaseSpec extends Specification { 
  "CouchDatabase Specification".title
  def setup = {
    val http = new Http
    val database = new Database("test")
    http(database delete) must not(throwA[ConnectException]).orSkip
    (http, database)
  }

  "A database" should {
    "give 404 when info called and nonexistant" in {
      val (http, database) = setup
      http(database info) must throwA[StatusCode].like { case StatusCode(404, _) => ok }
    }

    "give 404 when deleted but nonexistant" in {
      val (http, database) = setup
      http(database delete) must throwA[StatusCode].like { case StatusCode(404, _) => ok }
    }

    "succeed being created" in {
      val (http, database) = setup

      http(database create) must_== ()
    }

    "give 412 instead of allowing creation when already existant" in {
      val (http, database) = setup

      http(database create) must_== ()
      http(database create) must throwA[StatusCode].like { case StatusCode(412, _) => ok }
    }
    
    "have info when created" in {
      val (http, database) = setup

      http(database create) must_== ()
      http(database info).db_name must_== ("test")
    }

    "succeed in being deleted" in {
      val (http, database) = setup

      http(database create) must_== () 
      http(database delete) must_== ()
    }

    "succeed being recreated" in {
      val (http, database) = setup

      http(database create) must_== () 
      http(database delete) must_== ()
      http(database create) must_== () 
    }
  }
}

