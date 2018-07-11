//
// Scaled TypeScript Mode - a Scaled major mode for editing TypeScript code
// http://github.com/scaled/scala-mode/blob/master/LICENSE

package scaled.typescript

import scaled._
import scaled.Matcher
import scaled.code.{CodeConfig, Commenter, BlockIndenter}
import scaled.grammar._

@Plugin(tag="textmate-grammar")
class TypeScriptGrammarPlugin extends GrammarPlugin {
  import CodeConfig._

  override def grammars = Map("source.typescript" -> "TypeScript.ndf")

  override def effacers = List(
    effacer("comment.line", commentStyle),
    effacer("comment.block", docStyle),
    effacer("constant", constantStyle),
    effacer("invalid", invalidStyle),
    effacer("keyword", keywordStyle),
    effacer("string", stringStyle),

    effacer("entity.name.package", moduleStyle),
    effacer("entity.name.class", typeStyle),
    effacer("entity.name.type", typeStyle),
    effacer("entity.other.inherited-class", typeStyle),
    effacer("entity.name.function", functionStyle),
    effacer("entity.name.val-declaration", variableStyle),

    effacer("support.type", typeStyle),

    // effacer("meta.definition.method.typescript", functionStyle),
    effacer("meta.method.typescript", functionStyle),

    effacer("storage.modifier.import", moduleStyle),
    effacer("storage.modifier", keywordStyle),
    effacer("storage.type.annotation", preprocessorStyle),
    effacer("storage.type.def", keywordStyle),
    effacer("storage.type", keywordStyle),

    // effacer("variable.import", typeStyle),
    // effacer("variable.language", constantStyle),
    effacer("variable", variableStyle)
  )

  override def syntaxers = List(
    syntaxer("comment.line", Syntax.LineComment),
    syntaxer("comment.block", Syntax.DocComment),
    syntaxer("constant", Syntax.OtherLiteral),
    syntaxer("string", Syntax.StringLiteral)
  )
}

object TypeScriptConfig extends Config.Defs {

  @Var("If true, cases inside switch blocks are indented one step.")
  val indentCaseBlocks = key(true)
}

@Major(name="typescript",
       tags=Array("code", "project", "typescript"),
       pats=Array(".*\\.ts", ".*\\.tsx"),
       ints=Array("typescript"),
       desc="A major editing mode for the TypeScript language.")
class TypeScriptMode (env :Env) extends GrammarCodeMode(env) {
  import CodeConfig._
  import scaled.util.Chars._

  override def langScope = "source.typescript"

  override protected def createIndenter = new BlockIndenter(config, Std.seq(
    // bump extends/implements in two indentation levels
    BlockIndenter.adjustIndentWhenMatchStart(Matcher.regexp("(extends|implements)\\b"), 2),
    // align changed method calls under their dot
    new BlockIndenter.AlignUnderDotRule(),
    // handle javadoc and block comments
    new BlockIndenter.BlockCommentRule(),
    // handle indenting switch statements properly
    new BlockIndenter.SwitchRule() {
      override def indentCaseBlocks = config(TypeScriptConfig.indentCaseBlocks)
    },
    // handle continued statements, with some special sauce for : after case
    new BlockIndenter.CLikeContStmtRule(),
    // handle indenting lambda blocks
    new BlockIndenter.LambdaBlockRule(" =>")
  ))

  override val commenter = new Commenter() {
    override def linePrefix  = "//"
    override def blockOpen = "/*"
    override def blockPrefix = "*"
    override def blockClose = "*/"
    override def docOpen   = "/**"
  }

  // TODO: more things!
}
