import dependencies.DependenciesPlugin.autoImport._
import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.model.{GitHubSettings, scalac}
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates.badges._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val pluginSettings: Seq[Def.Setting[_]] = Seq(
      resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
      addSbtPlugin(%%("sbt-git", true)),
      addSbtPlugin(%%("sbt-unidoc", true)),
      addSbtPlugin(%%("sbt-release", true)),
      addSbtPlugin(%%("sbt-sonatype", true)),
      addSbtPlugin(%%("sbt-pgp", true)),
      addSbtPlugin(%%("sbt-ghpages", true)),
      addSbtPlugin(%%("sbt-site", true)),
      addSbtPlugin(%%("sbt-jmh", true)),
      addSbtPlugin(%%("scalastyle-sbt-plugin", true)),
      addSbtPlugin(%%("sbt-scoverage", true)),
      addSbtPlugin(%%("sbt-scalajs", true)),
      addSbtPlugin(%%("sbt-header", "3.0.1", true)),
      addSbtPlugin(%%("sbt-dependencies", true)),
      addSbtPlugin(%%("sbt-coursier", true)),
      addSbtPlugin(%%("sbt-microsites", true)),
      addSbtPlugin(%%("tut-plugin", true)),
      addSbtPlugin(%%("sbt-scalafmt", "1.5.1", true)),
      libraryDependencies ++= Seq(
        "com.geirsson" %% "scalafmt-cli" % "1.5.1",
      ),
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq(
            "-Xmx2048M",
            "-XX:ReservedCodeCacheSize=256m",
            "-XX:+UseConcMarkSweepGC",
            "-Dplugin.version=" + version.value,
            "-Dscala.version=" + scalaVersion.value
          )
      },
      scriptedBufferLog := false,
    )

    lazy val coreSettings: Seq[Def.Setting[_]] = Seq(
      resolvers += Resolver.typesafeIvyRepo("releases"),
      scalaVersion := scalac.`2.12`,
      crossScalaVersions := Seq(scalac.`2.12`),
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.1.1",
        %%("github4s"),
        %%("cats-core"),
        %%("base64"),
        %%("moultingyaml"),
        %%("cats-laws")             % Test,
        %%("scalacheck", "1.13.5")  % Test force(),
        %%("scalatest")             % Test,
        %%("scheckToolboxDatetime") % Test,
        %%("scalamockScalatest")    % Test
      ),
    )

    // Shade and bundle jawn-parser and intermediate deps (https://github.com/47deg/sbt-org-policies/issues/1173)
    lazy val jawnShadingSettings: Seq[Def.Setting[_]] = Seq(
      // intransitive(), so we bundle the bare minimum dependencies to shade the chain of calls from our code to jawn-parser.
      // Make sure to keep all versions in sync with those brought in transitively by github4s (not always the latest versions).
      // Also make sure to include our project's compiled code as an unmanaged jar (in build.sbt) to shade calls to github4s.
      libraryDependencies ++= Seq(
        %%("github4s"),
        %%("circe-parser", "0.10.0"),
        "io.circe" %% "circe-jawn" % "0.10.0",
        "org.spire-math" %% "jawn-parser" % "0.13.0",
      ).map(_.intransitive()),
      assembly / assemblyOption ~= (_.copy(includeScala = false)),
      // must be careful not to rename any calls to unshaded packages outside our bundled dependencies (ex. io.circe.** would break)
      assembly / assemblyShadeRules := Seq(ShadeRule.rename(
        "github4s.**"        -> "org_policies_github4s.@1",
        "io.circe.parser.**" -> "org_policies_circe_parser.@1",
        "io.circe.jawn.**"   -> "org_policies_circe_jawn.@1",
        "jawn.**"            -> "org_policies_jawn_parser.@1"
      )).map(_.inAll),
    ) ++ noPublishSettings
  }

  override def projectSettings: Seq[Def.Setting[_]] = artifactSettings ++ shellPromptSettings ++
    Seq(
      orgProjectName := name.value,
      orgGithubSetting := GitHubSettings(
        organization = "47deg",
        project = (name in LocalRootProject).value,
        organizationName = "47 Degrees",
        groupId = "com.47deg",
        organizationHomePage = url("http://47deg.com"),
        organizationEmail = "hello@47deg.com"
      ),
      orgGithubTokenSetting := "ORG_GITHUB_TOKEN",
      orgGithubOpsSetting := new GitHubOps(
        orgGithubSetting.value.organization,
        orgGithubSetting.value.project,
        getEnvVar(orgGithubTokenSetting.value)
      )
    )

  private[this] val artifactSettings = Seq(
    scalaVersion := scalac.`2.12`,
    crossScalaVersions := Seq(scalac.`2.12`),
    scalaOrganization := "org.scala-lang",
    startYear := Some(2017),
    orgBadgeListSetting := List(
      TravisBadge.apply,
      MavenCentralBadge.apply,
      LicenseBadge.apply,
      GitHubIssuesBadge.apply
    ),
    orgAfterCISuccessTaskListSetting ~= (_ filterNot (_ == depUpdateDependencyIssues.asRunnableItem))
  )
}
