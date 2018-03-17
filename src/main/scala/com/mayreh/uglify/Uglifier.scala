package com.mayreh.uglify

import scalariform.lexer.{Token, Tokens, ScalaLexer}
import scalariform.parser._

object Uglifier {

  def uglify(sources: Seq[String]): String = {
    sources.map { source =>
      println(source)

      shrink(
        replaceOptionalNewLinesWithWhitespace(
          wrapWithPackageBlock(source)
        )
      )

    }.mkString(";")
  }

  private def wrapWithPackageBlock(source: String): String = {

    def flattenCallExpr(expr: CallExpr): Seq[Token] = {
      def loop(current: CallExpr, result: Seq[Token]): Seq[Token] = {
        current match {
          case CallExpr(Some((elements, _)), id, _, _, _) =>
            elements.collect { case e: CallExpr => e }.flatMap { expr =>
              loop(expr, result)
            } :+ id
          case CallExpr(None, id, _, _, _) =>
            result :+ id
        }
      }

      loop(expr, Vector.empty)
    }

    val Some(compilationUnit: CompilationUnit) = ScalaParser.parse(source)

    val topStats = compilationUnit.topStats

    val stats = (topStats.firstStatOpt +: topStats.otherStats.map { case (_, stat) => stat }).flatten

    val pkgTokens = stats.collect { case PackageStat(_, name) =>
      flattenCallExpr(name)
    }.flatten

    val pkgContent = pkgTokens.lastOption.map { last =>
      if (source.length > last.lastCharacterOffset) {
        source.substring(last.lastCharacterOffset + 1)
      } else {
        source
      }
    }.getOrElse(source)

    val joined = pkgTokens.map(_.text).mkString(".")

    // handle root package
    val pkgId = if (joined.nonEmpty) {
      joined
    } else {
      "_root_"
    }

    s"package ${pkgId}{${(pkgContent)}};"
  }

  private def replaceOptionalNewLinesWithWhitespace(source: String): String = {

    def enumerate(current: AstNode, acc: Seq[AstNode]): Seq[AstNode] = {
      if (current.immediateChildren.nonEmpty) {
        current.immediateChildren.flatMap { enumerate(_, acc) } :+ current
      } else {
        acc :+ current
      }
    }

    val Some(compilationUnit: CompilationUnit) = ScalaParser.parse(source)

    val nodes = enumerate(compilationUnit, Vector.empty)

    val tokens = nodes.map(CanNewLine.apply).collect {
      case Some(CanNewLine(newLines)) => newLines
    }.flatten.sortBy(_.offset)

    val buffer = new StringBuilder(source)
    var adjustment = 0
    for {
      token <- tokens
    } {
      token.tokenType match {
        case Tokens.NEWLINE =>
          val offset = token.offset - adjustment

          buffer.replace(offset, offset + token.length, " ")
          adjustment += token.length - 1

        case Tokens.NEWLINES =>
          val offset = token.offset - adjustment

          buffer.replace(offset, offset + token.length, ";")
          adjustment += token.length - 1
        case _ =>
      }
    }

    buffer.result()
  }

  private def shrink(source: String): String = {

    val tokens = ScalaLexer.tokenise(source)
    val buffer = new StringBuilder(source)

    var previousToken: Option[Token] = None
    var adjustment = 0
    for {
      token <- tokens
    } {
      token.tokenType match {
        case Tokens.NEWLINE | Tokens.NEWLINES =>
          val length = token.lastCharacterOffset - token.offset + 1
          val offset = token.offset - adjustment

          buffer.replace(offset, offset + length, ";")
          adjustment += length - 1

        case _ =>
          // replace hidden tokens with single whitespace
          val hiddens = token.associatedWhitespaceAndComments
          if (hiddens.nonEmpty) {
            val length = hiddens.lastCharacterOffset - hiddens.offset + 1
            val offset = hiddens.offset - adjustment

            buffer.replace(offset, offset + length, " ")
            adjustment += length - 1
          }
      }
      previousToken = Some(token)
    }

    buffer.result()
  }

  case class CanNewLine(nlTokens: Seq[Token])
  object CanNewLine {
    def apply(node: AstNode): Option[CanNewLine] = node match {
      case Annotation(_, _, _, newLineOption) =>
        Some(CanNewLine(newLineOption.toSeq))
      case InfixExpr(_, _, newLineOption, _) =>
        Some(CanNewLine(newLineOption.toSeq))
      case IfExpr(_, _, newLinesOption, _, _) =>
        Some(CanNewLine(newLinesOption.toSeq))
      case WhileExpr(_, _, newLinesOption, _) =>
        Some(CanNewLine(newLinesOption.toSeq))
      case ForExpr(_, _, _, _, newLinesOption, _, _) =>
        Some(CanNewLine(newLinesOption.toSeq))
      case ProcFunBody(newLineOption, _) =>
        Some(CanNewLine(newLineOption.toSeq))
      case ParamClauses(newLineOption, paramClausesAndNewLines) =>
        Some(CanNewLine(newLineOption.toSeq ++ paramClausesAndNewLines.flatMap { case (_, token) => token }))
      case TemplateBody(newLineOption, _, _, _) =>
        Some(CanNewLine(newLineOption.toSeq))
      case PackageBlock(_, _, newLineOption, _, _, _) =>
        Some(CanNewLine(newLineOption.toSeq))
      case _ =>
        None
    }
  }
}
