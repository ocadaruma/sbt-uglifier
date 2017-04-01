package com.mayreh.uglify

import scalariform.lexer.{Token, Tokens, ScalaLexer}
import scalariform.parser._

object Uglifier {

  def uglify(sources: List[String]): String = {
    sources.map(wrapToPackageBlock).mkString(";")
  }

  private def wrapToPackageBlock(aSource: String) = {

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

    s"package ${pkgId}{${(shrink(pkgContent))}};"
  }

  private def flattenCallExpr(expr: CallExpr): List[Token] = {
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

  private def shrink(aSource: String): String = {

    val lineSeparator = System.lineSeparator()
    val separatorReplacement = lineSeparator match {
      case "\r\n" => "\\r\\n"
      case "\n" => "\\n"
    }
    val separatorAdjustment = separatorReplacement.length - lineSeparator.length

    val tokens = ScalaLexer.tokenise(aSource)
    val buffer = new StringBuilder(aSource)

    var adjustment = 0
    for {
      token <- tokens
    } {
      token.tokenType match {
        case Tokens.NEWLINE | Tokens.NEWLINES =>
          val length = token.lastCharacterOffset - token.offset + 1
          val offset = token.offset - adjustment

          // replace new line tokens with semicolon
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

          token.tokenType match {
            // replace raw String literal with normal String literal
            case Tokens.STRING_PART | Tokens.STRING_LITERAL =>
              if (token.rawText.startsWith("\"\"\"")) {
                val offset = token.offset - adjustment
                buffer.replace(offset, offset + 3, "\"")
                adjustment += 2
              }

              for {
                m <- lineSeparator.r.findAllMatchIn(token.rawText)
              } {
                val length = m.end - m.start
                val offset = token.offset + m.start - adjustment
                buffer.replace(offset, offset + length, separatorReplacement)
                adjustment -= separatorAdjustment
              }

              if (token.rawText.endsWith("\"\"\"")) {
                val offset = token.lastCharacterOffset - 2 - adjustment
                buffer.replace(offset, offset + 3, "\"")
                adjustment += 2
              }

            case _ =>
          }
      }
    }

    buffer.result()
  }
}
