package com.balopat.codingboard

import org.specs2.mutable._

class CodingBoardSpec extends Specification {

  "A CodingBoard" should {
       "return it's name in toString" in {
         aBoard.toString should_==("board")
       }

       "return empty String for "+ 
       "the lastCodeSnippet id if there are no codesnippets" in {
          aBoard.lastCodeSnippetId should_==("")
       }

       "return the last codeSnippet id" in {
          val board = aBoard
          val aCodeSnippet = new CodeSnippet("id", "desc", "code", "lang", 0)
          val anotherCodeSnippet = new CodeSnippet("id2", "desc", "code", "lang", 0)
          (board += 
            aCodeSnippet += 
            anotherCodeSnippet).lastCodeSnippetId should_==("id2")
       }

       "add a codeSnippet if the id is not there already" in {
          val board = aBoard
          val aCodeSnippet = new CodeSnippet("id", "desc", "code", "lang", 0) 
          (board += aCodeSnippet).codeSnippets.size should_== 1
       }

       "not add a codeSnippet if resubmitted" in {
          val board = aBoard 
          val aCodeSnippet = new CodeSnippet("id", "desc", "code", "lang", 0)
          val anotherCodeSnippet = new CodeSnippet("id", "desc", "code", "lang", 0)
          board += aCodeSnippet += anotherCodeSnippet
          board.codeSnippets.size should_==(1)
       }
      
       
       "does not expires before the lifetime reaches the end" in {
          "just before 1 minute should not be expired" ! (!aBoard.isExpired(60999))
       }

       "expires when lifetime reaches the end" in {
          "just after 1 minute should expire" ! (aBoard.isExpired(61001))
       }

       def aBoard() = {
         val lifeTimeInMinutes = 1 
         val creationTimeInMillis = 1000
         new CodingBoard("board", lifeTimeInMinutes, creationTimeInMillis)
       }


  }



}