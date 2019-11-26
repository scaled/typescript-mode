//
// Scaled TypeScript Mode - a Scaled major mode for editing TypeScript code
// http://github.com/scaled/typescript-mode/blob/master/LICENSE

package scaled.project

import com.eclipsesource.json._
import java.nio.file.{Files, Path}
import scaled._

object TypeScriptPlugins {

  val TSConfigFile = "tsconfig.json"
  val PackageFile = "package.json"

  @Plugin(tag="project-root")
  class TSConfigRootPlugin extends RootPlugin.File(TSConfigFile) {
    override protected def createRoot (paths :List[Path], path :Path) = {
      if (Files.exists(path.resolve(PackageFile))) Project.Root(path)
      else {
        var module = s"${path.getFileName()}"
        var root = path.getParent()
        while (root != null && !Files.exists(root.resolve(PackageFile))) {
          module = s"${path.getFileName()}/${module}"
          root = path.getParent()
        }
        if (root == null) Project.Root(path) else Project.Root(root, module)
      }
    }
  }

  /** Extract projects metadata from `package.json` and `tsconfig.json` files. */
  @Plugin(tag="project-resolver")
  class TSConfigResolverPlugin extends ResolverPlugin {

    override def metaFiles (root :Project.Root) = Seq(root.path.resolve(TSConfigFile))

    def addComponents (project :Project) :Unit = {
      val rootPath = project.root.path
      // we trigger on tsconfig.json (which is how we know it's a TypeScript project) but we
      // extract metadata from package.json
      val pkgFile = rootPath.resolve(PackageFile)
      val config = Json.parse(Files.newBufferedReader(pkgFile)).asObject

      val mod = project.root.module
      val modPath = if (mod == "") project.root.path else project.root.path.resolve(mod)
      val baseName = Option(config.get("name")).map(_.asString).
        getOrElse(rootPath.getFileName.toString)
      val projName = if (mod == "") baseName else s"${baseName}-${mod}"

      val sb = Ignorer.stockIgnores
      sb += Ignorer.ignorePath(project.root.path.resolve("node_modules"), project.root.path)
      Option(config.get("ignore")).map(_.asArray).foreach { ignores =>
        // TODO: handle glob ignores properly
        ignores.map(_.asString).foreach { sb += Ignorer.ignoreName(_) }
      }
      project.addComponent(classOf[Filer], new DirectoryFiler(project, sb))

      // TODO: package.json doesn't define source directories, so we hack some stuff
      val sourceDirs = Seq("src", "test").map(modPath.resolve(_))
      project.addComponent(classOf[Sources], new Sources(sourceDirs))

      val oldMeta = project.metaV()
      project.metaV() = oldMeta.copy(name = projName)
    }
  }

  // TODO: we should try to figure out if the typescript-language-server node module is actually
  // installed (globally); of course there's going to be no pleasant way to do this...
  @Plugin(tag="langserver")
  class TypeScriptLangPlugin extends LangPlugin {
    def suffs (root :Project.Root) = Set("ts", "tsx")
    def canActivate (root :Project.Root) = Files.exists(root.path.resolve(TSConfigFile))
    def createClient (proj :Project) = Future.success(
      new TypeScriptLangClient(proj.metaSvc, proj.root.path, serverCmd(proj.root.path)))
  }

  private def serverCmd (root :Path) :Seq[String] = {
    Seq("typescript-language-server", "--stdio")
  }
}

class TypeScriptLangClient (msvc :MetaService, root :Path, serverCmd :Seq[String])
    extends LangClient(msvc, root, serverCmd) {

  override def name = "TypeScript"
}
