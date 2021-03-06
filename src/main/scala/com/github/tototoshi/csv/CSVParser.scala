/*
* Copyright 2013 Toshiyuki Takahashi
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
package com.github.tototoshi.csv

import scala.util.parsing.combinator.{RegexParsers}
import java.io.{Reader, InputStream}


protected trait Between extends RegexParsers {

  def between[A](start: String, p: Parser[A], end: String): Parser[A] = start ~> p <~ end

  def between[A](startAndEnd: String, p: Parser[A]): Parser[A] = between(startAndEnd, p, startAndEnd)

}


class CSVParserException(msg: String) extends Exception(msg)

class CSVParser(format: CSVFormat)
  extends RegexParsers
  with Between {

  override def skipWhitespace = false

  def cr = "\r"

  def lf = "\n"

  def escape = format.escapeChar.toString

  def quote = format.quoteChar.toString

  def delimiter = format.delimiter.toString

  def record = field ~ rep(delimiter ~> field) ^^ {
    case head ~ tail => head :: tail
  }

  def field = format.quoting match {
    case QUOTE_NONE => {
      def textData = escape ~> (""".""".r | crlf) | not(delimiter) ~> """.""".r

      rep(textData) ^^ {
        _.mkString
      }
    }
    case QUOTE_ALL | QUOTE_MINIMAL | QUOTE_NONNUMERIC => {
      def textData = not(delimiter | quote | crlf) ~> """.""".r

      def escapedQuote = repN(2, quote) ^^ {
        _ => quote
      }

      def escaped = between(quote, rep(textData | delimiter | crlf | escapedQuote)) ^^ {
        _.mkString
      }

      def nonEscaped = rep(textData) ^^ {
        _.mkString
      }

      escaped | nonEscaped
    }
  }

  def crlf = cr + lf | cr | lf

  def parseLine(in: Input): ParseResult[List[String]] = parse(record <~ opt(crlf), in)

}
