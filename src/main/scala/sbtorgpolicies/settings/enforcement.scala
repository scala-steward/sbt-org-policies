/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package sbtorgpolicies.settings

import sbt.Keys._
import sbt._
import sbtorgpolicies.exceptions.ValidationException
import sbtorgpolicies.model._
import sbtorgpolicies.OrgPoliciesKeys._
import de.heikoseeberger.sbtheader
import scoverage.ScoverageKeys

trait enforcement {

  lazy val orgEnforcementSettingsTasks = Seq(
    orgCheckSettings := Def
      .sequential(
        checkScalaVersion,
        checkCrossScalaVersion,
        checkScoverageSettings,
        checkFileHeaderSettings
      )
      .value
  )

  private[this] def checkScalaVersion = Def.task {
    val scalaVersionValue = scalaVersion.value
    val isSbtPlugin       = sbtPlugin.value
    if (!isSbtPlugin && scalaVersionValue != scalac.latestScalaVersion) {
      throw ValidationException(
        s"scalaVersion is $scalaVersionValue. It should be ${scalac.latestScalaVersion}"
      )
    }
  }

  private[this] def checkCrossScalaVersion = Def.task {
    val crossScalaVersionsValue = crossScalaVersions.value
    val isSbtPlugin             = sbtPlugin.value
    if (!isSbtPlugin && !scalac.crossScalaVersions.forall(crossScalaVersionsValue.contains)) {
      throw ValidationException(s"""
           |crossScalaVersions is $crossScalaVersionsValue.
           |It should have at least these versions: ${scalac.crossScalaVersions
                                     .mkString(",")}""".stripMargin)
    }
  }

  private[this] def checkScoverageSettings = Def.task {

    val coverageFailOnMinimumValue = ScoverageKeys.coverageFailOnMinimum.value
    val coverageMinimumValue       = ScoverageKeys.coverageMinimum.value

    if (!coverageFailOnMinimumValue)
      throw ValidationException(
        s"coverageFailOnMinimum is $coverageFailOnMinimumValue, however, it should be enabled."
      )

    if (coverageMinimumValue < scoverageMinimum)
      throw ValidationException(
        s"coverageMinimumValue is $coverageMinimumValue. It should be at least $scoverageMinimum%"
      )
  }

  private[this] def checkFileHeaderSettings = Def.task {
    val headerMappings: Map[sbtheader.FileType, sbtheader.CommentStyle] =
      sbtheader.HeaderPlugin.autoImport.headerMappings.value
    val headerLicense: Option[sbtheader.License] =
      sbtheader.HeaderPlugin.autoImport.headerLicense.value

    if (headerMappings.size <= 0) {
      throw ValidationException("headerMappings is empty and it's a mandatory setting")
    }
    if (headerLicense.isEmpty) {
      throw ValidationException("headerLicense is empty and it's a mandatory setting")
    }
  }

}
