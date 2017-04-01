package com.mayreh.uglify

import scalariform.lexer.{Token, Tokens, ScalaLexer}
import scalariform.parser._

object Uglifier {

  def uglify(sources: Seq[String]): String = {
    sources.map(wrapWithPackageBlock).mkString(";")
  }

  private def wrapWithPackageBlock(aSource: String) = {

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

  private def flattenCallExpr(expr: CallExpr): Seq[Token] = {
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

  private def shrink(aSource: String): String = {

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
              val literal = StringLiteral(token)

              if (literal.isRaw) {
                var (contentOffset, contentInset) = (0, 0)
                literal match {
                  case StringLiteral.RawLeftPart(offset) =>
                    contentOffset = offset
                  case StringLiteral.RawRightPart(inset) =>
                    contentInset = inset
                  case StringLiteral.RawFullLiteral(offset, inset) =>
                    contentOffset = offset
                    contentInset = inset
                  case _ =>
                }

                if (contentOffset == 3) {
                  val offset = token.offset - adjustment
                  buffer.replace(offset, offset + contentOffset, "\"")
                  adjustment += 2
                }

                val content = token.rawText.substring(contentOffset, token.rawText.length - contentInset)
                for {
                  m <- "\r|\n|\"".r.findAllMatchIn(content)
                } {
                  val length = m.end - m.start
                  val offset = token.offset + m.start + contentOffset - adjustment

                  m.group(0) match {
                    case "\r" => buffer.replace(offset, offset + length, "\\r")
                    case "\n" => buffer.replace(offset, offset + length, "\\n")
                    case "\"" => buffer.replace(offset, offset + length, "\\\"")
                  }
                  adjustment -= 1
                }

                if (contentInset == 3) {
                  val offset = token.lastCharacterOffset - 2 - adjustment
                  buffer.replace(offset, offset + 3, "\"")
                  adjustment += 2
                }
              }

            case _ =>
          }
      }
    }

    buffer.result()
  }

  sealed abstract class StringLiteral { def isRaw: Boolean = this != StringLiteral.Normal }
  object StringLiteral {
    case class RawLeftPart(contentOffset: Int) extends StringLiteral
    case class RawRightPart(contentInset: Int) extends StringLiteral
    case class RawFullLiteral(contentOffset: Int, contentInset: Int) extends StringLiteral
    case object Normal extends StringLiteral

    def apply(token: Token): StringLiteral = {
      token.tokenType match {
        case Tokens.STRING_PART =>
          if (token.rawText.startsWith("\"\"\"")) {
            RawLeftPart(3)
          } else {
            Normal
          }
        case Tokens.STRING_LITERAL =>
          if (token.rawText.surroundedBy("\"\"\"", "\"\"\"")) {
            RawFullLiteral(3, 3)
          } else if (token.rawText.endsWith("\"\"\"")) {
            RawRightPart(3)
          } else {
            Normal
          }
      }
    }
  }

  implicit class RichString(val self: String) extends AnyVal {
    def surroundedBy(left: String, right: String): Boolean = {
      self.startsWith(left) && self.endsWith(right) && (self.length >= left.length + right.length)
    }
  }
}
