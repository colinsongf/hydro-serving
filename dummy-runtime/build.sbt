import java.io.File

enablePlugins(sbtdocker.DockerPlugin)

name := "dummy-runtime"

dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (dependencyClasspath in Compile).value
  val dockerFilesLocation=baseDirectory.value / "src/main/docker/"
  val jarTarget = s"/hydro-serving/app/app.jar"
  val osName = sys.props.get("os.name").getOrElse("unknown")

  new Dockerfile {
    from("openjdk:8u151-jre-alpine")

    env("APP_PORT","9090")
    env("SIDECAR_PORT","8080")
    env("SIDECAR_HOST","localhost")
    env("MODEL_DIR","/model")

    label("DEPLOYMENT_TYPE", "APP")

    add(dockerFilesLocation, "/hydro-serving/app/")
    // Add all files on the classpath
    add(classpath.files, "/hydro-serving/app/lib/")
    // Add the JAR file
    add(jarFile, jarTarget)

    volume("/model")

    if (osName.toLowerCase.startsWith("windows")) {
      run("dos2unix", "/hydro-serving/app/start.sh")
    }

    cmd("/hydro-serving/app/start.sh")
  }
}

imageNames in docker := Seq(
  ImageName(s"hydrosphere/serving-runtime-dummy:${version.value}")
)