scalaVersion := "2.12.1"

val docsFiles      = List(new File("docs/README.md"), new File("docs/src/main/resources/index.md"))
val otherDocsFiles = List(new File("other-docs/README.md"), new File("other-docs/src/main/resources/index.md"))
val nonDocsFiles   = List(new File("non-docs/README.md"))

def checkFiles(list: List[File]): Boolean = {
  list.foldLeft(true) {
    case (b, f) =>
      val content = IO.readLines(f).mkString("\n")
      b && content.contains("2.0") && !content.contains("1.2.1")
  } && nonDocsFiles.foldLeft(true) {
    case (b, f) =>
      val content = IO.readLines(f).mkString("\n")
      b && !content.contains("2.0") && content.contains("1.2.1")
  }
}

lazy val testCheckSettings = TaskKey[Unit]("testCheckSettings")
lazy val testCheckSettings2 = TaskKey[Unit]("testCheckSettings2")

testCheckSettings := Def.task {
  if (checkFiles(docsFiles)) {
    streams.value.log.info("Test succeeded.")
  } else {
    sys.error("Error validating docs files")
  }
}.value

testCheckSettings2 := Def.task {
  if (checkFiles(otherDocsFiles)) {
    streams.value.log.info("Test succeeded.")
  } else {
    sys.error("Error validating other docs files")
  }
}.value

orgUpdateDocFilesReplacementsSetting := Map("1.2.1" -> "2.0")