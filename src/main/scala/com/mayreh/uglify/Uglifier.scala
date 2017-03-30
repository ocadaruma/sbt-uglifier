package com.mayreh.uglify

import scalariform.lexer.{Token, Tokens, ScalaLexer}
import scalariform.parser._

object Uglifier {

  def uglify(sources: List[String]): String = {
    sources.map(wrapToPackageBlock).mkString(";")
  }

  def wrapToPackageBlock(aSource: String) = {

    val Some(compilationUnit: CompilationUnit) = ScalaParser.parse(aSource)

    val topStats = compilationUnit.topStats

    val stats = (topStats.firstStatOpt +: topStats.otherStats.map { case (_, stat) => stat }).flatten

    val pkgTokens = stats.collect { case PackageStat(token, name) =>
      flattenCallExpr(name)
    }.flatten

    val pkgContent = pkgTokens.lastOption.map { last =>
      if (aSource.length > last.lastCharacterOffset) {
        aSource.substring(last.lastCharacterOffset + 1)
      } else {
        aSource
      }
    }.getOrElse(aSource)

    val pkgId = pkgTokens.map(_.text).mkString(".")

    s"package ${pkgId} { ${toOneline(pkgContent)} };"
  }

  def flattenCallExpr(expr: CallExpr): List[Token] = {
    def loop(current: CallExpr, result: List[Token]): List[Token] = {
      current match {
        case CallExpr(Some((elements, _)), id, _, _, _) =>
          elements.collect { case e: CallExpr => e }.flatMap { expr =>
            loop(expr, result)
          } :+ id
        case CallExpr(None, id, _, _, _) =>
          result :+ id
      }
    }

    loop(expr, Nil)
  }

  def toOneline(source: String): String = {
    toOneline(ScalaLexer.tokenise(source))
  }

  def toOneline(tokens: List[Token]): String = {
    def handleRawStringLiteral(literal: String): String = {
      val RawStringRegex = "(?ms)\"\"\"(.*)\"\"\"".r
      literal match {
        case RawStringRegex(content) =>
          "\"" + content.replace("\n", "\\n") + "\""
        case _ =>
          literal
      }
    }

    tokens.map { token =>
      token.tokenType match {
        case Tokens.NEWLINE | Tokens.NEWLINES => ";"
        case Tokens.INTERPOLATION_ID => token.text
        case Tokens.STRING_LITERAL => handleRawStringLiteral(token.text) + " "
        case _ => token.text + " "
      }
    }.mkString
  }
}
